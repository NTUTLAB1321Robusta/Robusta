package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.aidvisitor.CarelessCleanupToleranceVisitor;
import ntut.csie.csdet.visitor.aidvisitor.CarelessCleanupOnlyInFinallyVisitor;
import ntut.csie.csdet.visitor.aidvisitor.CarelessClenupRaisedExceptionNotInTryCausedVisitor;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;

/**
 * Careless Cleanup的第一種檢查方式。
 * 如果關閉資源的程式碼不再finally裡面執行，則一律視為Careless Cleanup。
 * @author charles
 *
 */
public class CarelessCleanupVisitor2 extends ASTVisitor {
	/** AST的Root，用來取line number與start position用 */
	private CompilationUnit root;
	
	/** 儲存找到的例外處理壞味道程式碼所在的行數以及程式碼片段...等 */
	private List<MarkerInfo> carelessCleanupList;
	
	/** 根據設定檔，決定是否要偵測此壞味道 */
	private boolean isDetectingCarelessCleanupSmell;
	
	public CarelessCleanupVisitor2(CompilationUnit compilationUnit) {
		super();
		this.root = compilationUnit;
		carelessCleanupList = new ArrayList<MarkerInfo>();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingCarelessCleanupSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP);
	}
	
	/**
	 * 根據設定檔的資訊，決定要不要拜訪整棵樹。
	 */
	public boolean visit(MethodDeclaration node) {
		CarelessCleanupToleranceVisitor cctv = new CarelessCleanupToleranceVisitor();
		node.accept(cctv);
		return((!cctv.isTolerable()) && isDetectingCarelessCleanupSmell);
	}
	
	public List<MarkerInfo> getCarelessCleanupList() {
		return carelessCleanupList;
	}
	
	public boolean visit(TryStatement node) {
		// 不幫Nested TryStatement的程式碼結構檢查Careless Cleanup
		ASTNode outerTryStatement = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		if(outerTryStatement != null) {
			return false;
		}
		return true;
	}
	
	public boolean visit(MethodInvocation node) {
		// 在finally裡面的close動作，將不會進到這裡處理
		if(NodeUtils.isMethodInvocationInFinally(node)) {
			return false;
		}
		
		if(NodeUtils.isCloseResourceMethodInvocation(root, node)) {
			collectSmell(node);
		}
		
		return false;
	}
	
	/**
	 * 特別用來蒐集FinallyBlock裡面會造成例外的節點。
	 */
	public boolean visit(Block node) {
		TryStatement tryStatement = (TryStatement)NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		if(tryStatement != null) {
			Block finallyBlock = tryStatement.getFinally();
			// 這個Block是Finally
			if ((finallyBlock != null) && (finallyBlock.equals(node))) {			
				// 先蒐集Finally裡面的Careless Cleanup
				CarelessCleanupOnlyInFinallyVisitor ccoifv = new CarelessCleanupOnlyInFinallyVisitor(root);
				node.accept(ccoifv);
				for(MethodInvocation mi : ccoifv.getCarelessCleanupNodes()) {
					collectSmell(mi);
				}
				
				// 再來蒐集關閉串流的instance，在Try外面執行且會拋例外的情況
				CarelessClenupRaisedExceptionNotInTryCausedVisitor ccrenitcv = 
					new CarelessClenupRaisedExceptionNotInTryCausedVisitor(ccoifv.getfineCleanupNodes());
				ASTNode md = NodeUtils.getSpecifiedParentNode(node, ASTNode.METHOD_DECLARATION);
				md.accept(ccrenitcv);
				for(MethodInvocation mi : ccrenitcv.getCarelessCleanupNodes()) {
					collectSmell(mi);
				}

				// 蒐集完就不要再繼續visit下去啦~
				return false;
			}
		}
		return true;
	}
	
	/**
	 * 將這個node收入bad smell清單中。
	 * @param node
	 */
	private void collectSmell(MethodInvocation node) {
		StringBuilder exceptions = new StringBuilder();
		ITypeBinding[] exceptionTypes = NodeUtils.getMethodInvocationThrownCheckedExceptions(node);
		if (exceptionTypes != null) {
			for (ITypeBinding itb : exceptionTypes) {
				exceptions.append(itb.toString());
				exceptions.append(",");
			}
		}
		ASTNode parentNode = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_CARELESS_CLEANUP,
			(node.getExpression() != null)? node.getExpression().resolveTypeBinding() : null,
			node.toString(), node.getStartPosition(),
			root.getLineNumber(node.getStartPosition()),
			(exceptionTypes != null)? exceptions.toString() : null);
		markerInfo.setIsInTry((parentNode != null)? true:false);
		carelessCleanupList.add(markerInfo);
	}
}
