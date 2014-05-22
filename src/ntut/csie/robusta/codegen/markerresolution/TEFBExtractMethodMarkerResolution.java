package ntut.csie.robusta.codegen.markerresolution;

import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.refactoring.ExtractMethodAnalyzer;
import ntut.csie.robusta.codegen.refactoring.TEFBExtractMethodRefactoring;
import ntut.csie.robusta.codegen.refactoringui.CodeSmellRefactoringWizard;
import ntut.csie.robusta.codegen.refactoringui.ExtractMethodInputPage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
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
		String problem;
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			
			if(problem == null)
				return;
			
			CompilationUnit root = getCompilationUnit(marker.getResource());
			ASTNode enclosingNode = findRefactoringNode(marker, root);
			
			TEFBExtractMethodRefactoring refactoring = new TEFBExtractMethodRefactoring(root, enclosingNode);
			CodeSmellRefactoringWizard csRefactoringWizard = new CodeSmellRefactoringWizard(refactoring);
			csRefactoringWizard.setUserInputPage(new ExtractMethodInputPage("It is your way!"));
			csRefactoringWizard.setDefaultPageTitle("Extract Method");
			RefactoringWizardOpenOperation operation = new RefactoringWizardOpenOperation(csRefactoringWizard);
			operation.run(new Shell(), "It's my way");
			
		} catch(Exception e) {
			logger.error(e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	/*
	 * TODO: Extract this method to utilities class later, used in a lot of places
	 */
	private CompilationUnit getCompilationUnit(IResource resource) {
		
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			try {
				IJavaElement javaElement = JavaCore.create(resource);
	
				//Create AST to parse
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);

				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				CompilationUnit root = (CompilationUnit) parser.createAST(null);
				return root;			
			} catch (Exception e) {
				logger.error("[Extract Method] EXCEPTION ", e);
			}
		}
		return null;
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
