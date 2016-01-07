package ntut.csie.analyzer.unprotected;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.marker.AnnotationInfo;
import ntut.csie.util.AbstractBadSmellVisitor;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramVisitorData;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;

public class UnprotectedMainProgramVisitor extends AbstractBadSmellVisitor {
	// store detected Unprotected main Program
	private List<MarkerInfo> unprotectedMainList;

	private CompilationUnit root;
	private boolean isDetectingUnprotectedMainProgramSmell;
	ArrayList<AnnotationInfo> annotationList = new ArrayList<AnnotationInfo>();

	public UnprotectedMainProgramVisitor(CompilationUnit root) {
		this.root = root;
		unprotectedMainList = new ArrayList<MarkerInfo>();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingUnprotectedMainProgramSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM);
	}
	// this constructor is used to run unit test 
	public UnprotectedMainProgramVisitor() {
		unprotectedMainList = new ArrayList<MarkerInfo>();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingUnprotectedMainProgramSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM);
	}

	/**
	 * according to configuration decide whether to visit and find the main
	 * function
	 */
	public boolean visit(MethodDeclaration node) {
		if (!isDetectingUnprotectedMainProgramSmell)
			return false;

		if (node == null)
			return false;
		if (node.resolveBinding() == null)
			return false;
		if (node.resolveBinding().toString().contains("void main(java.lang.String[])")) {
			List<Statement> statements = node.getBody().statements();
			if (statements.isEmpty()) {
				return true;
			}
			UnprotectedMainProgramVisitorData properity = new UnprotectedMainProgramVisitorData();
			analizeStatementState(statements, properity);
			if(annotationList.size() == 0){
				return true;
			}
			if(annotationList.size() > 0){
				if(properity.unprotectedStatementAmount > 0){
					addMarkerInfo(node, RLMarkerAttribute.CS_UNPROTECTED_MAIN);
					return false;
				}
				if(properity.catchExceptionTryStatementAmount != properity.tryStatementAmount){
					addMarkerInfo(node, RLMarkerAttribute.CS_UNPROTECTED_MAIN);
					return false;
				}
			}
			addMarkerInfo(node, "please keep all statement in a try catch block, whick will catch \"Exception\".");
			return false;
		}
		return true;
	}

	private void analizeStatementState(List<Statement> statements, UnprotectedMainProgramVisitorData properity) {
		for (Object s : statements) {
			ASTNode node = (ASTNode) s;
			if (node.getNodeType() == ASTNode.VARIABLE_DECLARATION_STATEMENT) {
				boolean isEndWithLiteral = false;
				VariableDeclarationStatement variableDeclaration = (VariableDeclarationStatement) ((ASTNode) s);
				Object fragment = variableDeclaration.fragments().get(variableDeclaration.fragments().size() - 1);
				Expression initializer = ((VariableDeclarationFragment) fragment).getInitializer();
				if (initializer != null) {
					if (initializer.getClass().getName().endsWith("Literal")) {
						properity.variableDeclarationWithLiteralInitializer++;
						isEndWithLiteral = true;
					}
				}
				if (initializer == null) {
					properity.variableDeclarationWithLiteralInitializer++;
					isEndWithLiteral = true;
				}

				if (isEndWithLiteral)
					continue;
			}
			if (((ASTNode) s).getNodeType() == ASTNode.TRY_STATEMENT) {
				properity.tryStatementAmount++;
				if (doesCatchesAllException((TryStatement) node)) {
					properity.catchExceptionTryStatementAmount++;
					continue;
				}
			}
			AnnotationInfo ai = new AnnotationInfo(
					root.getLineNumber(node.getStartPosition()),
					node.getStartPosition(),
					node.getLength(),
					"Not All statements In Main Enclosed In A Try Statement Catching All Possible Exceptions");
			annotationList.add(ai);
		}
		properity.unprotectedStatementAmount = statements.size()
				- properity.tryStatementAmount;
	}

	private void addMarkerInfo(MethodDeclaration node, String MarkerInfoMessage) {
		MarkerInfo markerInfo = new MarkerInfo(MarkerInfoMessage, null,
				((CompilationUnit) node.getRoot()).getJavaElement()
						.getElementName(), // class name
				node.toString(), node.getStartPosition(), getLineNumber(node),
				null, annotationList);
		unprotectedMainList.add(markerInfo);
	}

	private boolean doesCatchesAllException(TryStatement tryStatement) {
		List<CatchClause> catchClauseList = tryStatement.catchClauses();
		for (CatchClause catchClause : catchClauseList) {
			if (catchClause.getException().getType().toString()
					.equals("Exception")
					|| catchClause.getException().getType().toString()
							.equals("Throwable")) {
				return true;
			}
		}

		return false;
	}

	/**
	 * according to start position get line number
	 */
	private int getLineNumber(MethodDeclaration method) {
		int position = method.getStartPosition();
		List<?> modifiers = method.modifiers();
		for (int i = 0, size = modifiers.size(); i < size; i++) {
			// if there is an annotation on method signature, according to
			// method declaration keyword "public" get marker position line
			// number
			if ((!((IExtendedModifier) modifiers.get(i)).isAnnotation())
					&& (modifiers.get(i).toString().contains("public"))) {
				position = ((ASTNode) modifiers.get(i)).getStartPosition();
				break;
			}
		}
		// if there is no annotation on method signature, according to
		// compilation unit get marker position line number
		return root.getLineNumber(position);
	}

	public List<MarkerInfo> getUnprotectedMainList() {
		return unprotectedMainList;
	}

	@Override
	public List<MarkerInfo> getBadSmellCollected() {
		return getUnprotectedMainList();
	}
}
