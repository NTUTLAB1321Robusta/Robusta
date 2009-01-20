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
 * Rethrow Unhandled exception的具體操作都在這個class中
 * @author chewei
 */

public class RethrowExRefactoring extends Refactoring {

	private IJavaProject project;
	
	//使用者所選擇的Exception Type
	private IType exType;
	
	private IMarker marker;
	
	private IOpenable actOpenable;
	
	// user 所填寫要丟出的Exception,預設是RunTimeException
	private String exceptionType;
	
	private TextFileChange textFileChange;
	
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	//存放目前所要fix的method node
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
	 * parse AST Tree並取得要修改的method node
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
				
				//取得該class所有的method
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<ASTNode> methodList = methodCollector.getMethodList();
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				
				//取得目前要被修改的method node
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
	 *建立Throw Exception的資訊 
	 */
	private void rethrowException(){
		try {
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
			
			//準備在Catch Caluse中加入throw exception
			//取得Code smell的資訊
			String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
			CSMessage msg = currentExList.get(Integer.parseInt(msgIdx));
			//收集該method所有的catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();
			//去比對startPosition,找出要修改的節點
			
			for (ASTNode cc : catchList){
				if(cc.getStartPosition() == msg.getPosition()){
					SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
					.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
				
					CatchClause clause = (CatchClause)cc;
					//自行建立一個throw statement加入
					ThrowStatement ts = ast.newThrowStatement();
					//將throw的variable傳入
					ClassInstanceCreation cic = ast.newClassInstanceCreation();
					//throw new RuntimeException()
					cic.setType(ast.newSimpleType(ast.newSimpleName(exceptionType)));
					//將throw new RuntimeException(ex)括號中加入參數 
					cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
					
					//取得CatchClause所有的statement
					List<Statement> statement = clause.getBody().statements();
					//將資料寫回
					ts.setExpression(cic);
					statement.add(ts);		
					//加入未import的Library(遇到RuntimeException就不用加Library)
					if(!exceptionType.equals("RuntimeException"))
						addImportDeclaration();
				}
			}
		
			//寫回Edit中
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
	 * 判斷是否有未加入的Library,但throw RuntimeException的情況要排除
	 * 因為throw RuntimeException不需import Library
	 */
	private void addImportDeclaration(){
		//判斷是否有import library
		boolean isImportLibrary = false;
		List<ImportDeclaration> importList = this.actRoot.imports();
		for(ImportDeclaration id : importList){
//			System.out.println("【Library Name】===>"+id.toString());
			if(exType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName())){
				isImportLibrary = true;
			}
		}
//		System.out.println("【Library Name】===>"+exType.getFullyQualifiedName());
		AST rootAst = this.actRoot.getAST(); 
		if(!isImportLibrary){
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(exType.getFullyQualifiedName()));
			this.actRoot.imports().add(imp);
		}
		
	}
	
	/**
	 * 紀錄user所要throw的exception type
	 * @param name : exception type
	 */
	public void setExceptionName(String name){
		this.exceptionType = name;
	}
	
	/**
	 * 取得JavaProject
	 */
	public IJavaProject getProject(){
		return project;
	}
	
	/**
	 * 儲存要Throw的Exception位置(要import使用)
	 * @param type
	 */
	public void setExType(IType type){
		this.exType = type;
	}
}
