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

	//�ϥΪ̩ҿ�ܪ�Exception Type
	private IType exType;
	//retry���ܼƦW��
	private String retry;
	//attemp���ܼƦW��
	private String attempt;
	//�̤jretry����	
	private String maxNum;
	//�̤jretry���ƪ��ܼƦW��
	private String maxAttempt;	
	
	// user �Ҷ�g�n��X��Exception,�w�]�ORunTimeException
	private String exceptionType;
	
	private IJavaProject project;
	
	//�s��n�ഫ��ICompilationUnit������
	private IJavaElement element;
	// �ثemethod��RL Annotation��T
	private List<RLMessage> currentMethodRLList = null;
	//�s��Q�ؿ諸����
	private ITextSelection iTSelection;
	
	private TextFileChange textFileChange;
	
	private String RETRY_TYPE = "";
	
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	
	//�s��ثe�ҭnfix��method node
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
		
		//���o��class�Ҧ���method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		//�Q��ITextSelection��T�Ө��o�ϥΪ̩ҿ�ܭn�ܧ�AST Node
		//�n�ϥγo��method�ݦbxml��import org.eclipse.jdt.astview
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
			
			//���o�ثe�n�Q�ק諸method node
			if(methodIdx != -1){
				//���o�o��method��RL��T
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
		System.out.println("�iMethod Name�j====>"+md.getName());
		
//		List methodSt = md.getBody().statements();
		//���ƻs�@��method����statement�O�d�U��
//		int pos = -1;
		TryStatement original = null;
		
		ASTTryCollect visitor = new ASTTryCollect();
		md.accept(visitor);
		List<ASTNode> tryList = visitor.getTryList();
		for(int i=0; i<tryList.size() ; i++){
			if(tryList.get(i) instanceof TryStatement){					
				TryStatement temp = (TryStatement)tryList.get(i);
				if(temp.getStartPosition() == selectNode.getStartPosition()){
					System.out.println("�iFind Try Statement�j");
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
	
	/**
	 * set attempt�ܼƦW��
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
	 * set maxAttempt�ܼƦW��
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
	 * set �̤j���զ���
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
	 * set Retry�ܼƦW��
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
	 * ����user�ҭnthrow��exception type
	 * @param name : exception type
	 */
	public RefactoringStatus setExceptionName(String name){
		//���p�ϥΪ̨S����g����F��,��RefactoringStatus�]��Error
		if(name.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			//���p���g�N��L�s�U��
			this.exceptionType = name;
			return new RefactoringStatus();
		}		
	}
}
