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
 * ��ϥΪ̧Ʊ�{���X�i�H�F��RL1�ɡA�ڭ����L��zcode�A�ò��ͤ@�Ǵ���
 * @author Charles
 * @version 0.0.1
 */
public class AchieveRL1QuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(AchieveRL1QuickFix.class);
	private String label;
	
	//���ק諸�{���X��T
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
				//���o�ثe�n�Q�ק諸method node
				findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
				
				GainMarkerInfoUtil gmi = new GainMarkerInfoUtil();
				
				//���n�Q�ק諸�{���X��T
				moveLine  = gmi.findMoveLine(msgIdx, this.actRoot, currentMethodNode);

				/* 
				 * p.s. 
				 *  �p�G���@�榳warning�A�N���|�X�{info���ﶵ�C
				 *  �ҥH�ثe�u�Ҽ{warning�ק������p�A���ѨϥΪ�code gene
				 */		
				actRoot.recordModifications();
				
				QuickFixUtil qf = new QuickFixUtil();
				
				TryStatement ts = (TryStatement) gmi.findTryInMethodDeclaration(currentMethodNode, moveLine);
				if(ts != null){
					//�ثe�F��RL1��code gene�A�u���ϥΪ̲���dummy handler�A�åBthrow unchecked exception
					for(Object obj : ts.catchClauses()){
						CatchClause cc = (CatchClause)obj;
						if(((String)marker.getAttribute(RLMarkerAttribute.MI_WITH_Ex)).contains(cc.getException().getType().toString())){
							//�[�JRL annotation
							qf.addAnnotationRoot(actRoot, currentMethodNode, 
									RTag.LEVEL_1_ERR_REPORTING, QuickFixUtil.runtimeException);
							
							//�[�JRuntimeException
							qf.addThrowStatement(cc, currentMethodNode.getAST(), 
									QuickFixUtil.runtimeException);
							
							//�R��printStackTrace�άOSystem.out.println����T
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
