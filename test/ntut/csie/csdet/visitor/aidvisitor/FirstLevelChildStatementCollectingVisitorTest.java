package ntut.csie.csdet.visitor.aidvisitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.TestEnvironmentBuilder;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.FirstLevelChildStatementExample;
import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FirstLevelChildStatementCollectingVisitorTest {

	private TestEnvironmentBuilder environmentBuilder;
	private FirstLevelChildStatementCollectingVisitor flcscVisitor;
	private CompilationUnit compilationUnit;
	
	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("FirstLevelChildStatementCollectingProject");
		environmentBuilder.createTestEnvironment();

		environmentBuilder.loadClass(FirstLevelChildStatementExample.class);

		compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanTestEnvironment();
	}

	@Test
	public void testParentWithTwoStatements() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(0).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(3, flcscVisitor.getChildren().size());
	}

	@Test
	public void testParentWithEmptyBlock() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(1).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(0, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testParentWithACommentUsingSemiColon() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(2).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(0, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testParentWithBlockCommentUsingSemiColon() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(3).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(0, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testParentWithTwoStatementsInATry() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(4).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(1, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testWithTwoStatementsInATryAndFinally() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(5).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(1, flcscVisitor.getChildren().size());
	}

	@Test
	public void testWithTwoStatementsInAndOutATry() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(6).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);
		
		assertEquals(2, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testWithThreeNestedTry() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(7).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(1, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testWithTwoTry() throws JavaModelException {
		List<MethodDeclaration> methodList = getMethodListFromCompilationUnit();
		
		Block blockOfMethod = methodList.get(8).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(3, flcscVisitor.getChildren().size());
	}

	@Test
	public void testWithNestedBlocks() throws JavaModelException {
		String tempCode = "System.out.println(\"inner try\")";
		List<MethodInvocation> miList;
		miList = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(compilationUnit, "methodWithNestedBlocks", tempCode);
		
		Block parentBlock = (Block) NodeUtils.getSpecifiedParentNode(miList.get(0), ASTNode.BLOCK);
		assertTrue(parentBlock.getParent().getNodeType() == ASTNode.TRY_STATEMENT);
		
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		parentBlock.accept(flcscVisitor);

		assertEquals(2, flcscVisitor.getChildren().size());
	}

	private List<MethodDeclaration> getMethodListFromCompilationUnit()
			throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		return methodList;
	}
	
}
