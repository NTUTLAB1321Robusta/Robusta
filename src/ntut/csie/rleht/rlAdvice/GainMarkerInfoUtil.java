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
 * 在mark資料之前，有很多找Statement，蒐集message的動作
 * @author Charles
 * @version 0.0.1
 */
public class GainMarkerInfoUtil {
	private static Logger logger = LoggerFactory.getLogger(GainMarkerInfoUtil.class);
	
	/**
	 * 找出被mark那行的相關資訊
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
	 * 找到欲修改的程式碼資訊
	 * @param msgIdx
	 * @param actRoot
	 * @param currentMethodNode
	 * @return
	 */
	public String findMoveLine(String msgIdx, CompilationUnit actRoot, ASTNode currentMethodNode) {
		RLAnalyzer rlVisitor = new RLAnalyzer(actRoot); 
		currentMethodNode.accept(rlVisitor);
		//有try block的，才有可能提供quick fix
		List<RLAdviceMessage> ccList = rlVisitor.getExceptionRLAdviceList();
		RLAdviceMessage csMsg = ccList.get(Integer.parseInt(msgIdx));
		return csMsg.getStatement();
	}
	
	/**
	 * 找出被mark的那行，屬於哪個try node
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
