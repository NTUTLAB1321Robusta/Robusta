package ntut.csie.robusta.codegen.markerresolution;

import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.thrown.ThrownExceptionInFinallyBlockVisitor;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.refactoring.ExtractMethodAnalyzer;
import ntut.csie.robusta.codegen.refactoring.ExtractMethodRefactoring;
import ntut.csie.robusta.codegen.refactoringui.CodeSmellRefactoringWizard;
import ntut.csie.robusta.codegen.refactoringui.ExtractMethodInputPage;
import ntut.csie.util.NodeUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMarkerResolution;
import org.jfree.chart.plot.Marker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtractMethodMarkerResolution implements IMarkerResolution {
	private static Logger logger = LoggerFactory.getLogger(ExtractMethodMarkerResolution.class);
	private String label;
	
	public ExtractMethodMarkerResolution(String label) {
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
			MarkerInfo markerInfo = findSmellMessage(root, marker);
			ASTNode node = getBadSmellNode(root, markerInfo);
			ExtractMethodAnalyzer analyzer = new ExtractMethodAnalyzer(node);
			ASTNode enclosingNode = analyzer.getEnclosingNode();
			ExtractMethodRefactoring refactoring = new ExtractMethodRefactoring(root, enclosingNode);
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
	 * TODO: Extract this method to utilities class later, used in alot of places
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
	 * Get the MarkerInfo that contains the bad smell information
	 */
	private MarkerInfo findSmellMessage(CompilationUnit actRoot, IMarker marker) {
		try {
			ASTMethodCollector methodCollector = new ASTMethodCollector();
			actRoot.accept(methodCollector);
			List<MethodDeclaration> methodList = methodCollector.getMethodList();
			String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
			ASTNode methodDeclaration = methodList.get(Integer.parseInt(methodIdx));
			
			String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
			ThrownExceptionInFinallyBlockVisitor tefbisitor = new ThrownExceptionInFinallyBlockVisitor(actRoot);
			methodDeclaration.accept(tefbisitor);
			
			List<MarkerInfo> throwsInFinallyList = tefbisitor.getThrownInFinallyList();
			MarkerInfo markerInfo = throwsInFinallyList.get(Integer.parseInt(msgIdx));
			return markerInfo;
		} catch (CoreException e) {
			logger.error("[Extract Method] EXCEPTION ", e);
		}
		return null;
	}
	
	/*
	 * Get the ASTnode from that contains bad smell
	 */
	private ASTNode getBadSmellNode(CompilationUnit astRoot, MarkerInfo markerInfo) {
		return NodeFinder.perform(astRoot, markerInfo.getPosition(), 0);
	}
	
}
