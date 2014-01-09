package ntut.csie.csdet.visitor.aidvisitor;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.TestEnvironmentBuilder;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MethodInvocationMayInterruptByExceptionCheckerTest {

	TestEnvironmentBuilder environmentBuilder;
	CompilationUnit compilationUnit;
	MethodInvocationMayInterruptByExceptionChecker checker;

	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder();
		environmentBuilder.createTestEnvironment();

		environmentBuilder
				.loadClass(MethodInvocationMayInterruptByExceptionCheckerExample.class);

		compilationUnit = environmentBuilder
				.getCompilationUnit(MethodInvocationMayInterruptByExceptionCheckerExample.class);

		checker = new MethodInvocationMayInterruptByExceptionChecker(
				compilationUnit);
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanTestEnvironment();
	}

	/**
	 * Will get VariableDeclaretionFragment
	 */
	@Test
	public void testGetVariableDeclarationWithLocalVariable() throws Exception {
		Method getVariableDeclaration = MethodInvocationMayInterruptByExceptionChecker.class
				.getDeclaredMethod("getVariableDeclaration",
						MethodInvocation.class);
		getVariableDeclaration.setAccessible(true);

		List<MethodInvocation> methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						"resourceAssignAndUseMultiTimes", "fis.close()");

		assertEquals(methodInvocation.size(), 1);
		ASTNode variableDeclaration = (ASTNode) getVariableDeclaration.invoke(
				checker, methodInvocation.get(0));
		assertEquals("fis=null", variableDeclaration.toString());
		assertEquals(334, variableDeclaration.getStartPosition());
	}

	/**
	 * Will get SingleVariableDeclaration
	 */
	@Test
	public void testGetVariableDeclarationWithParameter() throws Exception {
		Method getVariableDeclaration = MethodInvocationMayInterruptByExceptionChecker.class
				.getDeclaredMethod("getVariableDeclaration",
						MethodInvocation.class);
		getVariableDeclaration.setAccessible(true);

		List<MethodInvocation> methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						"resourceFromParameters", "file2.canRead()");

		assertEquals(methodInvocation.size(), 1);
		ASTNode variableDeclaration = (ASTNode) getVariableDeclaration.invoke(
				checker, methodInvocation.get(0));
		assertEquals("File file", variableDeclaration.toString());
		assertEquals(462, variableDeclaration.getStartPosition());
	}

	/**
	 * Will get VariableDeclaretionFragment
	 */
	@Test
	public void testGetVariableDeclarationWithField() throws Exception {
		Method getVariableDeclaration = MethodInvocationMayInterruptByExceptionChecker.class
				.getDeclaredMethod("getVariableDeclaration",
						MethodInvocation.class);
		getVariableDeclaration.setAccessible(true);
		
		List<MethodInvocation> methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						"resourceFromField", "file3.canRead()");
		
		assertEquals(methodInvocation.size(), 1);
		ASTNode variableDeclaration = (ASTNode) getVariableDeclaration.invoke(
				checker, methodInvocation.get(0));
		assertEquals("file3=null", variableDeclaration.toString());
		assertEquals(596, variableDeclaration.getStartPosition());
	}

}
