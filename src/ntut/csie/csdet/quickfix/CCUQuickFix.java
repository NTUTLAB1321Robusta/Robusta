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
 * 提供給Careless CleanUp的解法
 * @author chenyimin
 */
public class CCUQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(CCUQuickFix.class);

	private String label;

	private ASTRewrite rewrite;

	private TryStatement tryStatement;

	//欲修改的程式碼資訊
	private String moveLine;
	// 反白的行數
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
				//取得目前要被修改的method node
				findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));

				//找到要被修改的程式碼資訊
				moveLine = findMoveLine(msgIdx);

				findTryStatement();

				//若try Statement裡已經有Finally Block,就直接將該行程式碼移到Finally Block中
				//否則先建立Finally Block後,再移到Finally Block
				if (hasFinallyBlock()) {
					moveToFinallyBlock();
				} else {
					addNewFinallyBlock();
					findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
					findTryStatement();
					moveToFinallyBlock();
				}
				//將要變更的資料寫回
				applyChange(rewrite);
				findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
				findTryStatement();
				//反白被變更的程式碼
				//selectLine();
			}
		} catch (CoreException e) {
			logger.error("[CCUQuickFix] EXCEPTION ",e);
		}
	}

	/**
	 * 找到欲修改的程式碼資訊
	 * @return String
	 */
	private String findMoveLine(String msgIdx) {
		CarelessCleanUpAnalyzer ccVisitor = new CarelessCleanUpAnalyzer(this.actRoot); 
		currentMethodNode.accept(ccVisitor);
		//有try block的，才有機會提供quick fix
		List<CSMessage> ccList = ccVisitor.getCarelessCleanUpList(true);
		CSMessage csMsg = ccList.get(Integer.parseInt(msgIdx));
		return csMsg.getStatement();
	}

	/**
	 * 尋找Try Block
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
	 * 判斷Try Statement是否有Finally Block
	 * @return boolean
	 */
	private boolean hasFinallyBlock() {
		Block finallyBlock = tryStatement.getFinally();
		if (finallyBlock != null) {
			//假如有Finally Block就標示為true
			return true;
		}
		return false;
	}

	/**
	 * 在Try Statement裡建立Finally Block
	 */
	private void addNewFinallyBlock(){
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();

		Block block = ast.newBlock();
		tryStatement.setFinally(block);
		applyChange(null);
	}
	
	/**
	 * 將欲修改的程式碼移到Finally Block中
	 */
	private void moveToFinallyBlock() {
		Statement moveLineEs = null;

		rewrite = ASTRewrite.create(actRoot.getAST());
		
		ListRewrite tsRewrite = rewrite.getListRewrite(tryStatement.getBody(),Block.STATEMENTS_PROPERTY);
		List<?> tsList = tsRewrite.getOriginalList();

		//比對Try Statement裡是否有欲移動的程式碼,若有則移除
		for (int j=0; j<tsList.size(); j++) {
			String temp = tsList.get(j).toString();
			if (temp.contains(moveLine))
				moveLineEs = (Statement) tsList.get(j);
		}
		//比對Catch Clauses裡是否有欲移動的程式碼,若有則移除
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
//	 * 反白被變更的程式碼
//	 * @param Document 
//	 */
//	private void selectLine() {
//		//取得目前的EditPart
//		IEditorPart editorPart = EditorUtils.getActiveEditor();
//		ITextEditor editor = (ITextEditor) editorPart;
//
//		Block finallyBlock = tryStatement.getFinally();
//		List<?> finallystat = finallyBlock.statements();
//		for (int j =0; j < finallystat.size(); j++) {
//			Statement stat = (Statement) finallystat.get(j);
//			String temp = stat.toString();
//			if (temp.contains(moveLine)) {
//				//反白該行,在Quick fix完之後,可以將游標定位在Quick Fix那行
//				editor.selectAndReveal(stat.getStartPosition(), stat.getLength());
//				break;
//			}
//		}
//	}
}