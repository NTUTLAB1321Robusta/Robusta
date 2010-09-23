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
 * 在Marker上面的Quick Fix中，加入刪除此Statement的功能
 * @author Shiau
 */
public class OLQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(OLQuickFix.class);

	// 記錄所找到的code smell list
	private List<CSMessage> overLoggingList = null;

	// 記錄Code Smell的Type
	private String problem;
	// 記錄Quick Fix的說明
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
				//取得Marker的資訊
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);

				//若取得到Method的資訊，刪將Maker的這行刪除
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

	/** 取得OverLogging List */
	private List<CSMessage> findLoggingList() {
		if (currentMethodNode != null) {
			//尋找該method內的OverLogging
			OverLoggingDetector loggingDetector = new OverLoggingDetector(this.actRoot, currentMethodNode);
			loggingDetector.detect();
			//取得專案中OverLogging
			return loggingDetector.getOverLoggingList();
		}
		return null;
	}

	/**
	 * 刪除Message
	 * @param exception
	 */
	private void deleteMessage(int msgIdx) {
		try {
			actRoot.recordModifications();
			//取得EH smell的資訊
			CSMessage msg = overLoggingList.get(msgIdx);

			//收集該method所有的catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();

			//去比對startPosition,找出要修改的節點
			for (ASTNode cc : catchList) {
				if (cc.getStartPosition() == msg.getPosition()) {
					//刪除Logging Statement
					deleteCatchStatement(cc, msg);
					//寫回Edit中
					this.applyChange(null);
					break;
				}
			}
		} catch (Exception ex) {
			logger.error("[Delete Message] EXCEPTION ",ex);
		}
	}

	/**
	 * 刪除此Marker的Logging動作
	 * @param cc
	 * @param msg 
	 */
	private void deleteCatchStatement(ASTNode cc, CSMessage msg) {
		CatchClause clause = (CatchClause)cc;
		//取得CatchClause所有的statement,將相關print例外資訊的東西移除
		List statementList = clause.getBody().statements();

		if (statementList.size() != 0) {
			for (int i = 0; i < statementList.size(); i++) {
				if (statementList.get(i) instanceof ExpressionStatement) {
					ExpressionStatement statement = (ExpressionStatement) statementList.get(i);

					int tempLine = this.actRoot.getLineNumber(statement.getStartPosition());
					//若為選擇的行數，則刪除此行
					if (tempLine == msg.getLineNumber()) {
						statementList.remove(i);
					}
				}
			}
		}
	}
}
