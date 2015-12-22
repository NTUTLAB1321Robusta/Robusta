package ntut.csie.analyzer.unprotected;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.marker.AnnotationInfo;
import ntut.csie.util.AbstractBadSmellVisitor;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;

public class UnprotectedMainProgramVisitor extends AbstractBadSmellVisitor {
	//store detected Unprotected main Program
	private List<MarkerInfo> unprotectedMainList;	

	private CompilationUnit root;
	private boolean isDetectingUnprotectedMainProgramSmell;
	ArrayList<AnnotationInfo> annotationList = new ArrayList<AnnotationInfo>(32);
	
	public UnprotectedMainProgramVisitor(CompilationUnit root){
		this.root = root;
		unprotectedMainList = new ArrayList<MarkerInfo>();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingUnprotectedMainProgramSmell = smellSettings
				.isDetectingSmell(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM);
	}
	
	/**
	 * according to configuration decide whether to visit and find the main function
	 */
	public boolean visit(MethodDeclaration node) {
		if(!isDetectingUnprotectedMainProgramSmell)
			return false;

		if(node == null)
			return false;
		if(node.resolveBinding() == null)
			return false;
		if (node.resolveBinding().toString().contains("void main(java.lang.String[])")) {
			List<?> statements = node.getBody().statements();
			if(containUnprotectedStatement(statements)) {
				MarkerInfo markerInfo = new MarkerInfo(
						RLMarkerAttribute.CS_UNPROTECTED_MAIN, 
						null,
						((CompilationUnit)node.getRoot()).getJavaElement().getElementName(), // class name
						node.toString(),
						node.getStartPosition(),
						getLineNumber(node), 
						null,
						annotationList);
				unprotectedMainList.add(markerInfo);				
				return false;
			}
		}
		return true;
	}
	
	/**
	 * checks whether there are statements that are not placed in try catch(Exception e) statement or try catch(Throwable t) statement in main function
	 * @param statement
	 * @return
	 */
	private boolean containUnprotectedStatement(List<?> statement) {
		int unprotectedStatementCount = 0;
		for(Object s: statement) {
			ASTNode node = (ASTNode) s;
			
			if(node.getNodeType() == ASTNode.TRY_STATEMENT) {
				if(doesCatchesAllException((TryStatement) node))
					continue;
			}
			
			AnnotationInfo ai = new AnnotationInfo(root.getLineNumber(node.getStartPosition()), 
					node.getStartPosition(), 
					node.getLength(), 
					"Not All statements In Main Enclosed In A Try Statement Catching All Possible Exceptions");
			annotationList.add(ai);
			unprotectedStatementCount++;
		}
		
		return unprotectedStatementCount != 0? true : false;
	}

	private boolean doesCatchesAllException(TryStatement tryStatement) {
		List<CatchClause> catchClauseList = tryStatement.catchClauses();
		for (CatchClause catchClause : catchClauseList) {
			if (catchClause.getException().getType().toString().equals("Exception")
					|| catchClause.getException().getType().toString().equals("Throwable")) {
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
			//if there is an annotation on method signature, according to method declaration keyword "public" get marker position line number 
			if ((!((IExtendedModifier)modifiers.get(i)).isAnnotation()) && (modifiers.get(i).toString().contains("public"))) {
				position = ((ASTNode)modifiers.get(i)).getStartPosition();
				break;
			}
		}
		//if there is no annotation on method signature, according to compilation unit get marker position line number 
		return root.getLineNumber(position);
	}

	public List<MarkerInfo> getUnprotectedMainList(){
		return unprotectedMainList;
	}

	@Override
	public List<MarkerInfo> getBadSmellCollected() {
		return getUnprotectedMainList();
	}
}
