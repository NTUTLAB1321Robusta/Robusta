package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.OverLoggingDetector;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * �bMarker�W����Quick Fix���A�[�J�R����Statement���\��
 * @author Shiau
 */
public class OLQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(OLQuickFix.class);

	// �O���ҧ�쪺code smell list
	private List<CSMessage> overLoggingList = null;

	// �O��Code Smell��Type
	private String problem;
	// �O��Quick Fix������
	private String label;

	public OLQuickFix(String label) {
		this.label = label;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem != null && (problem.equals(RLMarkerAttribute.CS_OVER_LOGGING))) {				
				//���oMarker����T
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);

				//�Y���o��Method����T�A�R�NMaker���o��R��
				boolean isok = this.findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok) {
					overLoggingList = findLoggingList();

					deleteMessage(Integer.parseInt(msgIdx));
				}
			}
		} catch (CoreException e) {
			logger.error("[OLQuickFix] EXCEPTION ",e);
		}
	}

	/** ���oOverLogging List */
	private List<CSMessage> findLoggingList() {
		if (currentMethodNode != null) {
			//�M���method����OverLogging
			OverLoggingDetector loggingDetector = new OverLoggingDetector(this.actRoot, currentMethodNode);
			loggingDetector.detect();
			//���o�M�פ�OverLogging
			return loggingDetector.getOverLoggingList();
		}
		return null;
	}

	/**
	 * �R��Message
	 * @param exception
	 */
	private void deleteMessage(int msgIdx) {
		try {
			actRoot.recordModifications();
			//���oEH smell����T
			CSMessage msg = overLoggingList.get(msgIdx);

			//������method�Ҧ���catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();

			//�h���startPosition,��X�n�ק諸�`�I
			for (ASTNode cc : catchList) {
				if (cc.getStartPosition() == msg.getPosition()) {
					//�R��Logging Statement
					deleteCatchStatement(cc, msg);
					//�g�^Edit��
					this.applyChange(null);
					break;
				}
			}
		} catch (Exception ex) {
			logger.error("[Delete Message] EXCEPTION ",ex);
		}
	}

	/**
	 * �R����Marker��Logging�ʧ@
	 * @param cc
	 * @param msg 
	 */
	private void deleteCatchStatement(ASTNode cc, CSMessage msg) {
		CatchClause clause = (CatchClause)cc;
		//���oCatchClause�Ҧ���statement,�N����print�ҥ~��T���F�貾��
		List statementList = clause.getBody().statements();

		if (statementList.size() != 0) {
			for (int i = 0; i < statementList.size(); i++) {
				if (statementList.get(i) instanceof ExpressionStatement) {
					ExpressionStatement statement = (ExpressionStatement) statementList.get(i);

					int tempLine = this.actRoot.getLineNumber(statement.getStartPosition());
					//�Y����ܪ���ơA�h�R������
					if (tempLine == msg.getLineNumber()) {
						statementList.remove(i);
					}
				}
			}
		}
	}
}
