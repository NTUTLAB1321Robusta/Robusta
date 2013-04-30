package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.csdet.visitor.CarelessCleanupVisitor;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.codegen.QuickFixUtils;
import ntut.csie.robusta.codegen.VariableDeclarationStatementFinderVisitor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveCloseResouceFromTryCatchToFinallyBlockQuickFix implements IMarkerResolution {
	private static Logger logger = LoggerFactory.getLogger(MoveCloseResouceFromTryCatchToFinallyBlockQuickFix.class);
	private QuickFixCore quickFixCore;
	private String label;

	public MoveCloseResouceFromTryCatchToFinallyBlockQuickFix(String label) {
		this.label = label;
		quickFixCore = new QuickFixCore();
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		String methodIdx = "";
		String srcPos = "";
		String isInTryStatement = "";
		String msgIdx = "";
		try {
			String[] returnIndices = collectMarkerInfo(marker);
			methodIdx = returnIndices[0];
			srcPos = returnIndices[1];
			isInTryStatement = returnIndices[2];
			msgIdx = returnIndices[3];
			if (!Boolean.parseBoolean(isInTryStatement)) {
				throw new RuntimeException(
					"The close resource instance throws exception, we don't provide quick fix. " +
					"Please use refactoring. If you can see the message, please notify system developer.");
			}
		} catch (CoreException e) {
			throw new RuntimeException("Failed to resolve marker attribute.", e);
		}
		
		quickFixCore.setJavaFileModifiable(marker.getResource());
		CompilationUnit compilationUnit = quickFixCore.getCompilationUnit(); 
		MethodDeclaration methodDeclaration = QuickFixUtils.getMethodDeclaration(compilationUnit, Integer.parseInt(methodIdx));
		if(methodDeclaration == null) {
			return;
		}
		// 要移動的那行程式碼的長度(method invocation與expression statement只差一個分號，所以length只差1)
		int movelineLength = getMovelineLength(msgIdx, compilationUnit, methodDeclaration);
		ASTNode closeResourceNode = NodeFinder.perform(compilationUnit, Integer.parseInt(srcPos), movelineLength);
		TryStatement closeResourceNodeParent = (TryStatement)NodeUtils.getSpecifiedParentNode(closeResourceNode, ASTNode.TRY_STATEMENT);
		if(closeResourceNodeParent != null) {
			// move node from try/catch to finally
			
			//1. 找到並調整變數宣告的地方
			VariableDeclarationStatementFinderVisitor vdsf = 
				new VariableDeclarationStatementFinderVisitor((MethodInvocation) closeResourceNode);
			compilationUnit.accept(vdsf);
			VariableDeclarationStatement variableDeclarationOfCloseResource = vdsf.getFoundVariableDeclarationStatement();
			ASTNode variableDeclarationOfCloseResourceOfTry = NodeUtils.getSpecifiedParentNode(variableDeclarationOfCloseResource, ASTNode.TRY_STATEMENT);

			// 如果宣告的程式碼與關閉資源的程式碼都在同一個TryStatement裡面，就要調整宣告位置
			if((variableDeclarationOfCloseResourceOfTry != null) && 
			   (variableDeclarationOfCloseResourceOfTry.equals(closeResourceNodeParent))) {
				quickFixCore.moveOutVariableDeclarationStatementFromTry(closeResourceNodeParent, variableDeclarationOfCloseResource, compilationUnit.getAST(), methodDeclaration);
			}
			
			//2. 檢查是否要增加finally
			Block finallyBlock = quickFixCore.getFinallyBlock(closeResourceNodeParent, compilationUnit);

			//3. 移動close到finally
				// (method invocation與expression statement只差一個分號，所以length只差1)
			quickFixCore.moveNodeToFinally(closeResourceNodeParent, NodeFinder.perform(compilationUnit, Integer.parseInt(srcPos), movelineLength + 1), finallyBlock);
		} else {
			// node not in try statement
		}
		quickFixCore.applyChange();
	}
	
	private String[] collectMarkerInfo(IMarker marker) throws CoreException {
		String methodIdx = "";
		String srcPos = "";
		String isInTry = "";
		String msgIdx = "";
		String[] returnIndice = new String[4]; 
		String problem = "";
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			
			if((problem == null) || (!problem.equals(RLMarkerAttribute.CS_CARELESS_CLEANUP))) {
				return returnIndice;
			}
			methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
			srcPos = (String) marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString();
			isInTry = marker.getAttribute(RLMarkerAttribute.CCU_WITH_TRY).toString();
			msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
			returnIndice[0] = methodIdx;
			returnIndice[1] = srcPos;
			returnIndice[2] = isInTry;
			returnIndice[3] = msgIdx;
			return returnIndice;	
		} catch (CoreException e) {
			logger.error(e.getMessage());
			throw e;
		}
	}
	
	/**
	 * 找出壞味道程式碼的字元數
	 * @param msgIdx 在這個MethodDeclaration裡面，他是第幾個bad smell
	 * @param compilationUnit
	 * @param methodDeclaration 壞味道程式碼所在的MethodDeclaration
	 * @return
	 */
	private int getMovelineLength(String msgIdx, CompilationUnit compilationUnit, MethodDeclaration methodDeclaration) {
		CarelessCleanupVisitor ccVisitor = new CarelessCleanupVisitor(compilationUnit);
		methodDeclaration.accept(ccVisitor);
		return ccVisitor.getCarelessCleanupList().get(Integer.parseInt(msgIdx)).getStatement().length();
	}
}
