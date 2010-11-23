package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.CarelessCleanUpAnalyzer;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ���ѵ�Careless CleanUp���Ѫk
 * @author chenyimin
 */
public class CCUQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(CCUQuickFix.class);

	private String label;

	private ASTRewrite rewrite;

	private TryStatement tryStatement;

	//���ק諸�{���X��T
	private String moveLine;
	// �ϥժ����
	//int selectLine = -1;
	
	public CCUQuickFix(String label){
		this.label = label;
	}
	@Override
	public String getLabel() {
		return label;
	}
	
	@Override
	public void run(IMarker marker) {
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem != null && (problem.equals(RLMarkerAttribute.CS_CARELESS_CLEANUP))){
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				//���o�ثe�n�Q�ק諸method node
				findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));

				//���n�Q�ק諸�{���X��T
				moveLine = findMoveLine(msgIdx);

				findTryStatement();

				//�Ytry Statement�̤w�g��Finally Block,�N�����N�Ӧ�{���X����Finally Block��
				//�_�h���إ�Finally Block��,�A����Finally Block
				if (hasFinallyBlock()) {
					moveToFinallyBlock();
				} else {
					addNewFinallyBlock();
					findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
					findTryStatement();
					moveToFinallyBlock();
				}
				//�N�n�ܧ󪺸�Ƽg�^
				applyChange(rewrite);
				findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
				findTryStatement();
				//�ϥճQ�ܧ󪺵{���X
				//selectLine();
			}
		} catch (CoreException e) {
			logger.error("[CCUQuickFix] EXCEPTION ",e);
		}
	}

	/**
	 * �����ק諸�{���X��T
	 * @return String
	 */
	private String findMoveLine(String msgIdx) {
		CarelessCleanUpAnalyzer ccVisitor = new CarelessCleanUpAnalyzer(this.actRoot); 
		currentMethodNode.accept(ccVisitor);
		//��try block���A�~�����|����quick fix
		List<CSMessage> ccList = ccVisitor.getCarelessCleanUpList(true);
		CSMessage csMsg = ccList.get(Integer.parseInt(msgIdx));
		return csMsg.getStatement();
	}

	/**
	 * �M��Try Block
	 * @return
	 */
	private boolean findTryStatement() {
		MethodDeclaration md = (MethodDeclaration) currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();
		for (int i =0; i < statement.size(); i++) {
			if (statement.get(i) instanceof TryStatement) {
				TryStatement trystat = (TryStatement) statement.get(i);
				
				boolean isFound = false;

				Block tryBlock = trystat.getBody();
				List<?> tryStatements = tryBlock.statements();
				for (int j =0; j < tryStatements.size(); j++) {
					String tryString = tryStatements.get(j).toString();
					if(tryString.contains(moveLine))
						isFound = true;
				}

				List<?> ccList = trystat.catchClauses();
				for (int j =0;j < ccList.size(); j++) {
					CatchClause cc = (CatchClause) ccList.get(j);
					Block catchBlock = cc.getBody();
					List<?> catchStatements = catchBlock.statements();
					for(int k = 0; k < catchStatements.size(); k++) {
						String catchString = catchStatements.get(k).toString();
						if (catchString.contains(moveLine))
							isFound = true;
					}
				}

				if (trystat.getFinally() != null) {
					Block finallyBlock = trystat.getFinally();
					List<?> finallyStatements = finallyBlock.statements();
					for (int j=0; j < finallyStatements.size(); j++) {
						String finallyString = finallyStatements.get(j).toString();
						if (finallyString.contains(moveLine))
							isFound = true;
					}
				}
				if (!isFound) {
					continue;
				} else {
					tryStatement = trystat;
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * �P�_Try Statement�O�_��Finally Block
	 * @return boolean
	 */
	private boolean hasFinallyBlock() {
		Block finallyBlock = tryStatement.getFinally();
		if (finallyBlock != null) {
			//���p��Finally Block�N�Хܬ�true
			return true;
		}
		return false;
	}

	/**
	 * �bTry Statement�̫إ�Finally Block
	 */
	private void addNewFinallyBlock(){
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();

		Block block = ast.newBlock();
		tryStatement.setFinally(block);
		applyChange(null);
	}
	
	/**
	 * �N���ק諸�{���X����Finally Block��
	 */
	private void moveToFinallyBlock() {
		Statement moveLineEs = null;

		rewrite = ASTRewrite.create(actRoot.getAST());
		
		ListRewrite tsRewrite = rewrite.getListRewrite(tryStatement.getBody(),Block.STATEMENTS_PROPERTY);
		List<?> tsList = tsRewrite.getOriginalList();

		//���Try Statement�̬O�_�������ʪ��{���X,�Y���h����
		for (int j=0; j<tsList.size(); j++) {
			String temp = tsList.get(j).toString();
			if (temp.contains(moveLine))
				moveLineEs = (Statement) tsList.get(j);
		}
		//���Catch Clauses�̬O�_�������ʪ��{���X,�Y���h����
		List<?> ccList = tryStatement.catchClauses();
		for (int j =0; j < ccList.size(); j++) {
			CatchClause cc = (CatchClause) ccList.get(j);
			ListRewrite ccRewrite = rewrite.getListRewrite(cc.getBody(),Block.STATEMENTS_PROPERTY);
			List<?> ccbody = ccRewrite.getOriginalList();
			for (int k =0; k < ccbody.size(); k++) {
				String ccStat = ccbody.get(k).toString();
				if (ccStat.contains(moveLine))
					moveLineEs = (Statement) ccbody.get(k);
			}
		}

		Block finallyBlock = tryStatement.getFinally();
		ASTNode placeHolder = rewrite.createMoveTarget(moveLineEs);
		ListRewrite moveRewrite = rewrite.getListRewrite(finallyBlock, Block.STATEMENTS_PROPERTY);
		moveRewrite.insertLast(placeHolder, null);
	}

//	/**
//	 * �ϥճQ�ܧ󪺵{���X
//	 * @param Document 
//	 */
//	private void selectLine() {
//		//���o�ثe��EditPart
//		IEditorPart editorPart = EditorUtils.getActiveEditor();
//		ITextEditor editor = (ITextEditor) editorPart;
//
//		Block finallyBlock = tryStatement.getFinally();
//		List<?> finallystat = finallyBlock.statements();
//		for (int j =0; j < finallystat.size(); j++) {
//			Statement stat = (Statement) finallystat.get(j);
//			String temp = stat.toString();
//			if (temp.contains(moveLine)) {
//				//�ϥոӦ�,�bQuick fix������,�i�H�N��Щw��bQuick Fix����
//				editor.selectAndReveal(stat.getStartPosition(), stat.getLength());
//				break;
//			}
//		}
//	}
}