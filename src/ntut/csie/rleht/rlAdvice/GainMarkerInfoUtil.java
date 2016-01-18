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
 * before adding marker, there are some preprocess needs to be done such as searching statement and collecting smell message 
 * @author Charles
 * @version 0.0.1
 */
public class GainMarkerInfoUtil {
	private static Logger logger = LoggerFactory.getLogger(GainMarkerInfoUtil.class);
	
//	/**
//	 * get smell message of the marked line
//	 * @param marker
//	 * @param actRoot
//	 * @param currentMethodNode
//	 * @return
//	 */
	
	/**
	 * get information of a code which is needed to be modified
	 * @param msgIdx
	 * @param actRoot
	 * @param currentMethodNode
	 * @return
	 */
	public String findMoveLine(String msgIdx, CompilationUnit actRoot, ASTNode currentMethodNode) {
		RLAnalyzer rlVisitor = new RLAnalyzer(actRoot); 
		currentMethodNode.accept(rlVisitor);
		//only provide quick fix to the statement with try block
		List<RLAdviceMessage> ccList = rlVisitor.getExceptionRLAdviceList();
		RLAdviceMessage csMsg = ccList.get(Integer.parseInt(msgIdx));
		return csMsg.getStatement();
	}
	
	/**
	 * find out which marked line belongs to which try block 
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
