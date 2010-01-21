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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
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
 * @author Min
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
	
	private List<CSMessage> CarelessCleanUpList=null;
	
	String msgIdx;
	String methodIdx;
	
	//methodName���ܼƦW��,�w�]�Oclose
	private String methodName;
	
	//modifier��Type�A�w�]�Oprivate
	private String modifierType;
	
	//log��type,�w�]�Oe.printStackTrace
	private String logType;
	
	//���R�����{���X��T
	private String delLine;
	
	private ExpressionStatement delLineES;
	
	private Block finallyBlock;

	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		//�h�ק�AST Tree
		collectChange(marker.getResource());
		//����check final condition
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}
	
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
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			try {
				IJavaElement javaElement = JavaCore.create(resource);
				
				if (javaElement instanceof IOpenable) {
					this.actOpenable = (IOpenable) javaElement;
				}
				
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
				methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				
				//���o�ثe�n�Q�ק諸method node
				this.currentMethodNode = methodList.get(Integer.parseInt(methodIdx));
				
				if(currentMethodNode != null){
					CarelessCleanUpAnalyzer visitor = new CarelessCleanUpAnalyzer(this.actRoot);
					currentMethodNode.accept(visitor);
					//���ocode smell��List
					CarelessCleanUpList = visitor.getCarelessCleanUpList();	
					
					myExtractMethod();
					
				}
			
			}catch (Exception ex) {
				logger.error("[Find CS Method] EXCEPTION ",ex);
			}
		}
	}
	/**
	 * set methodName�ܼƦW��
	 */
	public RefactoringStatus setMethodName(String methodName){
		if(methodName.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Method Name is empty");
		}else{
			this.methodName = methodName;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * set modifier Type
	 */
	public RefactoringStatus setModifierType(String modifierType){
		if(modifierType.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.modifierType = modifierType;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * set Log type
	 */
	public RefactoringStatus setLogType(String logType){
		if(logType.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.logType = logType;
			return new RefactoringStatus();
		}
	}
	
	private void myExtractMethod(){
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();
					
		//�Ytry Statement�̨S��Finally Block,�إ�Finally Block
		judgeFinallyBlock(ast);
		
		//���o���R�����{���X
		findDelLine();
		
		//�R��fos.close();
		delLine();
		
		//�bfinally���[�JcloseStream(fos)
		extractMethod(ast);
		
		//�g�^Edit��
		applyChange();
	}
	
	
	private void findDelLine(){
		CSMessage csMsg=CarelessCleanUpList.get(Integer.parseInt(msgIdx));
		delLine=csMsg.getStatement();
	}
	
	/**
	 * �P�_Try Statment�O�_��Finally Block
	 * @return boolean
	 */
	private void judgeFinallyBlock(AST ast){
		//���o��k���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();
		
		for(int i=0;i<statement.size();i++){
			if(statement.get(i) instanceof TryStatement){
				TryStatement ts=(TryStatement) statement.get(i);
				Block finallyBlock=ts.getFinally();
				if(finallyBlock==null){
					Block block=ast.newBlock();
					ts.setFinally(block);
					break;
				}
			}
		}
	}
	
	private void delLine(){
		//���o��k���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();
		
		for(int i=0;i<statement.size();i++){
			if(statement.get(i) instanceof TryStatement){
				TryStatement ts=(TryStatement) statement.get(i);
				Block tsBlock=ts.getBody();
				List<?> tsStat=tsBlock.statements();
				//���Try Statement�̬O�_�������ʪ��{���X,�Y���h����
				for(int j=0; j<tsStat.size(); j++){
					String temp = tsStat.get(j).toString();
					if(temp.contains(delLine)){
						delLineES=(ExpressionStatement) tsStat.get(j);
						tsStat.remove(j);						
						break;
					}
				}
			}
		}
	}
	
	private void extractMethod(AST ast){
		//���o��T
		MethodInvocation delLineMI=(MethodInvocation) delLineES.getExpression();
		Expression exp=delLineMI.getExpression();
		SimpleName sn=(SimpleName)exp;
		
		
		//���o��k���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();
		//Block finallyBlock;
		for(int i=0;i<statement.size();i++){
			if(statement.get(i) instanceof TryStatement){
				TryStatement ts=(TryStatement) statement.get(i);
				finallyBlock=ts.getFinally();
				break;
			}
		}
		
		//�s�W��Method Invocation
		MethodInvocation newMI=ast.newMethodInvocation();
		//�]�wMI��name
		newMI.setName(ast.newSimpleName(methodName));
		//�]�wMI���Ѽ�
		newMI.arguments().add(ast.newSimpleName(sn.getIdentifier()));
		
		ExpressionStatement es=ast.newExpressionStatement((Expression)newMI);
		finallyBlock.statements().add(es);
		
		//�s�WMethod Declaration
		MethodDeclaration newMD=ast.newMethodDeclaration();
		
		//�]�w�s�����O(public)
		if(modifierType=="public"){
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));			
		}else if(modifierType=="protected"){
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD));						
		}else{
			newMD.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));									
		}
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
		
		TryStatement ts = ast.newTryStatement();
		Block tsBody=ts.getBody();
		tsBody.statements().add(delLineES);
		
		//��try �[�J�@��Catch clause
		List<CatchClause> catchStatement = ts.catchClauses();
		CatchClause cc = ast.newCatchClause();
		
		//�s��{���X�ҩߥX���ҥ~����
		ITypeBinding[] iType;
		iType=delLineMI.resolveMethodBinding().getExceptionTypes();
	
		//�إ�catch��type�� catch(... ex)
		SingleVariableDeclaration svdCatch = ast.newSingleVariableDeclaration();
		svdCatch.setType(ast.newSimpleType(ast.newSimpleName(iType[0].getName())));
		svdCatch.setName(ast.newSimpleName("e"));
		cc.setException(svdCatch);

		//�[�Jcatch��body
		if(logType=="e.printStackTrace();"){
			//�s�W��Method Invocation
			MethodInvocation catchMI=ast.newMethodInvocation();
			//�]�wMI��name
			catchMI.setName(ast.newSimpleName("printStackTrace"));
			//�]�wMI��Expression
			catchMI.setExpression(ast.newSimpleName("e"));			
			ExpressionStatement catchES=ast.newExpressionStatement((Expression)catchMI);
			cc.getBody().statements().add(catchES);
		}else{
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
			if(!isImportLibrary){
				ImportDeclaration imp = rootAst.newImportDeclaration();
				imp.setName(rootAst.newName("java.util.logging.Logger"));
				actRoot.imports().add(imp);
			}
			
			//�[�Jprivate Logger logger = Logger.getLogger(LoggerTest.class.getName());
			VariableDeclarationFragment vdf=ast.newVariableDeclarationFragment();
			//�]�wlogger
			vdf.setName(ast.newSimpleName("logger"));
			
			//vdf��initializer��Method Invocation
			MethodInvocation initMI=ast.newMethodInvocation();
			//�]�winitMI��Name
			initMI.setName(ast.newSimpleName("getLogger"));
			//�]�winitMI��Expression
			initMI.setExpression(ast.newSimpleName("Logger"));
			
			//�]�winitMI��arguments��Method Invocation
			MethodInvocation arguMI=ast.newMethodInvocation();
			//�]�warguMI��Name
			arguMI.setName(ast.newSimpleName("getName"));
			
			//�]�warguMI��Expression
			
			//�]�wExpression��Type Literal
			TypeLiteral tl=ast.newTypeLiteral();
			//�]�wtl��Type
			//���oclass Name
			ICompilationUnit icu = (ICompilationUnit) actOpenable;
			String javaName=icu.getElementName();
			String className=javaName.substring(0, javaName.length()-5);
			tl.setType(ast.newSimpleType(ast.newName(className)));
			
			arguMI.setExpression(tl);
			
			initMI.arguments().add(arguMI);
			
			vdf.setInitializer(initMI);

			FieldDeclaration fd=ast.newFieldDeclaration(vdf);
			fd.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
			fd.setType(ast.newSimpleType(ast.newName("Logger")));
			
			List<AbstractTypeDeclaration> typeList=actRoot.types();
			TypeDeclaration td=(TypeDeclaration) typeList.get(0);
			td.bodyDeclarations().add(0, fd);
			
			//�s�W��Method Invocation
			MethodInvocation catchMI=ast.newMethodInvocation();
			//�]�wMI��name
			catchMI.setName(ast.newSimpleName("info"));
			//�]�wMI��Expression
			catchMI.setExpression(ast.newSimpleName("logger"));
			
			//�]�wcatch��body��Method Invocation
			MethodInvocation cbMI=ast.newMethodInvocation();
			//�]�wcbMI��Name
			cbMI.setName(ast.newSimpleName("info"));
			//�]�wcbMI��Expression
			cbMI.setExpression(ast.newSimpleName("logger"));
			
			//�]�wcbMI��arguments��Method Invocation
			MethodInvocation cbarguMI=ast.newMethodInvocation();
			cbarguMI.setName(ast.newSimpleName("getMessage"));
			cbarguMI.setExpression(ast.newSimpleName("e"));
			
			cbMI.arguments().add(cbarguMI);
			
			ExpressionStatement catchES=ast.newExpressionStatement((Expression)cbMI);
			cc.getBody().statements().add(catchES);
			
		}
		catchStatement.add(cc);

		//�N�s�W��try statement�[�i��
		block.statements().add(ts);
		//�Nnew MD�[�J
		List<AbstractTypeDeclaration> typeList=actRoot.types();
		TypeDeclaration td=(TypeDeclaration) typeList.get(0);
		td.bodyDeclarations().add(newMD);
	}
	
	/**
	 * ���oJavaProject
	 */
	public IJavaProject getProject(){
		return project;
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
}
