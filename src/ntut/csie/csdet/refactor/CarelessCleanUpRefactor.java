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
 * Careless CleanUp Refactoring������ާ@���b�o��class��
 * @author Min, Shiau
 */
public class CarelessCleanUpRefactor extends Refactoring {
	private static Logger logger = LoggerFactory.getLogger(CarelessCleanUpRefactor.class);
	
	private IJavaProject project;
		
	//�ϥΪ̩��I�諸Marker
	private IMarker marker;
	
	private IOpenable actOpenable;
	
	private TextFileChange textFileChange;
	
	// �s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	
	// �s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;
	
	// ����Method���Ҧ���Careless CleanUp
	private List<MarkerInfo> CarelessCleanUpList = null;
	
	// Method�O�_�s�b�A�O�G�s�WCaller Method �A�_�G�s�WRelease Method
	private boolean isMethodExist = false;
	
	// methodName���ܼƦW��,�w�]�Oclose
	private String methodName;

	// modifier��Type�A�w�]�Oprivate
	private String modifierType;

	// log��type,�w�]�Oe.printStackTrace
	private String logType;
	
	// �ϥΪ̭Y���Existing Method�A�n�I�s��Method��T
	private IMethod existingMethod;
	
	// Careless CleanUp��Smell Message
	private MarkerInfo smellMessage = null;
	
	// ����귽��Statement
	private ExpressionStatement cleanUpExpressionStatement;
	
	// Try Block�bMethod�̪���m
	private int tryIndex = -1;

	/**
	 * �����ʧ@
	 */
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		//�h�ק�AST Tree
		collectChange(marker.getResource());
		//����check final condition
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}
	
	/**
	 * ��l�ʧ@
	 * �T�{��l���A�O�_�ŦX
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		//����check initial condition
		RefactoringStatus status;

		boolean isOK = findMethod(marker.getResource());
		if(isOK && currentMethodNode != null){
			CarelessCleanupVisitor visitor = new CarelessCleanupVisitor(actRoot);
			currentMethodNode.accept(visitor);
			//���ocode smell��List
			CarelessCleanUpList = visitor.getCarelessCleanupList();
		}

		//���oEH Smell����T
		findSmellMessage();

		// ���oSmell�Ҧb��Try Block��
		TryStatement tryStatement = findTryStatement();
		
		// �P�_Smell�O�_���If�����A�Y�O��If����Statement�Ƥ���L�W1��
		boolean isSizeSafe = detectIfStatementSize(tryStatement.getBody());

		if (isSizeSafe)
			status = new RefactoringStatus();
		else
			status = RefactoringStatus.createFatalErrorStatus("�P�_�������䥦���{���X");
		
		return status;
	}
	
	/**
	 * �R��Block��Smell Statement
	 * @param block
	 */
	private boolean detectIfStatementSize(Block block) {
		List<?> statements = block.statements();
		//���Try Statement�̬O�_�������ʪ��{���X,�Y���h����
		for(int i=0; i < statements.size(); i++) {
			if (statements.get(i) instanceof IfStatement) {
				IfStatement aStatement = (IfStatement) statements.get(i);
				// �Y��If�P�_���A�B�]�tSmell��T
				if (aStatement.getStartPosition() <= smellMessage.getPosition() &&
					aStatement.getStartPosition() + aStatement.getLength()
												  >= smellMessage.getPosition()) {

					IfStatement ifStatement = (IfStatement) statements.remove(i);
					Statement thenStatement = ifStatement.getThenStatement();
					// �p�G��Block ( if���["{" "}" )
					if (thenStatement instanceof Block) {
						Block ifBlock = (Block) ifStatement.getThenStatement();
						int ifSize = ifBlock.statements().size();
						if (ifSize == 1)
							return true;
						else
							return false;
					// ����Block (if �᪽����Statement)
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
		// 2010.07.20 ���e���g�k�APreview��Token���|�ܦ�
		// ��n�ܧ󪺵��G�]��composite�ǥX�h
		//Change[] changes = new Change[] {textFileChange};
		//CompositeChange change = new CompositeChange("My Extract Method", changes);

		/* �Ѧ�Eclipse Code
		 * org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring
		 * createChange method���g�k */
		String name = "Extract CleanUp Method";
		ICompilationUnit unit = (ICompilationUnit) this.actOpenable;
		CompilationUnitChange result = new CompilationUnitChange(name, unit);
		result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

		//MultiTextEdit root= new MultiTextEdit();
		//root.addChild(edits);

		// �N�קﵲ�G�]�m�bCompilationUnitChange
		TextEdit edits = textFileChange.getEdit();
		result.setEdit(edits);
		// �N�קﵲ�G�]��Group�A�|��ܦbPreview�W��`�I�C
		result.addTextEditGroup(new TextEditGroup("Careless Clean Up Method", 
								new TextEdit[] {edits} ));

		return result;
	}

	@Override
	public String getName() {		
		return "Extract CleanUp Method";
	}
	
	/**
	 * ��marker�Ƕi�ӨѦ�class�s���@��code smell��T
	 * @param marker
	 */
	public void setMarker(IMarker marker){
		this.marker = marker;
		this.project = JavaCore.create(marker.getResource().getProject());
	}
	
	/**
	 * parse AST Tree�è��o�n�ק諸method node
	 * @param resource
	 */
	private void collectChange(IResource resource){
		//���o�n�ק諸CompilationUnit
		if (findMethod(resource) && currentMethodNode != null) {
			extractMethod();
		}
	}
	
	/**
	 * ���o�ثe�n�Q�ק諸method node
	 * @param resource
	 * @return
	 */
	private boolean findMethod(IResource resource) {
		//���o�n�ק諸CompilationUnit
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

				//���o��class�Ҧ���method
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<ASTNode> methodList = methodCollector.getMethodList();
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);

				//���o�ثe�n�Q�ק諸method node
				this.currentMethodNode = methodList.get(Integer.parseInt(methodIdx));

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

		// ���oEH Smell����T
		findSmellMessage();

		// ���oSmell�Ҧb��Try Block��
		TryStatement tryStatement = findTryStatement();
		
		// �Ytry Statement�̨S��Finally Block,�h�إ�Finally Block
		Block finallyBlock = addFinallyBlock(ast, tryStatement);

		// �R��fos.close();
		deleteCleanUpLine(ast, tryStatement);

		// �Yfos�O�bTry Block�ŧi�A�N������Try Block�~
		moveInstance(ast, tryStatement);

		// �bfinally���[�JcloseStream(fos)
		addMethodInFinally(ast, finallyBlock);

		// �g�^Edit��
		applyChange();
	}

	/**
	 * �NInstance����Try Catch�~
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
					// �Y�ѼƫئbTry Block��
					if (fragment.getName().toString().equals(expression.toString())) {
						/* �N   InputStream fos = new ImputStream();
						 * �אּ fos = new InputStream();
						 * */
						Assignment assignment = ast.newAssignment();
						assignment.setOperator(Assignment.Operator.ASSIGN);
						// fos
						assignment.setLeftHandSide(ast.newSimpleName(fragment.getName().toString()));
						// new InputStream
						Expression init = fragment.getInitializer();
						ASTNode copyNode = ASTNode.copySubtree(init.getAST(), init);
						assignment.setRightHandSide((Expression) copyNode);

						// �Nfos = new ImputStream(); ������쥻���{����
						if(assignment.getRightHandSide().getNodeType() != ASTNode.NULL_LITERAL){
							ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
							tryStatement.getBody().statements().set(i, expressionStatement);
						}else{
							//�p�G���Ӫ��{���X�O�]�winstance��l��null�A���N����������
							tryStatement.getBody().statements().remove(i);
						}

						// InputStream fos = null
						// �Nnew�ʧ@������null
						fragment.setInitializer(ast.newNullLiteral());
						// �[�ܭ쥻�{���X���e
						MethodDeclaration md = (MethodDeclaration) currentMethodNode;
						md.getBody().statements().add(tryIndex, variable);
						break;
					}
				}
			}
		}
	}

	/**
	 * �M��R�����{���X
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
	 * ���oSmell�Ҧb��Try Statement
	 * @return	�M��o��GTry Statement �䤣��GNull
	 */
	private TryStatement findTryStatement() {
		//���o��k���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration) currentMethodNode;
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
				// �p�GSmell���Try��
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
	 * �P�_Try Statement�O�_��Finally Block�A�Y�L�h�إ�Finally Block
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
	 * �R��Careless CleanUp Smell �Ӧ�
	 * @param ast 
	 * @param tryStatement 
	 */
	private void deleteCleanUpLine(AST ast, TryStatement tryStatement) {
		boolean isDeleted = false;
		//�M��Try Block
		isDeleted = deleteBlockStatement(tryStatement.getBody(), ast);

		List<?> catchs = tryStatement.catchClauses();
		for (int j=0; j < catchs.size() && !isDeleted; j++) {
			CatchClause catchClause = (CatchClause)catchs.get(j);
			//�M��Catch Clause
			isDeleted = deleteBlockStatement(catchClause.getBody(), ast);
		}
		
		if(!isDeleted && tryStatement.getFinally() != null) {
			deleteBlockStatement(tryStatement.getFinally(), ast);
		}
	}

	/**
	 * �R��Block��Smell Statement
	 * @param block �n�Q�R����Block
	 * @param ast
	 * @return		�O�_�R��
	 */
	private boolean deleteBlockStatement(Block block, AST ast) {
		List<?> statements = block.statements();
		//���Try Statement�̬O�_�������ʪ��{���X,�Y���h����
		for(int i=0; i < statements.size(); i++) {
			if (statements.get(i) instanceof ExpressionStatement) {
				ExpressionStatement aStatement = (ExpressionStatement) statements.get(i);
				// �p�G�@��Try Catch�����h��Careless CleanUp Smell
				// �Φ�m�P�_�~�ॿ�T�P�_�X�O���@��
				if (aStatement.getStartPosition() == smellMessage.getPosition()) {
					cleanUpExpressionStatement = (ExpressionStatement) statements.remove(i);
					return true;
				}
			} else if (statements.get(i) instanceof IfStatement) {
				IfStatement aStatement = (IfStatement) statements.get(i);
				// �Y��If�P�_���A�B�]�tSmell��T
				if (aStatement.getStartPosition() <= smellMessage.getPosition() &&
					aStatement.getStartPosition() + aStatement.getLength()
												  >= smellMessage.getPosition()) {

					IfStatement ifStatement = (IfStatement) statements.remove(i);
					Statement thenStatement = ifStatement.getThenStatement();
					// �p�G��Block ( if���["{" "}" )
					if (thenStatement instanceof Block) {
						Block ifBlock = (Block) ifStatement.getThenStatement();
						return deleteBlockStatement(ifBlock, ast);
					// ����Block (if �᪽����Statement)
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
		//���o��T
		MethodInvocation delLineMI = (MethodInvocation) cleanUpExpressionStatement.getExpression();
		Expression exp = delLineMI.getExpression();
		SimpleName sn = (SimpleName) exp;

		//�s�WMethod Declaration
		MethodDeclaration newMD = ast.newMethodDeclaration();

		//�]�w�s�����O(public)
		if (modifierType == "public")
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));			
		else if (modifierType == "protected")
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD));						
		else
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));									

		//�]�wreturn type
		newMD.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		//�]�wMD���W��
		newMD.setName(ast.newSimpleName(methodName));
		//�]�w�Ѽ�
		SingleVariableDeclaration svd = ast.newSingleVariableDeclaration();
		svd.setType(ast.newSimpleType(ast.newSimpleName(exp.resolveTypeBinding().getName().toString())));
		svd.setName(ast.newSimpleName(sn.getIdentifier()));
		newMD.parameters().add(svd);

		//�]�wbody
		Block block = ast.newBlock();
		newMD.setBody(block);
		
		TryStatement ts = addTryStatement(ast, delLineMI);

		//�N�s�W��try statement�[�i��
		block.statements().add(ts);
		//�Nnew MD�[�J
		List<AbstractTypeDeclaration> typeList = actRoot.types();
		TypeDeclaration td = (TypeDeclaration) typeList.get(0);
		td.bodyDeclarations().add(newMD);
	}

	/**
	 * �NRelease��Method�[�J��Finally Block��
	 * @param ast	MethodNode
	 * @param finallyBlock 
	 */
	private void addMethodInFinally(AST ast, Block finallyBlock) {
		// e.g. fos.close();
		MethodInvocation delLineMI = (MethodInvocation) cleanUpExpressionStatement.getExpression();
		// e.g. fos
		Expression expression = delLineMI.getExpression();

		// �Y�Ӧ欰Method (e.g. closeFile(fos)) �h���������沾��Finally Block��
		if (expression == null) {
			finallyBlock.statements().add(cleanUpExpressionStatement);
			return;
		}

		MethodInvocation newMI = null;
		// �YMethod���s�b
		if (!isMethodExist) {
			newMI = createNewMethod(ast, expression, methodName);

			// �YMethod���s�b�A�إ߷sMethod
			addExtractMethod(ast);

		// �YMethod�w�s�b
		} else {
			//�s�W��Method Invocation
			newMI = createNewMethod(ast, expression, existingMethod.getElementName());

			// �]�m�I�sMethod���W��
			createCallerMethod(ast, newMI, finallyBlock);
		}

		ExpressionStatement es = ast.newExpressionStatement((Expression) newMI);
		finallyBlock.statements().add(es);
	}

	/**
	 * �s�WMethod
	 * @param ast
	 * @param expression
	 * @return
	 */
	private MethodInvocation createNewMethod(AST ast, Expression expression, String methodName) {
		SimpleName simpleName = (SimpleName) expression;

		//�s�W��Method Invocation
		MethodInvocation newMI = ast.newMethodInvocation();

		// �]�wMI��name
		newMI.setName(ast.newSimpleName(methodName));

		// �]�wMI���Ѽ�
		newMI.arguments().add(ast.newSimpleName(simpleName.getIdentifier()));

		return newMI;
	}

	/**
	 * �s�WCaller Method
	 * @param ast
	 * @param newMI
	 * @param finallyBlock 
	 */
	private void createCallerMethod(AST ast, MethodInvocation newMI, Block finallyBlock) {
		try {
			//Private�ɤ��S�O�ʧ@
			//if ((existingMethod.getFlags() & Flags.AccPrivate) != 0)

			IType classType = (IType) existingMethod.getParent();
			//�Y��Public
			if ((existingMethod.getFlags() & Flags.AccPublic) != 0) {
				// �YMethod��Static: �����I�s
				if ((existingMethod.getFlags() & Flags.AccStatic) != 0) {
					newMI.setExpression(ast.newSimpleName(classType.getElementName()));

				// �Y�DStatic Method: ��New�A�I�s
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

			// �s�WImport��T
			addImportPackage(classType);

		} catch (JavaModelException e) {
			logger.error("[Java Method] EXCEPTION", e);
		}
	}

	/**
	 * �[�JPublic Method��Import Package
	 * @param classType
	 */
	private void addImportPackage(IType classType) {
		//�YPackage�ۦP�A�h���[import
		String extractMethodPackage = classType.getPackageFragment().getElementName();
		String localMethodPackage = actRoot.getPackage().getName().toString();
		if (extractMethodPackage.equals(localMethodPackage))
			return;

		//�YPackage�[�J�L�]���[
		List<?> importList = actRoot.imports();
		for(Object id : importList)
			if(((ImportDeclaration)id).getName().getFullyQualifiedName().contains(classType.getFullyQualifiedName()))
				return;

		//���p�S��import,�N�[�J��AST��
		AST rootAst = actRoot.getAST(); 
		ImportDeclaration imp = rootAst.newImportDeclaration();
		imp.setName(rootAst.newName(classType.getFullyQualifiedName()));
		actRoot.imports().add(imp);
	}

	/**
	 * �[�JTry Statement
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
		//�إ� obj != null
		InfixExpression in = ast.newInfixExpression();
		in.setOperator(InfixExpression.Operator.NOT_EQUALS);
		in.setLeftOperand(ast.newSimpleName(delLineMI.getExpression().toString()));
		in.setRightOperand(ast.newNullLiteral());

		//�إ� if Satement
		IfStatement ifStatement = ast.newIfStatement();
		ifStatement.setExpression(in);
		//�[�JRelease Source Code
		ifStatement.setThenStatement(cleanUpExpressionStatement);
		//�[��Try Block����
		tsBody.statements().add(ifStatement);

		//��try �[�J�@��Catch clause
		List<CatchClause> catchStatement = ts.catchClauses();
		CatchClause cc = ast.newCatchClause();
		
		//�s��{���X�ҩߥX���ҥ~����
		ITypeBinding[] iType;
		iType = delLineMI.resolveMethodBinding().getExceptionTypes();
	
		//�إ�catch��type�� catch(... ex)
		SingleVariableDeclaration svdCatch = ast.newSingleVariableDeclaration();
		svdCatch.setType(ast.newSimpleType(ast.newSimpleName(iType[0].getName())));
		svdCatch.setName(ast.newSimpleName("e"));
		cc.setException(svdCatch);

		//�[�Jcatch��body
		if(logType.equals("e.printStackTrace();"))
			addPrintStackStatement(ast, cc);
		else
			addJavaLoggerStatement(ast, cc);

		catchStatement.add(cc);
		return ts;
	}

	/**
	 * �[�Je.printStatckTrace
	 * @param ast
	 * @param cc
	 */
	private void addPrintStackStatement(AST ast, CatchClause cc) {
		//�s�W��Method Invocation
		MethodInvocation catchMI = ast.newMethodInvocation();
		//�]�wMI��name
		catchMI.setName(ast.newSimpleName("printStackTrace"));
		//�]�wMI��Expression
		catchMI.setExpression(ast.newSimpleName("e"));			
		ExpressionStatement catchES = ast.newExpressionStatement((Expression)catchMI);
		cc.getBody().statements().add(catchES);
	}

	/**
	 * �[�Jlogger.warning(e.getMessage());
	 * @param ast
	 * @param cc
	 */
	private void addJavaLoggerStatement(AST ast, CatchClause cc) {
		//import java.util.logging.Logger;
		addJavaLoggerLibrary();
		
		//private Logger logger = Logger.getLogger(CarelessCleanUpTest.class.getName());
		addLoggerField(ast);
		
		//�]�wcatch��body��Method Invocation
		MethodInvocation cbMI = ast.newMethodInvocation();
		//�]�wcbMI��Name
		cbMI.setName(ast.newSimpleName("warning"));
		//�]�wcbMI��Expression
		cbMI.setExpression(ast.newSimpleName("logger"));
		
		//�]�wcbMI��arguments��Method Invocation
		MethodInvocation cbarguMI = ast.newMethodInvocation();
		cbarguMI.setName(ast.newSimpleName("getMessage"));
		cbarguMI.setExpression(ast.newSimpleName("e"));
		
		cbMI.arguments().add(cbarguMI);
		
		ExpressionStatement catchES = ast.newExpressionStatement((Expression)cbMI);
		cc.getBody().statements().add(catchES);
	}

	/**
	 *  �[�Jimport java.util.logging.Logger;
	 */
	private void addJavaLoggerLibrary() {
		//�P�_�O�_��import java.util.logging.Logger
		boolean isImportLibrary = false;
		List<?> importList = actRoot.imports();
		for(Object id : importList){
			if(((ImportDeclaration)id).getName().getFullyQualifiedName().contains("java.util.logging.Logger")){
				isImportLibrary = true;
			}
		}
		
		//���p�S��import,�N�[�J��AST��
		AST rootAst = actRoot.getAST(); 
		if (!isImportLibrary) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName("java.util.logging.Logger"));
			actRoot.imports().add(imp);
		}
	}
	
	/**
	 * �[�Jprivate Logger logger = Logger.getLogger(CarelessCleanUpTest.class.getName());
	 * @param ast
	 */
	private void addLoggerField(AST ast) {
		List<?> typeList = actRoot.types();
		TypeDeclaration td = (TypeDeclaration) typeList.get(0);
		
		//�Y�w�g�[�Jjava logger�h���[�J
		List<?> bodyList = td.bodyDeclarations();
		String result = "private Logger logger=Logger.getLogger";
		for (Object node: bodyList) {
			if (node instanceof FieldDeclaration) {
				FieldDeclaration test = (FieldDeclaration) node;
				if(test.toString().contains(result))
					return;
			}
		}
		
		//�[�Jprivate Logger logger = Logger.getLogger(LoggerTest.class.getName());
		VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		//�]�wlogger
		vdf.setName(ast.newSimpleName("logger"));
		
		//vdf��initializer��Method Invocation
		MethodInvocation initMI = ast.newMethodInvocation();
		//�]�winitMI��Name
		initMI.setName(ast.newSimpleName("getLogger"));
		//�]�winitMI��Expression
		initMI.setExpression(ast.newSimpleName("Logger"));

		/* �]�warguMI��Expression */
		MethodInvocation arguMI = ast.newMethodInvocation();
		//�]�warguMI��Name
		arguMI.setName(ast.newSimpleName("getName"));

		/* ���oclass Name */
		ICompilationUnit icu = (ICompilationUnit) actOpenable;
		String javaName = icu.getElementName();
		//�o��".java"
		String className = javaName.substring(0, javaName.length()-5);
		//�]�wExpression��Type Literal
		TypeLiteral tl = ast.newTypeLiteral();
		tl.setType(ast.newSimpleType(ast.newName(className)));

		arguMI.setExpression(tl);
		
		//�]�winitMI��arguments��Method Invocation
		initMI.arguments().add(arguMI);
		vdf.setInitializer(initMI);

		//�إ�FieldDeclaration
		FieldDeclaration fd = ast.newFieldDeclaration(vdf);
		fd.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
		fd.setType(ast.newSimpleType(ast.newName("Logger")));

		//�NFiled�g�JTypeTypeDeclaration���A������J�Ĥ@��TypeDeclaration
		td.bodyDeclarations().add(0, fd);
	}

	/**
	 * �N�n�ܧ󪺸�Ƽg�^��Document��
	 */
	private void applyChange() {
		//�g�^Edit��
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
	 * ���oJavaProject
	 */
	public IJavaProject getProject(){
		return project;
	}

	/**
	 * ���oICompilationUnit���W��
	 */
	public ASTNode getCurrentMethodNode(){
		IResource resource = marker.getResource();
		//���oMethodNode
		findMethod(resource);

		return currentMethodNode;
	}
	
	/**
	 * �]�w�O�_�ϥΤw�s�b��Method
	 */
	public RefactoringStatus setIsRefactoringMethodExist(boolean isMethodExist){
		this.isMethodExist = isMethodExist;
		return new RefactoringStatus();
	}
	
	/**
	 * set methodName�ܼƦW��
	 */
	public RefactoringStatus setNewMethodName(String methodName){
		if (methodName.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Method Name is empty");
		}
		
		boolean isError = false;
		char[] name = methodName.toCharArray();
		//Method�W�ٲĤ@�Ӧr�u�ରA~Z & a~z
		if (!(name[0] >= 'a' && name[0] <= 'z') && !(name[0] >= 'A' && name[0] <= 'Z'))
			isError = true;

		//Method�W�٤��঳�S��r��
		for (char c : name) {
			if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')) {
				isError = true;
				break;
			}
		}

		//�W�٭Y����A��ܿ��~�T��
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
	 * �]�m�w�s�b��Method��T
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
