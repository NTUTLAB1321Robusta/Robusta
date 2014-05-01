package ntut.csie.robusta.codegen.refactoring;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExtractMethodAnalyzerTest {

	private String projectName = "TestProject";
	private CompilationUnit compilationUnit;
	private ExtractMethodRefactoringTestHelper helper;

	@Before
	public void setUp() throws Exception {
		helper = new ExtractMethodRefactoringTestHelper(projectName);
		helper.InitailSettingForOnlyTEIFB();
		helper.loadClass(ThrowExceptionInFinallyBlockExampleForRefactoring.class);
		compilationUnit = helper.getCompilationUnit(ThrowExceptionInFinallyBlockExampleForRefactoring.class);
	}
		

	@After
	public void tearDown() throws Exception {
		helper.cleanUp();
	}

	//Method invocation: instance.method(); or instance.method(xxx); and so on
	@Test
	public void testGetEncloseRefactoringNode_WithMethodInvocation() throws Exception {
		List<MarkerInfo> badsmellList = helper.getTEIFBList(compilationUnit);
		//In the example, the first TEiFB bad smell is METHOD INVOCATION
		MarkerInfo markerInfo = badsmellList.get(0);
		ASTNode node = NodeFinder.perform(compilationUnit, markerInfo.getPosition(), 0);
		ExtractMethodAnalyzer analyzer = new ExtractMethodAnalyzer(node);
		ASTNode enclosingNode = analyzer.getEncloseRefactoringNode();
		assertTrue(enclosingNode.toString().equals("fileOutputStream.close()"));
	}
	
	//Super method invocation super.superMethod(); or super.superMethod(xxx); and so on
	@Test
	public void testGetEncloseRefactoringNode_WithSuperMethodInvocation() throws Exception {
		List<MarkerInfo> badsmellList = helper.getTEIFBList(compilationUnit);
		//In the example, the second TEiFB bad smell is SUPER METHOD INVOCATION
		MarkerInfo markerInfo = badsmellList.get(1);
		ASTNode node = NodeFinder.perform(compilationUnit, markerInfo.getPosition(), 0);
		ExtractMethodAnalyzer analyzer = new ExtractMethodAnalyzer(node);
		ASTNode enclosingNode = analyzer.getEncloseRefactoringNode();
		assertTrue(enclosingNode.toString().equals("super.finalize()"));
	}
	
	//Test with simple name: e.g. close(); or close(xxx); and so on
	@Test
	public void testGetEncloseRefactoringNode_WithSimpleName() throws Exception {
		List<MarkerInfo> badsmellList = helper.getTEIFBList(compilationUnit);
		//In the example, the third TEiFB bad smell is method invocation with simple name
		MarkerInfo markerInfo = badsmellList.get(2);
		ASTNode node = NodeFinder.perform(compilationUnit, markerInfo.getPosition(), 0);
		ExtractMethodAnalyzer analyzer = new ExtractMethodAnalyzer(node);
		ASTNode enclosingNode = analyzer.getEncloseRefactoringNode();
		assertTrue(enclosingNode.toString().equals("close(input)"));
	}
	
	@Test
	public void testGetDeclaredExceptions() throws Exception {
		List<MarkerInfo> badsmellList = helper.getTEIFBList(compilationUnit);
		MarkerInfo markerInfo = badsmellList.get(0);
		ASTNode node = NodeFinder.perform(compilationUnit, markerInfo.getPosition(), 0);
		ExtractMethodAnalyzer analyzer = new ExtractMethodAnalyzer(node);
		ITypeBinding[] exceptions = analyzer.getDeclaredExceptions();
		assertEquals(exceptions.length, 1);
		assertTrue(exceptions[0].getName().equals("IOException"));
	}
}
