package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.QuickFixUtils;
import ntut.csie.robusta.codegen.refactoring.ExtractMethodAnalyzer;
import ntut.csie.robusta.codegen.refactoring.TEFBExtractMethodRefactoring;
import ntut.csie.robusta.codegen.refactoringui.CodeSmellRefactoringWizard;
import ntut.csie.robusta.codegen.refactoringui.ExtractMethodInputPage;
import ntut.csie.util.PopupDialog;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TEFBExtractMethodMarkerResolution implements IMarkerResolution {
	private static Logger logger = LoggerFactory.getLogger(TEFBExtractMethodMarkerResolution.class);
	private String label;
	
	public TEFBExtractMethodMarkerResolution(String label) {
		this.label = label;
	}
	
	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		CompilationUnit root = QuickFixUtils.getCompilationUnit(marker.getResource());
		AST ast = root.getAST();
		if (ast.apiLevel() < 8) {// 8 means AST.JLS8
			PopupDialog.showDialog("Oops", "This feature only support at Eclipse Kepler");
		}else{
			String problem;
			try {
				problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
				if(problem == null)
					return;
				ASTNode enclosingNode = findRefactoringNode(marker, root);
				TEFBExtractMethodRefactoring refactoring = new TEFBExtractMethodRefactoring(root, enclosingNode);
				CodeSmellRefactoringWizard csRefactoringWizard = new CodeSmellRefactoringWizard(refactoring);
				csRefactoringWizard.setUserInputPage(new ExtractMethodInputPage("It is your way!"));
				csRefactoringWizard.setDefaultPageTitle("Extract Method");
				RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(csRefactoringWizard);
				operation.run(new Shell(), refactoring.getName());
			} catch(Exception e) {
				logger.error(e.getMessage());
				throw new RuntimeException(e);
			}
		}
	}
	
	/*
	 * Get the ASTnode from that contains bad smell
	 */
	private ASTNode getBadSmellNode(CompilationUnit astRoot, IMarker marker) {
		String strSourcePosition = marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS, null);
		int sourcePosition = Integer.parseInt(strSourcePosition);
		return NodeFinder.perform(astRoot, sourcePosition, 0);
	}
	
	/**
	 * @param marker
	 * @param root
	 * @return
	 */
	private ASTNode findRefactoringNode(IMarker marker, CompilationUnit root) {
		ASTNode beginNode = getBadSmellNode(root, marker);
		ExtractMethodAnalyzer analyzer = new ExtractMethodAnalyzer(beginNode);
		ASTNode enclosingNode = analyzer.getEncloseRefactoringNode();
		return enclosingNode;
	}
}
