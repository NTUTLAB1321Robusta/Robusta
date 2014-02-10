package ntut.csie.csdet.visitor.aidvisitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.TestEnvironmentBuilder;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
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
		Method getVariableDeclaration = getMethodGetVariableDeclaration();
		Expression expression = getExpressionByMethodNameAndCode(
				"resourceAssignAndUseMultiTimes", "fis.close()");

		ASTNode variableDeclaration = (ASTNode) getVariableDeclaration.invoke(
				checker, expression);
		assertEquals("fis=null", variableDeclaration.toString());
		assertEquals(334, variableDeclaration.getStartPosition());
	}

	/**
	 * Will get SingleVariableDeclaration
	 */
	@Test
	public void testGetVariableDeclarationWithParameter() throws Exception {
		Method getVariableDeclaration = getMethodGetVariableDeclaration();
		Expression expression = getExpressionByMethodNameAndCode(
				"resourceFromParameters", "file2.canRead()");

		ASTNode variableDeclaration = (ASTNode) getVariableDeclaration.invoke(
				checker, expression);
		assertEquals("File file2", variableDeclaration.toString());
		assertEquals(469, variableDeclaration.getStartPosition());
	}

	/**
	 * Will get VariableDeclaretionFragment
	 */
	@Test
	public void testGetVariableDeclarationWithField() throws Exception {
		Method getVariableDeclaration = getMethodGetVariableDeclaration();
		Expression expression = getExpressionByMethodNameAndCode(
				"resourceFromField", "file3.canRead()");

		ASTNode variableDeclaration = (ASTNode) getVariableDeclaration.invoke(
				checker, expression);
		assertEquals("file3=null", variableDeclaration.toString());
		assertEquals(610, variableDeclaration.getStartPosition());
	}

	@Test
	public void testIsMayInterruptByExceptionWithSafeThisClose() throws Exception {
	}

	@Test
	public void testIsMayInterruptByExceptionWithCloseResourceByInvokeMyClose()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeResourceByInvokeMyClose", "this.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));

		methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeResourceByInvokeMyClose", "close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithInvokeGetResourceAndCloseItWithX()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"invokeGetResourceAndCloseItWithImp",
				"resourceManager.getResourceWithImp().close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
		
		methodInvocation = getMethodInvocationByMethodNameAndCode(
				"invokeGetResourceAndCloseItWithInterface",
				"resourceManager.getResourceWithInterface().close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
		
		methodInvocation = getMethodInvocationByMethodNameAndCode(
				"invokeGetResourceAndCloseItNotImpCloseable",
				"resourceManager.getResourceNotImpCloseable().close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	private Method getMethodGetVariableDeclaration() throws NoSuchMethodException {
		Method method = MethodInvocationMayInterruptByExceptionChecker.class
				.getDeclaredMethod("getVariableDeclaration", Expression.class);
		method.setAccessible(true);
		return method;
	}

	private MethodInvocation getMethodInvocationByMethodNameAndCode(
			String methodName, String code) {
		List<MethodInvocation> methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						methodName, code);
		assertEquals(methodInvocation.size(), 1);

		return methodInvocation.get(0);
	}

	private Expression getExpressionByMethodNameAndCode(String methodName,
			String code) {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				methodName, code);
		return methodInvocation.getExpression();
	}
}