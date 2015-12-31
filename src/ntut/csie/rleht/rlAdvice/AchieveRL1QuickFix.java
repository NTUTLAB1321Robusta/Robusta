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
 * when user wants to make his program achieve robustness level 1, we provide some way and suggestion to help user.
 * @author Charles
 * @version 0.0.1
 */
public class AchieveRL1QuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(AchieveRL1QuickFix.class);
	private String label;
	
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
				//get the method node which will be modified.
				findMethodNodeWillBeQuickFixed(marker.getResource(), Integer.parseInt(methodIdx));
				
				GainMarkerInfoUtil gmi = new GainMarkerInfoUtil();
				
				moveLine = gmi.findMoveLine(msgIdx, this.javaFileWillBeQuickFixed, methodNodeWillBeQuickFixed);

				/* 
				 * p.s.
				 * if there is a warning mark appearing in editor then info mark will be suppressed.
				 */		
				javaFileWillBeQuickFixed.recordModifications();
				
				QuickFixUtil qf = new QuickFixUtil();
				
				TryStatement ts = (TryStatement) gmi.findTryInMethodDeclaration(methodNodeWillBeQuickFixed, moveLine);
				if(ts != null){
					//to achieve robustness level 1 , we only help user to remove dummy handler with throwing unchecked exception.
					for(Object obj : ts.catchClauses()){
						CatchClause cc = (CatchClause)obj;
						if(((String)marker.getAttribute(RLMarkerAttribute.MI_WITH_Ex)).contains(cc.getException().getType().toString())){
							qf.addAnnotationRoot(javaFileWillBeQuickFixed, methodNodeWillBeQuickFixed, 
									RTag.LEVEL_1_ERR_REPORTING, QuickFixUtil.runtimeException);
							
							qf.addThrowStatement(cc, methodNodeWillBeQuickFixed.getAST(), 
									QuickFixUtil.runtimeException);
							
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
