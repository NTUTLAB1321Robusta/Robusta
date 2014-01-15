package ntut.csie.csdet.visitor.aidvisitor;

import static org.junit.Assert.*;

import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.visitor.NewCarelessCleanupVisitor;
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
		assertEquals(1, methodList.size());
		
		Block blockOfMethod = methodList.get(0).getBody();
		flcscVisitor = new FirstLevelChildStatementCollectingVisitor();
		blockOfMethod.accept(flcscVisitor);

		assertEquals(2, flcscVisitor.getChildrens().size());
	}

	@Test
	public void testGetChildrens2() {
		fail("Not yet implemented");
	}

}
