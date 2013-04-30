package ntut.csie.csdet.refactor;

import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.visitor.CarelessCleanupVisitor;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Careless CleanUp Refactoring的具體操作都在這個class中
 * @author Min, Shiau
 */
public class CarelessCleanUpRefactor extends Refactoring {
	private static Logger logger = LoggerFactory.getLogger(CarelessCleanUpRefactor.class);
	
	private IJavaProject project;
		
	//使用者所點選的Marker
	private IMarker marker;
	
	private IOpenable actOpenable;
	
	private TextFileChange textFileChange;
	
	// 存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	// 存放目前所要fix的method node
	private MethodDeclaration currentMethodNode = null;
	
	// 收集Method內所有的Careless CleanUp
	private List<MarkerInfo> CarelessCleanUpList = null;
	
	// Method是否存在，是：新增Caller Method ，否：新增Release Method
	private boolean isMethodExist = false;
	
	// methodName的變數名稱,預設是close
	private String methodName;

	// modifier的Type，預設是private
	private String modifierType;

	// log的type,預設是e.printStackTrace
	private String logType;
	
	// 使用者若選擇Existing Method，要呼叫的Method資訊
	private IMethod existingMethod;
	
	// Careless CleanUp的Smell Message
	private MarkerInfo smellMessage = null;
	
	// 釋放資源的Statement
	private ExpressionStatement cleanUpExpressionStatement;
	
	// Try Block在Method裡的位置
	private int tryIndex = -1;

	/**
	 * 結束動作
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		//去修改AST Tree
		collectChange(marker.getResource());
		//不需check final condition
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}
	
	/**
	 * 初始動作
	 * 確認初始狀態是否符合
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		//不需check initial condition
		RefactoringStatus status;

		boolean isOK = findMethod(marker.getResource());
		if(isOK && currentMethodNode != null){
			CarelessCleanupVisitor visitor = new CarelessCleanupVisitor(actRoot);
			currentMethodNode.accept(visitor);
			//取得code smell的List
			CarelessCleanUpList = visitor.getCarelessCleanupList();
		}
		
		//取得EH Smell的資訊
		findSmellMessage();

		// 取得Smell所在的Try Block中
		TryStatement tryStatement = findTryStatement();
		
		// 判斷Smell是否位於If之中，若是其If內的Statement數不能過超1行
		boolean isSizeSafe = detectIfStatementSize(tryStatement.getBody());

		if (isSizeSafe)
			status = new RefactoringStatus();
		else
			status = RefactoringStatus.createFatalErrorStatus("判斷式內有其它的程式碼");
		
		return status;
	}
	
	/**
	 * 刪除Block內Smell Statement
	 * @param block
	 */
	private boolean detectIfStatementSize(Block block) {
		List<?> statements = block.statements();
		//比對Try Statement裡是否有欲移動的程式碼,若有則移除
		for(int i=0; i < statements.size(); i++) {
			if (statements.get(i) instanceof IfStatement) {
				IfStatement aStatement = (IfStatement) statements.get(i);
				// 若為If判斷式，且包含Smell資訊
				if (aStatement.getStartPosition() <= smellMessage.getPosition() &&
					aStatement.getStartPosition() + aStatement.getLength()
												  >= smellMessage.getPosition()) {

					IfStatement ifStatement = (IfStatement) statements.remove(i);
					Statement thenStatement = ifStatement.getThenStatement();
					// 如果為Block ( if有加"{" "}" )
					if (thenStatement instanceof Block) {
						Block ifBlock = (Block) ifStatement.getThenStatement();
						int ifSize = ifBlock.statements().size();
						if (ifSize == 1)
							return true;
						else
							return false;
					// 不為Block (if 後直接接Statement)
					} else {
						return true;
					}
				}
			}
		}
		return true;
	}

	@Override
	public Change createChange(IProgressMonitor pm) 
								throws CoreException, OperationCanceledException {
		// 2010.07.20 之前的寫法，Preview的Token不會變色
		// 把要變更的結果包成composite傳出去
		//Change[] changes = new Change[] {textFileChange};
		//CompositeChange change = new CompositeChange("My Extract Method", changes);

		/* 參考Eclipse Code
		 * org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring
		 * createChange method的寫法 */
		String name = "Extract CleanUp Method";
		ICompilationUnit unit = (ICompilationUnit) this.actOpenable;
		CompilationUnitChange result = new CompilationUnitChange(name, unit);
		result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

		//MultiTextEdit root= new MultiTextEdit();
		//root.addChild(edits);

		// 將修改結果設置在CompilationUnitChange
		TextEdit edits = textFileChange.getEdit();
		result.setEdit(edits);
		// 將修改結果設成Group，會顯示在Preview上方節點。
		result.addTextEditGroup(new TextEditGroup("Careless Clean Up Method", 
								new TextEdit[] {edits} ));

		return result;
	}

	@Override
	public String getName() {		
		return "Extract CleanUp Method";
	}
	
	/**
	 * 把marker傳進來供此class存取一些code smell資訊
	 * @param marker
	 */
	public void setMarker(IMarker marker){
		this.marker = marker;
		this.project = JavaCore.create(marker.getResource().getProject());
	}
	
	/**
	 * parse AST Tree並取得要修改的method node
	 * @param resource
	 */
	private void collectChange(IResource resource){
		//取得要修改的CompilationUnit
		if (findMethod(resource) && currentMethodNode != null) {
			extractMethod();
		}
	}
	
	/**
	 * 取得目前要被修改的method node
	 * @param resource
	 * @return
	 */
	private boolean findMethod(IResource resource) {
		//取得要修改的CompilationUnit
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			try {
				IJavaElement javaElement = JavaCore.create(resource);
				
				if (javaElement instanceof IOpenable)
					this.actOpenable = (IOpenable) javaElement;
				
				//Create AST to parse
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);

				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				this.actRoot = (CompilationUnit) parser.createAST(null);

				//取得該class所有的method
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<MethodDeclaration> methodList = methodCollector.getMethodList();
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);

				//取得目前要被修改的method node
				currentMethodNode = methodList.get(Integer.parseInt(methodIdx));

				return true;			
			} catch (Exception ex) {
				logger.error("[Find CS Method] EXCEPTION ",ex);
			}
		}
		return false;
	}
	
	private void extractMethod() {
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();

		// 取得EH Smell的資訊
		findSmellMessage();

		// 取得Smell所在的Try Block中
		TryStatement tryStatement = findTryStatement();
		
		// 若try Statement裡沒有Finally Block,則建立Finally Block
		Block finallyBlock = addFinallyBlock(ast, tryStatement);

		// 刪除fos.close();
		deleteCleanUpLine(ast, tryStatement);

		// 若fos是在Try Block宣告，將它移至Try Block外
		moveInstance(ast, tryStatement);

		// 在finally中加入closeStream(fos)
		addMethodInFinally(ast, finallyBlock);

		// 寫回Edit中
		applyChange();
	}

	/**
	 * 將Instance移至Try Catch外
	 * @param ast
	 * @param tryStatement
	 */
	private void moveInstance(AST ast, TryStatement tryStatement) {		
		// e.g. fos.close();
		MethodInvocation delLineMI = (MethodInvocation) cleanUpExpressionStatement.getExpression();
		// e.g. fos
		Expression expression = delLineMI.getExpression();

		// traverse try statements
		List<?> tryList = tryStatement.getBody().statements();
		for (int i=0; i < tryList.size(); i++) {

			if (tryList.get(i) instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variable = (VariableDeclarationStatement) tryList.get(i);
				List<?> fragmentsList = variable.fragments();
				if (fragmentsList.size() == 1) {
					VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragmentsList.get(0);
					// 若參數建在Try Block內
					if (fragment.getName().toString().equals(expression.toString())) {
						/* 將   InputStream fos = new ImputStream();
						 * 改為 fos = new InputStream();
						 * */
						Assignment assignment = ast.newAssignment();
						assignment.setOperator(Assignment.Operator.ASSIGN);
						// fos
						assignment.setLeftHandSide(ast.newSimpleName(fragment.getName().toString()));
						// new InputStream
						Expression init = fragment.getInitializer();
						ASTNode copyNode = ASTNode.copySubtree(init.getAST(), init);
						assignment.setRightHandSide((Expression) copyNode);

						// 將fos = new ImputStream(); 替換到原本的程式裡
						if(assignment.getRightHandSide().getNodeType() != ASTNode.NULL_LITERAL){
							ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
							tryStatement.getBody().statements().set(i, expressionStatement);
						}else{
							//如果本來的程式碼是設定instance初始為null，那就直接移除掉
							tryStatement.getBody().statements().remove(i);
						}

						// InputStream fos = null
						// 將new動作替換成null
						fragment.setInitializer(ast.newNullLiteral());
						// 加至原本程式碼之前
						MethodDeclaration md = currentMethodNode;
						md.getBody().statements().add(tryIndex, variable);
						break;
					}
				}
			}
		}
	}

	/**
	 * 尋找刪除的程式碼
	 */
	private void findSmellMessage() {
		try {
			String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
			smellMessage = CarelessCleanUpList.get(Integer.parseInt(msgIdx));
		} catch (CoreException e) {
			logger.error("[Find CS Method] EXCEPTION ", e);
		}
	}
	
	/**
	 * 取得Smell所在的Try Statement
	 * @return	尋找得到：Try Statement 找不到：Null
	 */
	private TryStatement findTryStatement() {
		//取得方法中所有的statement
		MethodDeclaration md = currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();

		return findTryStatement(statement);
	}
	
	private TryStatement findTryStatement(List<?> statement) {
		for (int i=0; i < statement.size(); i++) {
			if(((ASTNode)statement.get(i)).getNodeType() == ASTNode.WHILE_STATEMENT) {
				WhileStatement ws = (WhileStatement)statement.get(i);
				Block block = (Block)ws.getBody();
				return findTryStatement(block.statements());
			}
			if(((ASTNode)statement.get(i)).getNodeType() == ASTNode.FOR_STATEMENT) {
				ForStatement fs = (ForStatement)statement.get(i);
				Block block = (Block)fs.getBody();
				return findTryStatement(block.statements());
			}
			if(((ASTNode)statement.get(i)).getNodeType() == ASTNode.TRY_STATEMENT) {
				TryStatement aTryStatement = (TryStatement) statement.get(i);
				// 如果Smell位於Try內
				if (aTryStatement.getStartPosition() <= smellMessage.getPosition() &&
					aTryStatement.getStartPosition()+ aTryStatement.getLength()
					                                 >= smellMessage.getPosition()) {					
					tryIndex = i;
					return aTryStatement;
				}
			}
		}
		return null;
	}

	/**
	 * 判斷Try Statement是否有Finally Block，若無則建立Finally Block
	 * @param tryStatement 
	 */
	private Block addFinallyBlock(AST ast, TryStatement tryStatement) {
		assert tryStatement != null;
		if (tryStatement.getFinally() == null) {
			Block block = ast.newBlock();
			tryStatement.setFinally(block);
		}
		return tryStatement.getFinally();
	}

	/**
	 * 刪除Careless CleanUp Smell 該行
	 * @param ast 
	 * @param tryStatement 
	 */
	private void deleteCleanUpLine(AST ast, TryStatement tryStatement) {
		boolean isDeleted = false;
		//尋找Try Block
		isDeleted = deleteBlockStatement(tryStatement.getBody(), ast);

		List<?> catchs = tryStatement.catchClauses();
		for (int j=0; j < catchs.size() && !isDeleted; j++) {
			CatchClause catchClause = (CatchClause)catchs.get(j);
			//尋找Catch Clause
			isDeleted = deleteBlockStatement(catchClause.getBody(), ast);
		}
		
		if(!isDeleted && tryStatement.getFinally() != null) {
			deleteBlockStatement(tryStatement.getFinally(), ast);
		}
	}

	/**
	 * 刪除Block內Smell Statement
	 * @param block 要被刪除的Block
	 * @param ast
	 * @return		是否刪除
	 */
	private boolean deleteBlockStatement(Block block, AST ast) {
		List<?> statements = block.statements();
		//比對Try Statement裡是否有欲移動的程式碼,若有則移除
		for(int i=0; i < statements.size(); i++) {
			if (statements.get(i) instanceof ExpressionStatement) {
				ExpressionStatement aStatement = (ExpressionStatement) statements.get(i);
				// 如果一個Try Catch內有多個Careless CleanUp Smell
				// 用位置判斷才能正確判斷出是哪一個
				if (aStatement.getStartPosition() == smellMessage.getPosition()) {
					cleanUpExpressionStatement = (ExpressionStatement) statements.remove(i);
					return true;
				}
			} else if (statements.get(i) instanceof IfStatement) {
				IfStatement aStatement = (IfStatement) statements.get(i);
				// 若為If判斷式，且包含Smell資訊
				if (aStatement.getStartPosition() <= smellMessage.getPosition() &&
					aStatement.getStartPosition() + aStatement.getLength()
												  >= smellMessage.getPosition()) {

					IfStatement ifStatement = (IfStatement) statements.remove(i);
					Statement thenStatement = ifStatement.getThenStatement();
					// 如果為Block ( if有加"{" "}" )
					if (thenStatement instanceof Block) {
						Block ifBlock = (Block) ifStatement.getThenStatement();
						return deleteBlockStatement(ifBlock, ast);
					// 不為Block (if 後直接接Statement)
					} else {
						ifStatement.setThenStatement(ast.newBlock());
						cleanUpExpressionStatement = (ExpressionStatement) thenStatement;
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Add New Method
	 * @param ast
	 */
	@SuppressWarnings("unchecked")
	private void addExtractMethod(AST ast) {
		//取得資訊
		MethodInvocation delLineMI = (MethodInvocation) cleanUpExpressionStatement.getExpression();
		Expression exp = delLineMI.getExpression();
		SimpleName sn = (SimpleName) exp;

		//新增Method Declaration
		MethodDeclaration newMD = ast.newMethodDeclaration();

		//設定存取型別(public)
		if (modifierType == "public")
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));			
		else if (modifierType == "protected")
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD));						
		else
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));									

		//設定return type
		newMD.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		//設定MD的名稱
		newMD.setName(ast.newSimpleName(methodName));
		//設定參數
		SingleVariableDeclaration svd = ast.newSingleVariableDeclaration();
		svd.setType(ast.newSimpleType(ast.newSimpleName(exp.resolveTypeBinding().getName().toString())));
		svd.setName(ast.newSimpleName(sn.getIdentifier()));
		newMD.parameters().add(svd);

		//設定body
		Block block = ast.newBlock();
		newMD.setBody(block);
		
		TryStatement ts = addTryStatement(ast, delLineMI);

		//將新增的try statement加進來
		block.statements().add(ts);
		//將new MD加入
		List<AbstractTypeDeclaration> typeList = actRoot.types();
		TypeDeclaration td = (TypeDeclaration) typeList.get(0);
		td.bodyDeclarations().add(newMD);
	}

	/**
	 * 將Release的Method加入至Finally Block中
	 * @param ast	MethodNode
	 * @param finallyBlock 
	 */
	private void addMethodInFinally(AST ast, Block finallyBlock) {
		// e.g. fos.close();
		MethodInvocation delLineMI = (MethodInvocation) cleanUpExpressionStatement.getExpression();
		// e.g. fos
		Expression expression = delLineMI.getExpression();

		// 若該行為Method (e.g. closeFile(fos)) 則直直接此行移至Finally Block中
		if (expression == null) {
			finallyBlock.statements().add(cleanUpExpressionStatement);
			return;
		}

		MethodInvocation newMI = null;
		// 若Method不存在
		if (!isMethodExist) {
			newMI = createNewMethod(ast, expression, methodName);

			// 若Method不存在，建立新Method
			addExtractMethod(ast);

		// 若Method已存在
		} else {
			//新增的Method Invocation
			newMI = createNewMethod(ast, expression, existingMethod.getElementName());

			// 設置呼叫Method的名稱
			createCallerMethod(ast, newMI, finallyBlock);
		}

		ExpressionStatement es = ast.newExpressionStatement((Expression) newMI);
		finallyBlock.statements().add(es);
	}

	/**
	 * 新增Method
	 * @param ast
	 * @param expression
	 * @return
	 */
	private MethodInvocation createNewMethod(AST ast, Expression expression, String methodName) {
		SimpleName simpleName = (SimpleName) expression;

		//新增的Method Invocation
		MethodInvocation newMI = ast.newMethodInvocation();

		// 設定MI的name
		newMI.setName(ast.newSimpleName(methodName));

		// 設定MI的參數
		newMI.arguments().add(ast.newSimpleName(simpleName.getIdentifier()));

		return newMI;
	}

	/**
	 * 新增Caller Method
	 * @param ast
	 * @param newMI
	 * @param finallyBlock 
	 */
	private void createCallerMethod(AST ast, MethodInvocation newMI, Block finallyBlock) {
		try {
			//Private時不特別動作
			//if ((existingMethod.getFlags() & Flags.AccPrivate) != 0)

			IType classType = (IType) existingMethod.getParent();
			//若為Public
			if ((existingMethod.getFlags() & Flags.AccPublic) != 0) {
				// 若Method為Static: 直接呼叫
				if ((existingMethod.getFlags() & Flags.AccStatic) != 0) {
					newMI.setExpression(ast.newSimpleName(classType.getElementName()));

				// 若非Static Method: 先New再呼叫
				} else {
					//new Method();
					ClassInstanceCreation classInstance = ast.newClassInstanceCreation();
					classInstance.setType(ast.newSimpleType(ast.newSimpleName(classType.getElementName())));

					//method = new Method();
					VariableDeclarationFragment variableDeclarationFragment = ast.newVariableDeclarationFragment();
					variableDeclarationFragment.setName(ast.newSimpleName("method"));
					variableDeclarationFragment.setInitializer(classInstance);

					//Method method = new Method;
					VariableDeclarationStatement variableDeclaration = ast.newVariableDeclarationStatement(variableDeclarationFragment);
					variableDeclaration.setType(ast.newSimpleType(ast.newSimpleName(classType.getElementName())));

					finallyBlock.statements().add(variableDeclaration);

					newMI.setExpression(ast.newSimpleName("method"));
				}
			}

			// 新增Import資訊
			addImportPackage(classType);

		} catch (JavaModelException e) {
//			logger.error("[Java Method] EXCEPTION", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * 加入Public Method的Import Package
	 * @param classType
	 */
	private void addImportPackage(IType classType) {
		//若Package相同，則不加import
		String extractMethodPackage = classType.getPackageFragment().getElementName();
		String localMethodPackage = actRoot.getPackage().getName().toString();
		if (extractMethodPackage.equals(localMethodPackage))
			return;

		//若Package加入過也不加
		List<?> importList = actRoot.imports();
		for(Object id : importList)
			if(((ImportDeclaration)id).getName().getFullyQualifiedName().contains(classType.getFullyQualifiedName()))
				return;

		//假如沒有import,就加入到AST中
		AST rootAst = actRoot.getAST(); 
		ImportDeclaration imp = rootAst.newImportDeclaration();
		imp.setName(rootAst.newName(classType.getFullyQualifiedName()));
		actRoot.imports().add(imp);
	}

	/**
	 * 加入Try Statement
	 * @param ast
	 * @param delLineMI
	 * @return
	 */
	private TryStatement addTryStatement(AST ast, MethodInvocation delLineMI) {
		TryStatement ts = ast.newTryStatement();
		Block tsBody = ts.getBody();

		/* if (obj != null)
		 * 		obj.close();
		 */
		//建立 obj != null
		InfixExpression in = ast.newInfixExpression();
		in.setOperator(InfixExpression.Operator.NOT_EQUALS);
		in.setLeftOperand(ast.newSimpleName(delLineMI.getExpression().toString()));
		in.setRightOperand(ast.newNullLiteral());

		//建立 if Satement
		IfStatement ifStatement = ast.newIfStatement();
		ifStatement.setExpression(in);
		//加入Release Source Code
		ifStatement.setThenStatement(cleanUpExpressionStatement);
		//加到Try Block之中
		tsBody.statements().add(ifStatement);

		//替try 加入一個Catch clause
		List<CatchClause> catchStatement = ts.catchClauses();
		CatchClause cc = ast.newCatchClause();
		
		//存放程式碼所拋出的例外類型
		ITypeBinding[] iType;
		iType = delLineMI.resolveMethodBinding().getExceptionTypes();
	
		//建立catch的type為 catch(... ex)
		SingleVariableDeclaration svdCatch = ast.newSingleVariableDeclaration();
		svdCatch.setType(ast.newSimpleType(ast.newSimpleName(iType[0].getName())));
		svdCatch.setName(ast.newSimpleName("e"));
		cc.setException(svdCatch);

		//加入catch的body
		if(logType.equals("e.printStackTrace();"))
			addPrintStackStatement(ast, cc);
		else
			addJavaLoggerStatement(ast, cc);

		catchStatement.add(cc);
		return ts;
	}

	/**
	 * 加入e.printStatckTrace
	 * @param ast
	 * @param cc
	 */
	private void addPrintStackStatement(AST ast, CatchClause cc) {
		//新增的Method Invocation
		MethodInvocation catchMI = ast.newMethodInvocation();
		//設定MI的name
		catchMI.setName(ast.newSimpleName("printStackTrace"));
		//設定MI的Expression
		catchMI.setExpression(ast.newSimpleName("e"));			
		ExpressionStatement catchES = ast.newExpressionStatement((Expression)catchMI);
		cc.getBody().statements().add(catchES);
	}

	/**
	 * 加入logger.warning(e.getMessage());
	 * @param ast
	 * @param cc
	 */
	private void addJavaLoggerStatement(AST ast, CatchClause cc) {
		//import java.util.logging.Logger;
		addJavaLoggerLibrary();
		
		//private Logger logger = Logger.getLogger(CarelessCleanUpTest.class.getName());
		addLoggerField(ast);
		
		//設定catch的body的Method Invocation
		MethodInvocation cbMI = ast.newMethodInvocation();
		//設定cbMI的Name
		cbMI.setName(ast.newSimpleName("warning"));
		//設定cbMI的Expression
		cbMI.setExpression(ast.newSimpleName("logger"));
		
		//設定cbMI的arguments的Method Invocation
		MethodInvocation cbarguMI = ast.newMethodInvocation();
		cbarguMI.setName(ast.newSimpleName("getMessage"));
		cbarguMI.setExpression(ast.newSimpleName("e"));
		
		cbMI.arguments().add(cbarguMI);
		
		ExpressionStatement catchES = ast.newExpressionStatement((Expression)cbMI);
		cc.getBody().statements().add(catchES);
	}

	/**
	 *  加入import java.util.logging.Logger;
	 */
	private void addJavaLoggerLibrary() {
		//判斷是否有import java.util.logging.Logger
		boolean isImportLibrary = false;
		List<?> importList = actRoot.imports();
		for(Object id : importList){
			if(((ImportDeclaration)id).getName().getFullyQualifiedName().contains("java.util.logging.Logger")){
				isImportLibrary = true;
			}
		}
		
		//假如沒有import,就加入到AST中
		AST rootAst = actRoot.getAST(); 
		if (!isImportLibrary) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName("java.util.logging.Logger"));
			actRoot.imports().add(imp);
		}
	}
	
	/**
	 * 加入private Logger logger = Logger.getLogger(CarelessCleanUpTest.class.getName());
	 * @param ast
	 */
	private void addLoggerField(AST ast) {
		List<?> typeList = actRoot.types();
		TypeDeclaration td = (TypeDeclaration) typeList.get(0);
		
		//若已經加入java logger則不加入
		List<?> bodyList = td.bodyDeclarations();
		String result = "private Logger logger=Logger.getLogger";
		for (Object node: bodyList) {
			if (node instanceof FieldDeclaration) {
				FieldDeclaration test = (FieldDeclaration) node;
				if(test.toString().contains(result))
					return;
			}
		}
		
		//加入private Logger logger = Logger.getLogger(LoggerTest.class.getName());
		VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		//設定logger
		vdf.setName(ast.newSimpleName("logger"));
		
		//vdf的initializer的Method Invocation
		MethodInvocation initMI = ast.newMethodInvocation();
		//設定initMI的Name
		initMI.setName(ast.newSimpleName("getLogger"));
		//設定initMI的Expression
		initMI.setExpression(ast.newSimpleName("Logger"));

		/* 設定arguMI的Expression */
		MethodInvocation arguMI = ast.newMethodInvocation();
		//設定arguMI的Name
		arguMI.setName(ast.newSimpleName("getName"));

		/* 取得class Name */
		ICompilationUnit icu = (ICompilationUnit) actOpenable;
		String javaName = icu.getElementName();
		//濾掉".java"
		String className = javaName.substring(0, javaName.length()-5);
		//設定Expression的Type Literal
		TypeLiteral tl = ast.newTypeLiteral();
		tl.setType(ast.newSimpleType(ast.newName(className)));

		arguMI.setExpression(tl);
		
		//設定initMI的arguments的Method Invocation
		initMI.arguments().add(arguMI);
		vdf.setInitializer(initMI);

		//建立FieldDeclaration
		FieldDeclaration fd = ast.newFieldDeclaration(vdf);
		fd.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
		fd.setType(ast.newSimpleType(ast.newName("Logger")));

		//將Filed寫入TypeTypeDeclaration中，直接放入第一個TypeDeclaration
		td.bodyDeclarations().add(0, fd);
	}

	/**
	 * 將要變更的資料寫回至Document中
	 */
	private void applyChange() {
		//寫回Edit中
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));			
			textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);
		}catch (JavaModelException e) {
			logger.error("[Apply Change My Extract Method] EXCEPTION",e);
		} catch (MalformedTreeException e) {
			logger.error("[Apply Change My Extract Method] EXCEPTION",e);
		}
	}


	/**
	 * 取得JavaProject
	 */
	public IJavaProject getProject(){
		return project;
	}

	/**
	 * 取得ICompilationUnit的名稱
	 */
	public MethodDeclaration getCurrentMethodNode() {
		IResource resource = marker.getResource();
		//取得MethodNode
		findMethod(resource);

		return currentMethodNode;
	}
	
	/**
	 * 設定是否使用已存在的Method
	 */
	public RefactoringStatus setIsRefactoringMethodExist(boolean isMethodExist){
		this.isMethodExist = isMethodExist;
		return new RefactoringStatus();
	}
	
	/**
	 * set methodName變數名稱
	 */
	public RefactoringStatus setNewMethodName(String methodName){
		if (methodName.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Method Name is empty");
		}
		
		boolean isError = false;
		char[] name = methodName.toCharArray();
		//Method名稱第一個字只能為A~Z & a~z
		if (!(name[0] >= 'a' && name[0] <= 'z') && !(name[0] >= 'A' && name[0] <= 'Z'))
			isError = true;

		//Method名稱不能有特殊字元
		for (char c : name) {
			if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')) {
				isError = true;
				break;
			}
		}

		//名稱若不對，顯示錯誤訊息
		if (isError)
			return RefactoringStatus.createFatalErrorStatus(methodName + " is not a valid Java identifer");
		else
			this.methodName = methodName;
		
		return new RefactoringStatus();
	}
	
	/**
	 * set modifier Type
	 */
	public RefactoringStatus setNewMethodModifierType(String modifierType){
		if (modifierType.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		} else {
			this.modifierType = modifierType;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * set Log type
	 */
	public RefactoringStatus setNewMethodLogType(String logType){
		if (logType.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		} else {
			this.logType = logType;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * 設置已存在的Method資訊
	 */
	public RefactoringStatus setExistingMethod(IMethod method){
		if (method == null) {
			return RefactoringStatus.createFatalErrorStatus("Existing Method Field is empty");
		} else {
			existingMethod = method;
			return new RefactoringStatus();
		}
	}
}
