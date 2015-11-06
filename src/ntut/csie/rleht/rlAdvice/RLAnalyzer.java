package ntut.csie.rleht.rlAdvice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.RLBaseVisitor;

/**
 * apply visitor pattern to analyze the exception which may be thrown and method which has been defined robustness level
 * @author Charles
 * @version 0.0.1
 *  2011/02/10
 */
public class RLAnalyzer extends RLBaseVisitor {

	/** AST tree's root(file name)*/
	private CompilationUnit root;
	
	/** collect all method in class*/
	private ASTMethodCollector methodCollector;
	
	
	private List<RLAdviceMessage> lstRLAdviceMessage;
	
	public RLAnalyzer(CompilationUnit rootCompilationUnit){
		super(true);
		root = rootCompilationUnit;
		lstRLAdviceMessage = new ArrayList<RLAdviceMessage>();
		methodCollector = new ASTMethodCollector();
		root.accept(methodCollector);
	}

	/**
	 * check method invocation with exception signature 
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
	 * check whether the node will throw exception 
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
	 * 
	 * if this visitor visit a MethodInvocation and this MethodInvocation will throw exception, this condition will invoke addMarker().
	 * this addMarker(ASTNode node) method will put code's smell information into RLAdviceMessage.
	 * this addMarker(ASTNode node) method is different from the method in RLBuilder.class with the same name, addMarker(ASTNode node) in the RLBuilder.class 
	 * is adding marker in editor according to RLAdviceMessage.
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
	 * find out the try statement which is belonged to the input node
	 * @param node
	 * @return
	 */
	private ASTNode findTryNode(ASTNode node){
		ASTNode tryNodeCadidate = node.getParent(); 
		if(node.getParent().getNodeType() == ASTNode.TRY_STATEMENT){
			return tryNodeCadidate;
		}else if(node.getParent().getNodeType() == ASTNode.TYPE_DECLARATION){
			//avoid non stopping searching, set TYPE_DECLARATION as the break point
			return null;
		}else{
			return findTryNode(tryNodeCadidate);
		}
	}

}
