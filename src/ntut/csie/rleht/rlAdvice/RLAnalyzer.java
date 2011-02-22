package ntut.csie.rleht.rlAdvice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;

import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

/**
 * 分析會拋出例外，以及有定義RL的外部Method，應用了visitor pattern
 * @author Charles
 * @version 0.0.1
 *  2011/02/10
 */
public class RLAnalyzer extends RLBaseVisitor {

	/** AST tree的root(檔案名稱) */
	private CompilationUnit root;
	
	/** 收集class中的method */
	private ASTMethodCollector methodCollector;
	
//	/** 儲存找到的Method List */
//	private List<ASTNode> lstMethod;
	
	private List<RLAdviceMessage> lstRLAdviceMessage;
	
	public RLAnalyzer(CompilationUnit rootCompilationUnit){
		super(true);
		root = rootCompilationUnit;
		lstRLAdviceMessage = new ArrayList<RLAdviceMessage>();
		methodCollector = new ASTMethodCollector();
		root.accept(methodCollector);
	}

	/**
	 * 檢查有拋出例外的宣告
	 */
	protected boolean visitNode(ASTNode node){
		switch(node.getNodeType()){
			case ASTNode.METHOD_INVOCATION:
				MethodInvocation mi = (MethodInvocation) node;
				addAdvice(mi);
				return false;
			default:
				return true;
		}
	}
	
	/**
	 * 檢查此node有沒有拋出例外
	 * @param node
	 */
	private void addAdvice(ASTNode node) {
		MethodInvocation mi = (MethodInvocation) node;
		if(mi.resolveMethodBinding().getExceptionTypes().length > 0){
			addMarker(mi);
		}
	}
	
	public List<RLAdviceMessage> getExceptionRLAdviceList(){
		return lstRLAdviceMessage;
	}
	
	public void throwEx() throws IOException{
		throw new IOException();
	}

	/**
	 * 呼叫這個Class的function，在檢查過MethodInvocation，且得知會拋出例外後， 會addMarker。
	 * 這裡的addMarker，負責把程式碼的資訊，紀錄到RLAdviceMessage裡面。
	 * 相較於RLBuilder.class的addMarker，則是利用RLAdviceMessage的資訊以後，重新貼一次marker
	 * @param node
	 */
	private void addMarker(ASTNode node){
		MethodInvocation mi = (MethodInvocation) node;
		ASTNode tryNode = findTryNode(node);
		TryStatement ts = null;
		if(tryNode != null){
		    ts = (TryStatement) findTryNode(node);
		    for(Object obj : ts.catchClauses()){
		    	CatchClause cc = (CatchClause)obj;
		    	ASTNode catchClause = (ASTNode)cc;
		        RLAdviceMessage rladvicemsg = new RLAdviceMessage(RLMarkerAttribute.CS_EXCEPTION_RLADVICE, 
		                null, mi.toString(), mi.getStartPosition(), catchClause.getStartPosition(),
		                root.getLineNumber(mi.getStartPosition()),
		                root.getLineNumber(catchClause.getStartPosition()), 
		                cc.getException().resolveBinding().getType().getBinaryName(), 
		                mi.resolveMethodBinding().getAnnotations());
		        lstRLAdviceMessage.add(rladvicemsg);
		    }
		}
	}
	
	/**
	 * 找出這個node所屬的TryStatement
	 * @param node
	 * @return
	 */
	private ASTNode findTryNode(ASTNode node){
		ASTNode tryNodeCadidate = node.getParent(); 
		if(node.getParent().getNodeType() == ASTNode.TRY_STATEMENT){
			return tryNodeCadidate;
		}else if(node.getParent().getNodeType() == ASTNode.TYPE_DECLARATION){
			//防止一直往上找不停，所以以TYPE_DECLARATION當作往上找的最後一層
			return null;
		}else{
			return findTryNode(tryNodeCadidate);
		}
	}

}
