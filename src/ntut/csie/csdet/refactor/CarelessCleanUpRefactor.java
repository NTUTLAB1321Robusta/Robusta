package ntut.csie.csdet.refactor;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.CarelessCleanUpAnalyzer;
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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
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
	
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	
	private List<CSMessage> CarelessCleanUpList = null;
	
	private boolean isMethodExist = false;
	
	//methodName的變數名稱,預設是close
	private String methodName;
	
	//modifier的Type，預設是private
	private String modifierType;
	
	//log的type,預設是e.printStackTrace
	private String logType;
	
	//使用者若選擇Existing Method，要呼叫的Method資訊
	private IMethod existingMethod;
	
	//Careless CleanUp的Smell Message
	private CSMessage smellMessage = null;
	
	//釋放資源的Statement
	private ExpressionStatement cleanUpExpressionStatement;
	
	//原程式的Try Statement
	private TryStatement tryStatement = null;
	//原程式的Finally Statement
	private Block finallyBlock;

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
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		//不需check initial condition
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		//把要變更的結果包成composite傳出去
		Change[] changes = new Change[] {textFileChange};
		CompositeChange change = new CompositeChange("My Extract Method", changes);
		return change;
	}

	@Override
	public String getName() {		
		return "My Extract Method";
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
		boolean isOK = findMethod(resource);
		if(isOK && currentMethodNode != null){
			CarelessCleanUpAnalyzer visitor = new CarelessCleanUpAnalyzer(this.actRoot);
			currentMethodNode.accept(visitor);
			//取得code smell的List
			CarelessCleanUpList = visitor.getCarelessCleanUpList();	

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
				List<ASTNode> methodList = methodCollector.getMethodList();
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);

				//取得目前要被修改的method node
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

		//取得EH Smell的資訊
		findSmellMessage();
		
		//若try Statement裡沒有Finally Block,則建立Finally Block
		judgeFinallyBlock(ast);

		//刪除fos.close();
		deleteCleanUpLine();

		//在finally中加入closeStream(fos)
		addMethodInFinally(ast);

		//若Method不存在，建立新Method
		if (!isMethodExist)
			addExtractMethod(ast);

		//寫回Edit中
		applyChange();
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
	 * 判斷Try Statment是否有Finally Block，若無則建立Finally Block
	 */
	private void judgeFinallyBlock(AST ast) {
		//取得方法中所有的statement
		MethodDeclaration md = (MethodDeclaration) currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();

		//TODO 未考慮Nested Try block的形況
		for (int i=0; i < statement.size(); i++){
			if (statement.get(i) instanceof TryStatement) {
				TryStatement aTryStatement = (TryStatement) statement.get(i);
				if (aTryStatement.getStartPosition() <= smellMessage.getPosition() &&
					aTryStatement.getStartPosition() + aTryStatement.getLength() >= smellMessage.getPosition()) {
					tryStatement = aTryStatement;
					break;
				}
			}
		}

		assert tryStatement != null;
		if (tryStatement.getFinally() == null) {
			Block block = ast.newBlock();
			tryStatement.setFinally(block);
		}
		finallyBlock = tryStatement.getFinally();
	}
	
	/**
	 * 刪除Careless CleanUp Smell 該行
	 */
	private void deleteCleanUpLine() {
		boolean isDeleted = false;
		//尋找Try Block
		isDeleted = deleteBlockStatement(tryStatement.getBody());

		List<CatchClause> catchs = tryStatement.catchClauses();
		for (int j=0; j < catchs.size() && !isDeleted; j++) {
			CatchClause catchClause = catchs.get(j);
			//尋找Catch Clause
			isDeleted = deleteBlockStatement(catchClause.getBody());
		}
	}

	/**
	 * 刪除Block內Smell Statement
	 * @param block
	 */
	private boolean deleteBlockStatement(Block block) {
		List<?> statments = block.statements();
		//比對Try Statement裡是否有欲移動的程式碼,若有則移除
		for(int i=0; i < statments.size(); i++) {
			String temp = statments.get(i).toString();

			if (statments.get(i) instanceof ExpressionStatement) {
				ExpressionStatement aStatement = (ExpressionStatement) statments.get(i);
				if (aStatement.getStartPosition() == smellMessage.getPosition()) {
					cleanUpExpressionStatement = (ExpressionStatement) statments.get(i);
					statments.remove(i);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Add New Method
	 * @param ast
	 */
	private void addExtractMethod(AST ast) {
		//取得資訊
		MethodInvocation delLineMI = (MethodInvocation) cleanUpExpressionStatement.getExpression();
		Expression exp = delLineMI.getExpression();
		SimpleName sn = (SimpleName) exp;
		
		//新增Method Declaration
		MethodDeclaration newMD=ast.newMethodDeclaration();

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
		SingleVariableDeclaration svd=ast.newSingleVariableDeclaration();
		svd.setType(ast.newSimpleType(ast.newSimpleName(exp.resolveTypeBinding().getName().toString())));
		svd.setName(ast.newSimpleName(sn.getIdentifier()));
		newMD.parameters().add(svd);

		//設定body
		Block block=ast.newBlock();
		newMD.setBody(block);
		
		TryStatement ts = addTryStatement(ast, delLineMI);

		//將新增的try statement加進來
		block.statements().add(ts);
		//將new MD加入
		List<AbstractTypeDeclaration> typeList = actRoot.types();
		TypeDeclaration td=(TypeDeclaration) typeList.get(0);
		td.bodyDeclarations().add(newMD);
	}

	/**
	 * @param ast
	 * @param sn
	 */
	private void addMethodInFinally(AST ast) {
		MethodInvocation delLineMI = (MethodInvocation) cleanUpExpressionStatement.getExpression();
		SimpleName simpleName = (SimpleName) delLineMI.getExpression();

		//新增的Method Invocation
		MethodInvocation newMI = ast.newMethodInvocation();

		if (!isMethodExist) {
			//設定MI的name
			newMI.setName(ast.newSimpleName(methodName));
			//設定MI的參數
			newMI.arguments().add(ast.newSimpleName(simpleName.getIdentifier()));
		} else {
			try {
				//Private時不特別動作
				//if ((existingMethod.getFlags() & Flags.AccPrivate) != 0)

				IType classType = (IType) existingMethod.getParent();
				//若為Public
				if ((existingMethod.getFlags() & Flags.AccPublic) != 0) {

					if ((existingMethod.getFlags() & Flags.AccStatic) != 0) {
						newMI.setExpression(ast.newSimpleName(classType.getElementName()));
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
				
				addImportPackage(classType);
				
				//設定MI的name
				newMI.setName(ast.newSimpleName(existingMethod.getElementName()));
				//設定MI的參數
				newMI.arguments().add(ast.newSimpleName(simpleName.getIdentifier()));

			} catch (JavaModelException e) {
				logger.error("[Java Method] EXCEPTION", e);
			}
		}

		ExpressionStatement es = ast.newExpressionStatement((Expression) newMI);
		finallyBlock.statements().add(es);
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
		List<ImportDeclaration> importList = actRoot.imports();
		for(ImportDeclaration id : importList)
			if(id.getName().getFullyQualifiedName().contains(classType.getFullyQualifiedName()))
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
		tsBody.statements().add(cleanUpExpressionStatement);
		
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
	 * 加入logger.info(e.getMessage());
	 * @param ast
	 * @param cc
	 */
	private void addJavaLoggerStatement(AST ast, CatchClause cc) {
		//import java.util.logging.Logger;
		addJavaLoggerLibrary();
		
		//private Logger logger = Logger.getLogger(CarelessCleanUpTest.class.getName());
		addLoggerField(ast);

		//新增的Method Invocation
		MethodInvocation catchMI=ast.newMethodInvocation();
		//設定MI的name
		catchMI.setName(ast.newSimpleName("info"));
		//設定MI的Expression
		catchMI.setExpression(ast.newSimpleName("logger"));
		
		//設定catch的body的Method Invocation
		MethodInvocation cbMI = ast.newMethodInvocation();
		//設定cbMI的Name
		cbMI.setName(ast.newSimpleName("info"));
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
		List<ImportDeclaration> importList = actRoot.imports();
		for(ImportDeclaration id : importList){
			if(id.getName().getFullyQualifiedName().contains("java.util.logging.Logger")){
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
		List<AbstractTypeDeclaration> typeList = actRoot.types();
		TypeDeclaration td = (TypeDeclaration) typeList.get(0);
		td.bodyDeclarations().add(0, fd);
	}

	/**
	 * 將要變更的資料寫回至Document中
	 */
	private void applyChange(){
		//寫回Edit中
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);
		}catch (JavaModelException e) {
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
	public ASTNode getCurrentMethodNode(){
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
