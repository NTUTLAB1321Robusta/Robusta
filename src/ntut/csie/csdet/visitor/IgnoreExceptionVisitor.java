package ntut.csie.csdet.visitor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

public class IgnoreExceptionVisitor extends ASTVisitor {
	CompilationUnit root;
	private List<MarkerInfo> ignoreExceptionList;
	private boolean isDetectingIgnoredExcetion;
	
	public IgnoreExceptionVisitor(CompilationUnit root) {
		super();
		this.root = root;
		ignoreExceptionList = new ArrayList<MarkerInfo>();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingIgnoredExcetion = smellSettings.isDetectingSmell(SmellSettings.SMELL_IGNORECHECKEDEXCEPTION);
	}
	
	/**
	 * 根據設定檔的資訊，決定要不要拜訪整棵樹。
	 */
	public boolean visit(MethodDeclaration node) {
		return isDetectingIgnoredExcetion;
	}
	
	public boolean visit(CatchClause node) {
		List<?> statements  = node.getBody().statements();
		if(statements.size() ==  0) {
			SingleVariableDeclaration svd = node.getException();
			MarkerInfo markerInfo = new MarkerInfo(	RLMarkerAttribute.CS_INGNORE_EXCEPTION, 
													svd.resolveBinding().getType(),
													node.toString(), node.getStartPosition(),
													root.getLineNumber(node.getStartPosition()),
													svd.getType().toString());
			ignoreExceptionList.add(markerInfo);
		}
		return true;	
	}
	
	public List<MarkerInfo> getIgnoreList() {
		return ignoreExceptionList;
	}
}
