package ntut.csie.analyzer.empty;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class EmptyCatchBlockVisitor extends ASTVisitor {
	CompilationUnit root;
	private List<MarkerInfo> emptyCatchBlockList;
	private boolean isDetectingEmptyCatchBlock;
	
	public EmptyCatchBlockVisitor(CompilationUnit root) {
		super();
		this.root = root;
		emptyCatchBlockList = new ArrayList<MarkerInfo>();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingEmptyCatchBlock = smellSettings.isDetectingSmell(SmellSettings.SMELL_EMPTYCATCHBLOCK);
	}
	
	/**
	 * 根據設定檔的資訊，決定要不要拜訪整棵樹。
	 */
	public boolean visit(MethodDeclaration node) {
		return isDetectingEmptyCatchBlock;
	}
	
	/**
	 * Shouldn't visit Initializer when the user don't want to detect this kind of bad smells
	 */
	public boolean visit(Initializer node) {
		return isDetectingEmptyCatchBlock;
	}

	public boolean visit(CatchClause node) {
		if (node.getBody().statements().size() == 0) {
			addSmellInfo(node);
		}
		return true;
	}

	private void addSmellInfo(CatchClause node) {
		SingleVariableDeclaration svd = node.getException();
		MarkerInfo markerInfo = new MarkerInfo(
				RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK,
				svd.resolveBinding().getType(), node.toString(),
				node.getStartPosition(),
				root.getLineNumber(node.getStartPosition()),
				svd.getType().toString());
		emptyCatchBlockList.add(markerInfo);
	}

	public List<MarkerInfo> getEmptyCatchList() {
		return emptyCatchBlockList;
	}
	
}
