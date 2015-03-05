package ntut.csie.analyzer.careless;

import static org.junit.Assert.fail;
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
		environmentBuilder.createEnvironment();

		environmentBuilder
				.loadClass(ClosingResourceBeginningPositionFinderExample.class);

		compilationUnit = environmentBuilder
				.getCompilationUnit(ClosingResourceBeginningPositionFinderExample.class);

		finder = new ClosingResourceBeginningPositionFinder();
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	@Test
	public void testfindPositionWithLocalVariable() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceAssignAndUseMultiTimes", "fis.close()");

		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(12, compilationUnit.getLineNumber(detectionStartPosition));
	}

	@Test
	public void testfindPositionWithParameter() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceFromParameters", "file2.canRead()");

		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(17, compilationUnit.getLineNumber(detectionStartPosition));
	}

	@Test
	public void testfindPositionWithField() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceFromField", "file3.canRead()");

		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(25, compilationUnit.getLineNumber(detectionStartPosition));
	}
	
	@Test
	public void testfindPositionWithGetResource() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"invokeGetResourceAndCloseIt", "resourceManager.getResource().close()");
		
		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(44, compilationUnit.getLineNumber(detectionStartPosition));
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
		assertEquals(11, getLineNumber(variableDeclaration));
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
		assertEquals(17, getLineNumber(variableDeclaration));
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
		assertEquals(23, getLineNumber(variableDeclaration));
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
	public void testGetStartPositionOfMethodDeclarationBody() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceAssignAndUseMultiTimes", "fis.available()");
		
		Method method = getMethodGetStartPositionOfMethodDeclarationBody();
		int position = (Integer) method.invoke(finder, methodInvocation);
		assertEquals(10, compilationUnit.getLineNumber(position));
	}
	
	@Test
	public void testResourceWithMultipleAssignmentAndClose() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceWithMultipleAssignmentAndClose", "fis.close()");
		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(53, compilationUnit.getLineNumber(detectionStartPosition));
	}

	@Test
	public void testResourceAssignmentInIfElseBlock() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceAssignmentInIfElseBlock", "fis.close()");
		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(62, compilationUnit.getLineNumber(detectionStartPosition));
	}
	
	@Test
	public void testResourceAssignmentInSwitchBlock() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceAssignmentInSwitchBlock", "fis.close()");
		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(73, compilationUnit.getLineNumber(detectionStartPosition));
	}
	
	@Test
	public void testInitializedResourceAssignAgainInIfElseBlock() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"initializedResourceAssignAgainInIfElseBlock", "fis.close()");
		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(89, compilationUnit.getLineNumber(detectionStartPosition));
	}
	
	@Test
	public void testResourceAssignmentByQuestionColonOperator() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceAssignmentByQuestionColonOperator", "fis.close()");
		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(100, compilationUnit.getLineNumber(detectionStartPosition));
	}
	
	@Test
	public void testAssignmentAndCloseInTheSameIfBlockAndAreSibling() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"assignmentAndCloseInTheSameIfBlockAndAreSibling", "fis.close()");
		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(108, compilationUnit.getLineNumber(detectionStartPosition));
	}
	
	@Test
	public void testAssignmentAndCloseInTheSameIfBlockButAreNotSibling() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"assignmentAndCloseInTheSameIfBlockButAreNotSibling", "fis.close()");
		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(117, compilationUnit.getLineNumber(detectionStartPosition));
	}
	
	@Test
	public void testAssignmentAndCloseInDifferentIfBlockButAreAtSameDepth() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"assignmentAndCloseInDifferentIfBlockButAreAtSameDepth", "fis.close()");
		int detectionStartPosition = finder.findPosition(methodInvocation);
		assertEquals(127, compilationUnit.getLineNumber(detectionStartPosition));
	}
	
	private Method getMethodGetStartPositionOfMethodDeclarationBody() throws NoSuchMethodException {
		Method method = ClosingResourceBeginningPositionFinder.class
				.getDeclaredMethod("getStartPositionOfMethodDeclarationBody", MethodInvocation.class);
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
	 *                If node is not a sub-node of this compilationUnit
	 */
	private int getLineNumber(ASTNode node) {
		if (!node.getRoot().equals(compilationUnit)) {
			throw new IllegalArgumentException("Not in this compilationUnit");
		}
		return compilationUnit.getLineNumber(node.getStartPosition());
	}
}
