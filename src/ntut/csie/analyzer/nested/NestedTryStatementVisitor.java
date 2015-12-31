package ntut.csie.analyzer.nested;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.marker.AnnotationInfo;
import ntut.csie.util.AbstractBadSmellVisitor;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

public class NestedTryStatementVisitor extends AbstractBadSmellVisitor {
	private CompilationUnit compilationUnit;
	private List<MarkerInfo> nestedTryStatementList;
	private boolean isDetectingNestedTryStatementSmell;
	
	public NestedTryStatementVisitor(CompilationUnit compilationUnit) {
		super();
		this.compilationUnit = compilationUnit;
		nestedTryStatementList = new ArrayList<MarkerInfo>();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingNestedTryStatementSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_NESTEDTRYSTATEMENT);
	}
	
	/**
	 * Get the list which stores nested try bad smell.
	 * @return
	 */
	public List<MarkerInfo> getNestedTryStatementList() {
		return nestedTryStatementList;
	}

	/**
	 * according to profile, decide whether to visit whole AST tree.
	 * @author pig
	 */
	@Override
	public boolean visit(CompilationUnit node) {
		return isDetectingNestedTryStatementSmell;
	}
	
	
	
	@Override
	public boolean visit(Initializer node) {
		return isDetectingNestedTryStatementSmell;
	}

	@Override
	public boolean visit(MethodDeclaration node) {
		return isDetectingNestedTryStatementSmell;
	}

	@Override
	public boolean visit(TryStatement node) {
		ASTNode parentTryStatement = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		if (parentTryStatement != null) {
			collectSmell(node);
		}
		return true;
	}
	
	private void collectSmell(TryStatement node) {
		ArrayList<AnnotationInfo> annotationList = new ArrayList<AnnotationInfo>(2);
		AnnotationInfo ai = new AnnotationInfo(compilationUnit.getLineNumber(node.getStartPosition()), 
				node.getStartPosition(), 
				node.getLength(), 
				"Nesting Try Statements!");
		annotationList.add(ai);
		
		MarkerInfo markerInfo = new MarkerInfo(
				RLMarkerAttribute.CS_NESTED_TRY_STATEMENT,
				null, 
				((CompilationUnit)node.getRoot()).getJavaElement().getElementName(), // class name
				node.toString(), 
				node.getStartPosition(),
				compilationUnit.getLineNumber(node.getStartPosition()),	
				null,
				annotationList);
		nestedTryStatementList.add(markerInfo);
	}

	@Override
	public List<MarkerInfo> getBadSmellCollected() {
		return getNestedTryStatementList();
	}
}
