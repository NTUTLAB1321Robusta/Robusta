package ntut.csie.csdet.visitor.aidvisitor;

import static org.junit.Assert.*;

import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.visitor.NewCarelessCleanupVisitor;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.TestEnvironmentBuilder;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupAdvancedExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupBaseExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.FirstLevelChildStatementExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.MethodInvocationBeforeClose;
import ntut.csie.jdt.util.NodeUtils;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FirstLevelChildStatementCollectingVisitorTest {

	TestEnvironmentBuilder environmentBuilder;
	FirstLevelChildStatementCollectingVisitor flcscVisitor;
	
	
	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("FirstLevelChildStatementCollectingProject");
		environmentBuilder.createTestEnvironment();

		environmentBuilder.loadClass(FirstLevelChildStatementExample.class);
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanTestEnvironment();
	}

	@Test
	public void testParentWithTwoStatements() throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);

		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		//assertEquals(3, methodList.size());
		
		Block blockOfMethod = methodList.get(0).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(3, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testParentWithEmptyBlock() throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);

		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		//assertEquals(1, methodList.size());
		
		Block blockOfMethod = methodList.get(1).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(0, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testParentWithACommentUsingSemiColon() throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);

		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		//assertEquals(1, methodList.size());
		
		Block blockOfMethod = methodList.get(2).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(0, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testParentWithBlockComment() throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);

		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		//assertEquals(7, methodList.size());
		
		Block blockOfMethod = methodList.get(3).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(0, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testParentWithTwoStatementsInATry() throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);

		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		//assertEquals(1, methodList.size());
		
		Block blockOfMethod = methodList.get(4).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(1, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testWithTwoStatementsInATryAndFinally() throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);

		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		//assertEquals(1, methodList.size());
		
		Block blockOfMethod = methodList.get(5).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(1, flcscVisitor.getChildren().size());
	}

	@Test
	public void testWithTwoStatementsInAndOutATry() throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);

		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		//assertEquals(1, methodList.size());
		
		Block blockOfMethod = methodList.get(6).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);
		
		assertEquals(2, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testWithThreeNestedTry() throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);

		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		//assertEquals(1, methodList.size());
		
		Block blockOfMethod = methodList.get(7).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(1, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testWithTwoTry() throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);

		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		//assertEquals(1, methodList.size());
		
		Block blockOfMethod = methodList.get(8).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(6, flcscVisitor.getChildren().size());
	}
	
	@Test
	public void testToString() throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);

		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		//assertEquals(1, methodList.size());
		
		Block blockOfMethod = methodList.get(2).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals("[]", flcscVisitor.getChildren().toString());
	}

	@Test
	public void testWithNestedBlocks() throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(FirstLevelChildStatementExample.class);

		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		
		String tempCode = "System.out.println(\"inner try\")";
		List<MethodInvocation> miList;
		miList = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(compilationUnit, "methodWithNestedBlocks", tempCode);
		assertEquals("[try]", miList.get(0).getParent().getParent().toString());
		
		Block blockOfMethod = methodList.get(9).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		//assertEquals("[]", flcscVisitor.getChildren().toString());
	}
}
