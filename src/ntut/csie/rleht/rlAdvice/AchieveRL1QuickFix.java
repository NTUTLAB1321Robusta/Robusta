package ntut.csie.rleht.rlAdvice;

import ntut.csie.csdet.quickfix.BaseQuickFix;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.agile.exception.RTag;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 當使用者希望程式碼可以達到RL1時，我們幫他整理code，並產生一些提示
 * @author Charles
 * @version 0.0.1
 */
public class AchieveRL1QuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(AchieveRL1QuickFix.class);
	private String label;
	
	//欲修改的程式碼資訊
	private String moveLine;
	
	public AchieveRL1QuickFix(String label){
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(IMarker marker) {
		String problem;
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem != null && (problem.equals(RLMarkerAttribute.CS_EXCEPTION_RLADVICE))){
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				//取得目前要被修改的method node
				findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
				
				GainMarkerInfoUtil gmi = new GainMarkerInfoUtil();
				
				//找到要被修改的程式碼資訊
				moveLine  = gmi.findMoveLine(msgIdx, this.actRoot, currentMethodNode);

				/* 
				 * p.s. 
				 *  如果那一行有warning，就不會出現info的選項。
				 *  所以目前只考慮warning修完的情況，提供使用者code gene
				 */		
				actRoot.recordModifications();
				
				QuickFixUtil qf = new QuickFixUtil();
				
				TryStatement ts = (TryStatement) gmi.findTryInMethodDeclaration(currentMethodNode, moveLine);
				if(ts != null){
					//目前達成RL1的code gene，只幫使用者移除dummy handler，並且throw unchecked exception
					for(Object obj : ts.catchClauses()){
						CatchClause cc = (CatchClause)obj;
						if(((String)marker.getAttribute(RLMarkerAttribute.MI_WITH_Ex)).contains(cc.getException().getType().toString())){
							//加入RL annotation
							qf.addAnnotationRoot(actRoot, currentMethodNode, 
									RTag.LEVEL_1_ERR_REPORTING, QuickFixUtil.runtimeException);
							
							//加入RuntimeException
							qf.addThrowStatement(cc, currentMethodNode.getAST(), 
									QuickFixUtil.runtimeException);
							
							//刪掉printStackTrace或是System.out.println的資訊
							qf.deleteStatement(cc.getBody().statements(), QuickFixUtil.dummyHandlerStrings);
							this.applyChange();
							break;
						}
					}
				}
			}
		} catch (CoreException e) {
			logger.error("[ELAdviceQuickFix] EXCEPTION ",e);
		}
	}

}
