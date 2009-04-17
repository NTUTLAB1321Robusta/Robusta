package ntut.csie.csdet.refactor;

import java.util.List;

import ntut.csie.csdet.visitor.ASTTryCollect;
import ntut.csie.csdet.visitor.SpareHandlerAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;

public class NoAltRetryRefactoring extends Refactoring {

	//使用者所選擇的Exception Type
	private IType exType;
	//retry的變數名稱
	private String retry;
	//attemp的變數名稱
	private String attempt;
	//最大retry次數	
	private String maxNum;
	//最大retry次數的變數名稱
	private String maxAttempt;	
	
	// user 所填寫要丟出的Exception,預設是RunTimeException
	private String exceptionType;
	
	private IJavaProject project;
	
	//存放要轉換成ICompilationUnit的物件
	private IJavaElement element;
	// 目前method的RL Annotation資訊
	private List<RLMessage> currentMethodRLList = null;
	//存放被框選的物件
	private ITextSelection iTSelection;
	
	private TextFileChange textFileChange;
	
	private String RETRY_TYPE = "";
	
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	
	public NoAltRetryRefactoring(IJavaProject project,IJavaElement element,ITextSelection sele,String retryType){
		this.project = project;
		this.element = element;
		this.iTSelection = sele;
		this.RETRY_TYPE = retryType;
	}
	
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return null;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();		
		if(iTSelection.getOffset() < 0 || iTSelection.getLength() == 0){
			status.addFatalError("Selection Error, please retry again!!!");
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		Change[] changes = new Change[] {textFileChange};
		CompositeChange change = new CompositeChange("No Alternative Retry", changes);
		return change;
	}

	@Override
	public String getName() {
		return "No Alternative Retry ";
	}
		
	private void collectChange(RefactoringStatus status){
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
				
		parser.setSource((ICompilationUnit) element);
		parser.setResolveBindings(true);
		this.actRoot = (CompilationUnit) parser.createAST(null);
		
		//取得該class所有的method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		//利用ITextSelection資訊來取得使用者所選擇要變更的AST Node
		//要使用這個method需在xml檔import org.eclipse.jdt.astview
		ASTNode selectNode = NodeFinder.perform(actRoot, iTSelection.getOffset(), iTSelection.getLength());
		if(selectNode == null){
			status.addFatalError("Selection Error, please retry again!!!");
		}else if(!(selectNode instanceof TryStatement)){
			status.addFatalError("Selection Error, please retry again!!!");
		}else{			
			List<ASTNode> methodList = methodCollector.getMethodList();
			int methodIdx = -1;
			SpareHandlerAnalyzer visitor = null;
			for(ASTNode method : methodList){
				methodIdx++;
				visitor = new SpareHandlerAnalyzer(selectNode,actRoot);
				method.accept(visitor);
				if(visitor.getResult()){				
					break;
				}
			}
			if(!visitor.getResult()){
				status.addFatalError("Selection Error, please retry again!!!");
			}
			
			//取得目前要被修改的method node
			if(methodIdx != -1){
				//取得這個method的RL資訊
				currentMethodNode = methodList.get(methodIdx);
				ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
				currentMethodNode.accept(exVisitor);
				currentMethodRLList = exVisitor.getMethodRLAnnotationList();
				introduceNoAltRetry(selectNode);
			}else{
				status.addFatalError("Selection Error, please retry again!!!");
			}
		}
	}
	
	private void introduceNoAltRetry(ASTNode selectNode){
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		System.out.println("【Method Name】====>"+md.getName());
		
//		List methodSt = md.getBody().statements();
		//先複製一份method內的statement保留下來
//		int pos = -1;
		TryStatement original = null;
		
		ASTTryCollect visitor = new ASTTryCollect();
		md.accept(visitor);
		List<ASTNode> tryList = visitor.getTryList();
		for(int i=0; i<tryList.size() ; i++){
			if(tryList.get(i) instanceof TryStatement){					
				TryStatement temp = (TryStatement)tryList.get(i);
				if(temp.getStartPosition() == selectNode.getStartPosition()){
					System.out.println("【Find Try Statement】");
					original = (TryStatement)tryList.get(i);
					break;
				}
			}
		}
		
		ASTNode parent = original.getParent();
		if(parent != null){
			
		}
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
	
	/**
	 * set attempt變數名稱
	 * @param attempt
	 */
	public RefactoringStatus setAttemptVariable(String attempt){		
		if(attempt.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.attempt = attempt;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * set maxAttempt變數名稱
	 * @param attempt
	 */
	public RefactoringStatus setMaxAttemptVariable(String attempt){
		if(attempt.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.maxAttempt = attempt;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * set 最大重試次數
	 * @param num
	 */
	public RefactoringStatus setMaxAttemptNum(String num){
		if(num.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.maxNum = num;
			return new RefactoringStatus();
		}		
	}
	
	/**
	 * set Retry變數名稱
	 * @param attempt
	 */
	public RefactoringStatus setRetryVariable(String retry){
		if(retry.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.retry = retry;
			return new RefactoringStatus();
		}	
	}
	
	/**
	 * 紀錄user所要throw的exception type
	 * @param name : exception type
	 */
	public RefactoringStatus setExceptionName(String name){
		//假如使用者沒有填寫任何東西,把RefactoringStatus設成Error
		if(name.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			//假如有寫就把他存下來
			this.exceptionType = name;
			return new RefactoringStatus();
		}		
	}
}
