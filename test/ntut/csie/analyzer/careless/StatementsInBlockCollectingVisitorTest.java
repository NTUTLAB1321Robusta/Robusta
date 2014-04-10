package ntut.csie.analyzer.careless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.careless.StatementsInBlockCollectingVisitor;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.util.NodeUtils;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class StatementsInBlockCollectingVisitorTest {

	private TestEnvironmentBuilder environmentBuilder;
	private StatementsInBlockCollectingVisitor sbcVisitor;
	private CompilationUnit compilationUnit;
	
	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("StatementsInBlockCollectingProject");
		environmentBuilder.createTestEnvironment();

		environmentBuilder.loadClass(StatementsInBlockExample.class);

		compilationUnit = environmentBuilder
				.getCompilationUnit(StatementsInBlockExample.class);
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanTestEnvironment();
	}

	@Test
	public void testParentWithTwoStatements() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(0).getBody();
		sbcVisitor = new StatementsInBlockCollectingVisitor();
		blockOfMethod.accept(sbcVisitor);

		assertEquals(2, sbcVisitor.getStatementsInBlock().size());
	}

	@Test
	public void testParentWithEmptyBlock() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(1).getBody();
		sbcVisitor = new StatementsInBlockCollectingVisitor();
		blockOfMethod.accept(sbcVisitor);

		assertEquals(0, sbcVisitor.getStatementsInBlock().size());
	}
	
	@Test
	public void testParentWithACommentUsingSemiColon() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(2).getBody();
		sbcVisitor = new StatementsInBlockCollectingVisitor();
		blockOfMethod.accept(sbcVisitor);

		assertEquals(0, sbcVisitor.getStatementsInBlock().size());
	}
	
	@Test
	public void testParentWithBlockCommentUsingSemiColon() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(3).getBody();
		sbcVisitor = new StatementsInBlockCollectingVisitor();
		blockOfMethod.accept(sbcVisitor);

		assertEquals(0, sbcVisitor.getStatementsInBlock().size());
	}
	
	@Test
	public void testParentWithTwoStatementsInATry() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(4).getBody();
		sbcVisitor = new StatementsInBlockCollectingVisitor();
		blockOfMethod.accept(sbcVisitor);

		assertEquals(1, sbcVisitor.getStatementsInBlock().size());
	}
	
	@Test
	public void testWithTwoStatementsInATryAndFinally() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(5).getBody();
		sbcVisitor = new StatementsInBlockCollectingVisitor();
		blockOfMethod.accept(sbcVisitor);

		assertEquals(1, sbcVisitor.getStatementsInBlock().size());
	}

	@Test
	public void testWithTwoStatementsInAndOutATry() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(6).getBody();
		sbcVisitor = new StatementsInBlockCollectingVisitor();
		blockOfMethod.accept(sbcVisitor);
		
		assertEquals(2, sbcVisitor.getStatementsInBlock().size());
	}
	
	@Test
	public void testWithThreeNestedTry() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(7).getBody();
		sbcVisitor = new StatementsInBlockCollectingVisitor();
		blockOfMethod.accept(sbcVisitor);

		assertEquals(1, sbcVisitor.getStatementsInBlock().size());
	}
	
	@Test
	public void testWithTwoTry() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(8).getBody();
		sbcVisitor = new StatementsInBlockCollectingVisitor();
		blockOfMethod.accept(sbcVisitor);

		assertEquals(3, sbcVisitor.getStatementsInBlock().size());
	}

	@Test
	public void testWithNestedBlocks() {
		String tempCode = "System.out.println(\"inner try\")";
		List<MethodInvocation> miList;
		miList = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(compilationUnit, "methodWithNestedBlocks", tempCode);
		assertEquals(1, miList.size());

		Block parentBlock = (Block) NodeUtils.getSpecifiedParentNode(
				miList.get(0), ASTNode.BLOCK);
		assertTrue(parentBlock.getParent().getNodeType() == ASTNode.TRY_STATEMENT);

		sbcVisitor = new StatementsInBlockCollectingVisitor();
		parentBlock.accept(sbcVisitor);

		assertEquals(2, sbcVisitor.getStatementsInBlock().size());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testWithNonBlockASTNode() {
		String tempCode = "outerMethodInvocation(methodInvocationAsArgument())";
		List<MethodInvocation> miList;
		miList = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(
				compilationUnit, "methodWithMethodInvocationAsArgument",
				tempCode);
		assertEquals(1, miList.size());

		MethodInvocation putMethodInvocation = miList.get(0);

		sbcVisitor = new StatementsInBlockCollectingVisitor();
		putMethodInvocation.accept(sbcVisitor);
	}

	private List<MethodDeclaration> getMethodListFromCompilationUnit()
			throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		return methodList;
	}
	
}
