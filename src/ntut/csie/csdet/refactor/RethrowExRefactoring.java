package ntut.csie.csdet.refactor;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
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
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
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
 * Rethrow Unhandled exception������ާ@���b�o��class��
 * @author chewei
 */

public class RethrowExRefactoring extends Refactoring {
	private static Logger logger = LoggerFactory.getLogger(RethrowExRefactoring.class);
	
	private IJavaProject project;
	
	//�ϥΪ̩ҿ�ܪ�Exception Type
	private IType exType;
	
	//�ϥΪ̩��I�諸Marker
	private IMarker marker;
	
	private IOpenable actOpenable;
	
	// user �Ҷ�g�n��X��Exception,�w�]�ORunTimeException
	private String exceptionType;
	
	private TextFileChange textFileChange;
	
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	
	//�s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;
	
	private List<CSMessage> currentExList = null;
	
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
		CompositeChange change = new CompositeChange("Rethrow Unhandled Exception", changes);
		return change;
	}

	@Override
	public String getName() {		
		return "Rethrow Unhandle Exception";
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
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				
				//���o�ثe�n�Q�ק諸method node
				this.currentMethodNode = methodList.get(Integer.parseInt(methodIdx));
				if(currentMethodNode != null){
					CodeSmellAnalyzer visitor = new CodeSmellAnalyzer(this.actRoot);
					currentMethodNode.accept(visitor);
					String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
					//�P�_�OIgnore Ex or Dummy handler�è��ocode smell��List
					if(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)){
						currentExList = visitor.getIgnoreExList();	
					}else{
						currentExList = visitor.getDummyList();
					}
					//�h�ק�AST Tree�����e
					rethrowException();
				}
			
			}catch (Exception ex) {
				logger.error("[Find CS Method] EXCEPTION ",ex);
			}
		}
	}
	
	/**
	 *�إ�Throw Exception����T 
	 */
	private void rethrowException(){
		try {
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
			
			//�ǳƦbCatch Caluse���[�Jthrow exception
			//���oCode smell����T
			String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
			CSMessage msg = currentExList.get(Integer.parseInt(msgIdx));
			//������method�Ҧ���catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();
			
			//�h���startPosition,��X�n�ק諸catch			
			for (ASTNode cc : catchList){
				if(cc.getStartPosition() == msg.getPosition()){
					SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
					.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
				
					CatchClause clause = (CatchClause)cc;
					//�ۦ�إߤ@��throw statement�[�J
					ThrowStatement ts = ast.newThrowStatement();
					//�Nthrow��variable�ǤJ
					ClassInstanceCreation cic = ast.newClassInstanceCreation();
					//throw new RuntimeException()
					cic.setType(ast.newSimpleType(ast.newSimpleName(exceptionType)));
					//�Nthrow new RuntimeException(ex)�A�����[�J�Ѽ� 
					cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
					
					//���oCatchClause�Ҧ���statement
					List<Statement> statement = clause.getBody().statements();
					//�N�p�ODummy handler�n����print�ҥ~��T���F�貾��
					String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
					if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)){
						deleteStatement(statement);
					}
					//�N��Ƽg�^
					ts.setExpression(cic);
					statement.add(ts);		
					//�[�J��import��Library(�J��RuntimeException�N���Υ[Library)
					if(!exceptionType.equals("RuntimeException"))
						addImportDeclaration();
				}
			}
		
			//�g�^Edit��
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());	
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);
			
		}catch (Exception ex) {
			logger.error("[Rethrow Exception] EXCEPTION ",ex);
//			ex.printStackTrace();
		}
	}
	
	/**
	 * �bRethrow���e,���N������print�r�곣�M����
	 */
	private void deleteStatement(List<Statement> statementTemp){
		// �qCatch Clause�̭���R��ر���
		if(statementTemp.size() != 0){
			for(int i=0;i<statementTemp.size();i++){			
				ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);
				// �J��System.out.print or printStackTrace�N��Lremove��
				if(statement.getExpression().toString().contains("System.out.print")||
						statement.getExpression().toString().contains("printStackTrace")){	
						statementTemp.remove(i);
						//����������ArrayList����m�|���s�վ�L,�ҥH�Q�λ��^���~�򩹤U��ŦX������ò���
						deleteStatement(statementTemp);						
				}			
			}
		}

	}
	
	/**
	 * �P�_�O�_�����[�J��Library,��throw RuntimeException�����p�n�ư�
	 * �]��throw RuntimeException����import Library
	 */
	private void addImportDeclaration(){
		//�P�_�O�_��import library
		boolean isImportLibrary = false;
		List<ImportDeclaration> importList = this.actRoot.imports();
		for(ImportDeclaration id : importList){
			if(exType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName())){
				isImportLibrary = true;
			}
		}
		
		//���p�S��import�N�[�J��AST��
		AST rootAst = this.actRoot.getAST(); 
		if(!isImportLibrary){
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(exType.getFullyQualifiedName()));
			this.actRoot.imports().add(imp);
		}
		
	}
	
	/**
	 * ����user�ҭnthrow��exception type
	 * @param name : exception type
	 */
	public RefactoringStatus setExceptionName(String name){
		//���p�ϥΪ̨S����g����F��,��RefactoringStatus�]��Error
		if(name.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Please Choose an Exception Type");
		}else{
			//���p���g�N��L�s�U��
			this.exceptionType = name;
			return new RefactoringStatus();
		}		
	}
	
	/**
	 * ���oJavaProject
	 */
	public IJavaProject getProject(){
		return project;
	}
	
	/**
	 * �x�s�nThrow��Exception��m(�nimport�ϥ�)
	 * @param type
	 */
	public void setExType(IType type){
		
		this.exType = type;
	}
}
