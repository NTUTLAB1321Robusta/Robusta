package ntut.csie.rleht.rlAdvice;

import java.util.List;

import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * �bmark��Ƥ��e�A���ܦh��Statement�A�`��message���ʧ@
 * @author Charles
 * @version 0.0.1
 */
public class GainMarkerInfoUtil {
	private static Logger logger = LoggerFactory.getLogger(GainMarkerInfoUtil.class);
	
	/**
	 * ��X�Qmark���檺������T
	 * @param marker
	 * @param actRoot
	 * @param currentMethodNode
	 * @return
	 */
	public RLAdviceMessage findSmellMessage(IMarker marker, CompilationUnit actRoot, ASTNode currentMethodNode){
		RLAdviceMessage rlAdviceMessage = null;
		try {
			String msgIdx;
			msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
			RLAnalyzer rlvisitor = new RLAnalyzer(actRoot);
			currentMethodNode.accept(rlvisitor);
			rlAdviceMessage = rlvisitor.getExceptionRLAdviceList().get(Integer.parseInt(msgIdx));
		} catch (CoreException e) {
			logger.error("[Find RLException] EXCEPTION ", e);
		}
		return rlAdviceMessage;
	}
	
	/**
	 * �����ק諸�{���X��T
	 * @param msgIdx
	 * @param actRoot
	 * @param currentMethodNode
	 * @return
	 */
	public String findMoveLine(String msgIdx, CompilationUnit actRoot, ASTNode currentMethodNode) {
		RLAnalyzer rlVisitor = new RLAnalyzer(actRoot); 
		currentMethodNode.accept(rlVisitor);
		//��try block���A�~���i�ണ��quick fix
		List<RLAdviceMessage> ccList = rlVisitor.getExceptionRLAdviceList();
		RLAdviceMessage csMsg = ccList.get(Integer.parseInt(msgIdx));
		return csMsg.getStatement();
	}
	
	/**
	 * ��X�Qmark������A�ݩ����try node
	 * @param node
	 * @param moveLine
	 * @return
	 */
	public ASTNode findTryInMethodDeclaration(ASTNode node, String moveLine){
		MethodDeclaration md = (MethodDeclaration) node;
		List<?> lstStatements = md.getBody().statements();
		for(Object st: lstStatements){
			if(st instanceof TryStatement){
				TryStatement ts = (TryStatement) st;
				List<?> lstStatementsInTry = ts.getBody().statements();
				for(Object stInTry: lstStatementsInTry){
					if(stInTry instanceof ExpressionStatement){
						ExpressionStatement es = (ExpressionStatement) stInTry;
						if(es.toString().contains(moveLine+";")){
							return ts;
						}
					}
				}
			}
		}
		return null;
	}
}
