package ntut.csie.analyzer.careless;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.testutility.TestEnvironmentBuilder;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ClosingResourceBeginningPositionFinderTest {

	TestEnvironmentBuilder environmentBuilder;
	CompilationUnit compilationUnit;
	ClosingResourceBeginningPositionFinder finder;

	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder();
		environmentBuilder.createTestEnvironment();

		environmentBuilder
				.loadClass(ClosingResourceBeginningPositionFinderExample.class);

		compilationUnit = environmentBuilder
				.getCompilationUnit(ClosingResourceBeginningPositionFinderExample.class);

		finder = new ClosingResourceBeginningPositionFinder();
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanTestEnvironment();
	}

	@Test
	public void testfindPositionWithLocalVariable() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceAssignAndUseMultiTimes", "fis.close()");

		int position = finder.findPosition(methodInvocation);
		assertEquals(10, compilationUnit.getLineNumber(position));
	}

	@Test
	public void testfindPositionWithParameter() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceFromParameters", "file2.canRead()");

		int position = finder.findPosition(methodInvocation);
		assertEquals(16, compilationUnit.getLineNumber(position));
	}

	@Test
	public void testfindPositionWithField() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceFromField", "file3.canRead()");

		int position = finder.findPosition(methodInvocation);
		assertEquals(24, compilationUnit.getLineNumber(position));
	}
	
	@Test
	public void testfindPositionWithGetResource() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"invokeGetResourceAndCloseIt", "resourceManager.getResource().close()");
		
		int position = finder.findPosition(methodInvocation);
		assertEquals(43, compilationUnit.getLineNumber(position));
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
				finder, expression);
		assertEquals("fis=null", variableDeclaration.toString());
		assertEquals(10, getLineNumber(variableDeclaration));
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
				finder, expression);
		assertEquals("File file2", variableDeclaration.toString());
		assertEquals(16, getLineNumber(variableDeclaration));
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
				finder, expression);
		assertEquals("file3=null", variableDeclaration.toString());
		assertEquals(22, getLineNumber(variableDeclaration));
	}

	/**
	 * The structure doesn't expect will cause RuntimeException
	 */
	@Test(expected = RuntimeException.class)
	public void testSetStartPositionByTheExpressionOfMethodInvocationWithWrongFormat() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeResourceByInvokeMyClose", "this.close()");

		Method method = getMethodSetStartPositionByTheExpressionOfMethodInvocation();
		// Because reflaction will pack RuntimeException to InvocationTargetException
		try {
			method.invoke(finder, methodInvocation);
		} catch (InvocationTargetException e) {
			throw (RuntimeException) e.getTargetException();
		}
	}

	@Test
	public void testGetStartPositionOfMethodDeclaration() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceAssignAndUseMultiTimes", "fis.available()");
		
		Method method = getMethodGetStartPositionOfMethodDeclaration();
		int position = (Integer) method.invoke(finder, methodInvocation);
		assertEquals(9, compilationUnit.getLineNumber(position));
	}

	private Method getMethodGetStartPositionOfMethodDeclaration() throws NoSuchMethodException {
		Method method = ClosingResourceBeginningPositionFinder.class
				.getDeclaredMethod("getStartPositionOfMethodDeclaration", MethodInvocation.class);
		method.setAccessible(true);
		return method;
	}

	private Method getMethodGetVariableDeclaration() throws NoSuchMethodException {
		Method method = ClosingResourceBeginningPositionFinder.class
				.getDeclaredMethod("getVariableDeclaration", Expression.class);
		method.setAccessible(true);
		return method;
	}

	private Method getMethodSetStartPositionByTheExpressionOfMethodInvocation()
			throws NoSuchMethodException {
		Method method = ClosingResourceBeginningPositionFinder.class
				.getDeclaredMethod(
						"setStartPositionByTheExpressionOfMethodInvocation",
						MethodInvocation.class);
		method.setAccessible(true);
		return method;
	}

	private MethodInvocation getMethodInvocationByMethodNameAndCode(
			String methodName, String code) {
		List<MethodInvocation> methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						methodName, code);
		assertEquals(1, methodInvocation.size());

		return methodInvocation.get(0);
	}
	
	private Expression getExpressionByMethodNameAndCode(String methodName,
			String code) {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				methodName, code);
		return methodInvocation.getExpression();
	}

	/**
	 * @exception IllegalArgumentException
	 *                If node is not a subnode of this compilationUnit
	 */
	private int getLineNumber(ASTNode node) {
		if (!node.getRoot().equals(compilationUnit)) {
			throw new IllegalArgumentException("Not in this compilationUnit");
		}
		return compilationUnit.getLineNumber(node.getStartPosition());
	}
}
