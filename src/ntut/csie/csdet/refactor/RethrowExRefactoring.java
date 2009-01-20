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

/**
 * Rethrow Unhandled exception������ާ@���b�o��class��
 * @author chewei
 */

public class RethrowExRefactoring extends Refactoring {

	private IJavaProject project;
	
	//�ϥΪ̩ҿ�ܪ�Exception Type
	private IType exType;
	
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
			collectChange(marker.getResource());
			RefactoringStatus status = new RefactoringStatus();		
			return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		Change[] changes = new Change[] {textFileChange};
		CompositeChange change = new CompositeChange("Rethrow Unhandled Exception", changes);
		return change;
	}

	@Override
	public String getName() {		
		return "Rethrow Unhandle Exception";
	}

	public void setMarker(IMarker marker){
		this.marker = marker;
		this.project = JavaCore.create(marker.getResource().getProject());
	}
	
	/**
	 * parse AST Tree�è��o�n�ק諸method node
	 * @param resource
	 */
	private void collectChange(IResource resource){
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
					currentExList = visitor.getIgnoreExList();
					rethrowException();
				}
			
			}catch (Exception ex) {
				//logger.error("[Find CS Method] EXCEPTION ",ex);
				ex.printStackTrace();
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
			//�h���startPosition,��X�n�ק諸�`�I
			
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
			//logger.error("[Rethrow Exception] EXCEPTION ",ex);
			ex.printStackTrace();
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
//			System.out.println("�iLibrary Name�j===>"+id.toString());
			if(exType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName())){
				isImportLibrary = true;
			}
		}
//		System.out.println("�iLibrary Name�j===>"+exType.getFullyQualifiedName());
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
	public void setExceptionName(String name){
		this.exceptionType = name;
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
