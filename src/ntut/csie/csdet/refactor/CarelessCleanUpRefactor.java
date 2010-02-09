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
	
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	
	//�s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;
	
	private List<CSMessage> CarelessCleanUpList = null;
	
	private boolean isMethodExist = false;
	
	//methodName���ܼƦW��,�w�]�Oclose
	private String methodName;
	
	//modifier��Type�A�w�]�Oprivate
	private String modifierType;
	
	//log��type,�w�]�Oe.printStackTrace
	private String logType;
	
	//�ϥΪ̭Y���Existing Method�A�n�I�s��Method��T
	private IMethod existingMethod;
	
	//Careless CleanUp��Smell Message
	private CSMessage smellMessage = null;
	
	//����귽��Statement
	private ExpressionStatement cleanUpExpressionStatement;
	
	//��{����Try Statement
	private TryStatement tryStatement = null;
	//��{����Finally Statement
	private Block finallyBlock;

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
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		//����check initial condition
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		//��n�ܧ󪺵��G�]��composite�ǥX�h
		Change[] changes = new Change[] {textFileChange};
		CompositeChange change = new CompositeChange("My Extract Method", changes);
		return change;
	}

	@Override
	public String getName() {		
		return "My Extract Method";
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
		boolean isOK = findMethod(resource);
		if(isOK && currentMethodNode != null){
			CarelessCleanUpAnalyzer visitor = new CarelessCleanUpAnalyzer(this.actRoot);
			currentMethodNode.accept(visitor);
			//���ocode smell��List
			CarelessCleanUpList = visitor.getCarelessCleanUpList();	

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

		//���oEH Smell����T
		findSmellMessage();
		
		//�Ytry Statement�̨S��Finally Block,�h�إ�Finally Block
		judgeFinallyBlock(ast);

		//�R��fos.close();
		deleteCleanUpLine();

		//�bfinally���[�JcloseStream(fos)
		addMethodInFinally(ast);

		//�YMethod���s�b�A�إ߷sMethod
		if (!isMethodExist)
			addExtractMethod(ast);

		//�g�^Edit��
		applyChange();
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
	 * �P�_Try Statment�O�_��Finally Block�A�Y�L�h�إ�Finally Block
	 */
	private void judgeFinallyBlock(AST ast) {
		//���o��k���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration) currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();

		//TODO ���Ҽ{Nested Try block���Ϊp
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
	 * �R��Careless CleanUp Smell �Ӧ�
	 */
	private void deleteCleanUpLine() {
		boolean isDeleted = false;
		//�M��Try Block
		isDeleted = deleteBlockStatement(tryStatement.getBody());

		List<CatchClause> catchs = tryStatement.catchClauses();
		for (int j=0; j < catchs.size() && !isDeleted; j++) {
			CatchClause catchClause = catchs.get(j);
			//�M��Catch Clause
			isDeleted = deleteBlockStatement(catchClause.getBody());
		}
	}

	/**
	 * �R��Block��Smell Statement
	 * @param block
	 */
	private boolean deleteBlockStatement(Block block) {
		List<?> statments = block.statements();
		//���Try Statement�̬O�_�������ʪ��{���X,�Y���h����
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
		//���o��T
		MethodInvocation delLineMI = (MethodInvocation) cleanUpExpressionStatement.getExpression();
		Expression exp = delLineMI.getExpression();
		SimpleName sn = (SimpleName) exp;
		
		//�s�WMethod Declaration
		MethodDeclaration newMD=ast.newMethodDeclaration();

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
		SingleVariableDeclaration svd=ast.newSingleVariableDeclaration();
		svd.setType(ast.newSimpleType(ast.newSimpleName(exp.resolveTypeBinding().getName().toString())));
		svd.setName(ast.newSimpleName(sn.getIdentifier()));
		newMD.parameters().add(svd);

		//�]�wbody
		Block block=ast.newBlock();
		newMD.setBody(block);
		
		TryStatement ts = addTryStatement(ast, delLineMI);

		//�N�s�W��try statement�[�i��
		block.statements().add(ts);
		//�Nnew MD�[�J
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

		//�s�W��Method Invocation
		MethodInvocation newMI = ast.newMethodInvocation();

		if (!isMethodExist) {
			//�]�wMI��name
			newMI.setName(ast.newSimpleName(methodName));
			//�]�wMI���Ѽ�
			newMI.arguments().add(ast.newSimpleName(simpleName.getIdentifier()));
		} else {
			try {
				//Private�ɤ��S�O�ʧ@
				//if ((existingMethod.getFlags() & Flags.AccPrivate) != 0)

				IType classType = (IType) existingMethod.getParent();
				//�Y��Public
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
				
				//�]�wMI��name
				newMI.setName(ast.newSimpleName(existingMethod.getElementName()));
				//�]�wMI���Ѽ�
				newMI.arguments().add(ast.newSimpleName(simpleName.getIdentifier()));

			} catch (JavaModelException e) {
				logger.error("[Java Method] EXCEPTION", e);
			}
		}

		ExpressionStatement es = ast.newExpressionStatement((Expression) newMI);
		finallyBlock.statements().add(es);
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
		List<ImportDeclaration> importList = actRoot.imports();
		for(ImportDeclaration id : importList)
			if(id.getName().getFullyQualifiedName().contains(classType.getFullyQualifiedName()))
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
		tsBody.statements().add(cleanUpExpressionStatement);
		
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
	 * �[�Jlogger.info(e.getMessage());
	 * @param ast
	 * @param cc
	 */
	private void addJavaLoggerStatement(AST ast, CatchClause cc) {
		//import java.util.logging.Logger;
		addJavaLoggerLibrary();
		
		//private Logger logger = Logger.getLogger(CarelessCleanUpTest.class.getName());
		addLoggerField(ast);

		//�s�W��Method Invocation
		MethodInvocation catchMI=ast.newMethodInvocation();
		//�]�wMI��name
		catchMI.setName(ast.newSimpleName("info"));
		//�]�wMI��Expression
		catchMI.setExpression(ast.newSimpleName("logger"));
		
		//�]�wcatch��body��Method Invocation
		MethodInvocation cbMI = ast.newMethodInvocation();
		//�]�wcbMI��Name
		cbMI.setName(ast.newSimpleName("info"));
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
		List<ImportDeclaration> importList = actRoot.imports();
		for(ImportDeclaration id : importList){
			if(id.getName().getFullyQualifiedName().contains("java.util.logging.Logger")){
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
		List<AbstractTypeDeclaration> typeList = actRoot.types();
		TypeDeclaration td = (TypeDeclaration) typeList.get(0);
		td.bodyDeclarations().add(0, fd);
	}

	/**
	 * �N�n�ܧ󪺸�Ƽg�^��Document��
	 */
	private void applyChange(){
		//�g�^Edit��
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
