package ntut.csie.analyzer.careless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import ntut.csie.analyzer.careless.closingmethod.ResourceCloser;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.testutility.TestEnvironmentBuilder;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class MethodInvocationMayInterruptByExceptionCheckerTest {

	TestEnvironmentBuilder environmentBuilder;
	CompilationUnit compilationUnit;
	CloseInvocationExecutionChecker checker;

	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder();
		environmentBuilder.createEnvironment();
		environmentBuilder.loadClass(MethodInvocationMayInterruptByExceptionCheckerExample.class);
		compilationUnit = environmentBuilder.getCompilationUnit(MethodInvocationMayInterruptByExceptionCheckerExample.class);
		checker = new CloseInvocationExecutionChecker();
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithCloseResourceByInvokeMyClose()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeResourceByInvokeMyClose", "this.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());

		methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeResourceByInvokeMyClose", "close()");
		assertEquals(1, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithGetterMethodReturningCloseableObject()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"invokeGetResourceAndCloseItNotImpCloseable",
				"resourceManager.getResourceNotImpCloseable().close()");
		assertEquals(1, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithUserDefinedClosedMethodWithCloseableArgument()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeByUserDefinedMethod",
				"ResourceCloser.closeResourceDirectly(is)");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithResourceJustBeCreated()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"createAndCloseDirectlyWithNewFile", "fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithInTryBlock() throws Exception {
		// First "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		List<MethodInvocation> methodInvocationList = getSameResourceCloseManyTimesTestCaseInvocation();
		assertEquals(1 ,checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocationList.get(0)).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithInCatchBlock()
			throws Exception {
		// Second "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		List<MethodInvocation> methodInvocationList = getSameResourceCloseManyTimesTestCaseInvocation();
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocationList.get(1)).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithFirstStatementInCatchBlock()
			throws Exception {
		// Third "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		List<MethodInvocation> methodInvocationList = getSameResourceCloseManyTimesTestCaseInvocation();
		assertEquals(1 ,checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocationList.get(2)).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithIntDeclare() throws Exception {
		// Third "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableIntDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithStrtingDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableStringDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithCharDeclare() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableCharDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithbooleanDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableBooleanDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithIntAssignt() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableIntAssignment",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithStrtingAssign()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableStringAssignment",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithCharAssign() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableCharAssignment",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithBooleanAssign()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableBooleanAssignment",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithSpectIntDeclare()
			throws Exception {
		// Third "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"suspectVariableIntDeclarationOrAssignment",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithSpectStrtingDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"suspectVariableStringDeclarationOrAssignment",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithSpectCharDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"suspectVariableCharDeclarationOrAssignment",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithsuSpectVariableDeclarationOrAssignment()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"suspectVariableBooleanDeclarationOrAssignment",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithObjectDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"specialVariableDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithObjectNullDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"specialVariableDeclarationWithNull",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testgetASTNodesThatMayThrowExceptionBeforeCloseInvocationWithSpecialObjectNullDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"specialVariableAssignmentWithNull",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideInsideComparingBooleanStateIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenResourceCloseInsideInsideComparingBooleanStateIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideElseStatementAfterCheckingBooleanIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideElseStatementAfterCheckingBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideElseStatementAfterCheckingBooleanIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenResourceCloseInsidElseStatementAfterComparingBooleanStateIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsidElseStatementAfterComparingBooleanStateIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingBooleanIfStatementContainVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingBooleanIfStatementContainVariableDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationWhenResourceCloseAfterCheckingBooleanIfStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingBooleanIfStatementContainMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenResourceCloseAfterBooleanComparingIfStatementContainVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanComparingIfStatementContainVariableDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenResourceCloseAfterCompareBooleanStateIfStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCompareBooleanStateIfStatementContainMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterBooleanCheckingIfStatementContainBooleanCheckingIfStatementAndVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfStatementContainBooleanCheckingIfStatementAndVariableDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenresourceCloseAfterBooleanCheckingIfStatementContainBooleanCheckingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfStatementContainBooleanCheckingIfStatementAndMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterBooleanCheckingIfStatementContainNestedBooleanComparingIfStatementAndVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfStatementContainNestedBooleanComparingIfStatementAndVariableDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenresourceCloseAfterBooleanCheckingIfStatementContainNestedBooleanComparingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfStatementContainNestedBooleanComparingIfStatementAndMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterBooleanCheckingIfElseStatementContainVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfElseStatementContainVariableDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterBooleanCheckingIfElseStatementContainBooleanCheckingIfStatementAndVariableDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfElseStatementContainBooleanCheckingIfStatementAndVariableDeclare",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenResourceCloseAfterBooleanCheckingIfElseStatementContainBooleanCheckingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfElseStatementContainBooleanCheckingIfStatementAndMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenresourceCloseAfterBooleanCheckingIfElseStatementContainBooleanComparingIfStatementAndVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfElseStatementContainBooleanComparingIfStatementAndVariableDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingtwoBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingtwoBooleanIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterBooleanComparingIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingThreeAndOperandBooleanIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingthreeBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingThreeBooleanIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingtwoBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingtwoBooleanIfElseStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingThreeAndOperandBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingThreeBooleanIfElseStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingThreeBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingThreeBooleanIfElseStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideBooleanComparingIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideBooleanComparingIfElseStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingBooleanIfElseStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	//sibiling test???
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingtwoBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingtwoBooleanIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingThreeAndOperandBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingThreeAndOperandBooleanIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingThreeBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingThreeBooleanIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingtwoBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingtwoBooleanIfElseStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingThreeAndOperandBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingThreeAndOperandBooleanIfElseStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingThreeBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingThreeBooleanIfElseStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingTwoBooleanIfStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingTwoBooleanIfStatementContainMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResouRceCloseAfterCheckingMultiBooleanBooleanIfStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingMultiBooleanBooleanIfStatementContainMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingMultiBooleanBooleanIfStatementContainVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingMultiBooleanBooleanIfStatementContainVariableDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingTwoBooleanIfStatementAfterMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingTwoBooleanIfStatementAfterMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingTwoBooleanIfStatementBeforeMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingTwoBooleanIfStatementBeforeMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingMultiBooleanBooleanIfStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingMultiBooleanBooleanIfStatementContainMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingMultiBooleanBooleanElseStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingMultiBooleanBooleanElseStatementContainMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterMultiBooleanOperandCheckingIfStatementContainMultiBooleanOperandCheckingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterMultiBooleanOperandCheckingIfStatementContainMultiBooleanOperandCheckingIfStatementAndMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndMethodInvocation",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndVariableDeclaration",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInThePrefixIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInThePrefixIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInThePrefixElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInThePrefixElseStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInThePrefixElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInThePrefixElseIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterPrefixIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterPrefixIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterPrefixElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterPrefixElseStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterPrefixElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterPrefixElseIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsNullIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsNullIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsNullIfStatementAndAMethodInvocationInsideIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsNullIfStatementAndAMethodInvocationInsideIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsNullIfStatementAndAMethodInvocationInsideElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsNullIfStatementAndAMethodInvocationInsideElseStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsNullElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsNullElseIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsNullElseIfStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsNullElseIfStatementAndAMethodInvocationInside",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsSameIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsSameIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInCheckingInstanceIsSameIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInCheckingInstanceIsSameIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsSameElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsSameElseIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInCheckingInstanceIsSameElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInCheckingInstanceIsSameElseIfStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsSameIfStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsSameIfStatementAndAMethodInvocationInside",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsSameElseIfStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsSameElseIfStatementAndAMethodInvocationInside",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingInstanceIsSameIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsSameElseIfStatementAndAMethodInvocationInside",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingQualifiNameIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInTheCheckingQualifiedNameSameIfStatement",
				"qualifier.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterTheCheckingQualifiedNameSameIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTheCheckingQualifiedNameSameIfStatement",
				"qualifier.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}

	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInTheCheckingQualifiedNameSameElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInTheCheckingQualifiedNameSameElseIfStatement",
				"qualifier.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterTheCheckingQualifiedNameSameElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTheCheckingQualifiedNameSameElseIfStatement",
				"qualifier.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterTheCheckingQualifiedNameSameIfStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTheCheckingQualifiedNameSameIfStatementAndAMethodInvocationInside",
				"qualifier.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterTheCheckingQualifiedNameSameElseStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTheCheckingQualifiedNameSameElseStatementAndAMethodInvocationInside",
				"qualifier.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterTheCheckingQualifiedNameSameElseIfStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTheCheckingQualifiedNameSameElseIfStatementAndAMethodInvocationInside",
				"qualifier.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariableAssignmentStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariableAssignmentStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariableAssignmenWithInfixExpressionStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariableAssignmenWithInfixExpressionStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariableAssignmentWithInfixExpressionAndExtandOperandStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariableAssignmentWithInfixExpressionAndExtandOperandStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariableAssignmentWithParenthesizeExpression()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariableAssignmentWithParenthesizeExpression",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterMultiVariableAssignmentStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterMultiVariableAssignmentStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariablePrefixExpressionStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariablePrefixExpressionStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariablePostfixExpressionStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariablePostfixExpressionStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInTheSynchronizedStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInTheSynchronizedStatement",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterExceptionTryCatchBlock()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterExceptionTryCatchBlock",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterNestExceptionIOExceptionTryCatchBlock()//待跟peter合併便會消除
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterNestExceptionIOExceptionTryCatchBlock",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterMethodInvocationTryCatchBlock()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterMethodInvocationTryCatchBlock",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterNestExceptionTryCatchBlock()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterNestExceptionTryCatchBlock",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterThrowableTryCatchBlock()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterThrowableTryCatchBlock",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterIOExceptionTryCatchBlock()//合併後會消失
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterIOExceptionTryCatchBlock",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testResourceCloseAfterTryStatementThatThrowsRuntimeException()
	throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTryStatementThatThrowsRuntimeException",
				"fis.close()");
		assertEquals(1, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testResourceCloseAfterTryStatementThatCatchGenericException()
	throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTryStatementThatCatchGenericException",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testResourceCloseAfterTryStatementThatUsesBlanketCatchClause()
	throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTryStatementThatUsesBlanketCatchClause",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	@Test
	public void testResourceCloseAfterTryStatementThatCatchesThrowable()
	throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTryStatementThatCatchesThrowable",
				"fis.close()");
		assertEquals(0, checker.getASTNodesThatMayThrowExceptionBeforeCloseInvocation(methodInvocation).size());
	}
	
	private MethodInvocation getMethodInvocationByMethodNameAndCode(
			String methodName, String code) {
		List<MethodInvocation> methodInvocation = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(compilationUnit, methodName, code);
		assertEquals(methodInvocation.size(), 1);
		return methodInvocation.get(0);
	}
	
	private List<MethodInvocation> getSameResourceCloseManyTimesTestCaseInvocation() {
		List<MethodInvocation> methodInvocation = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(compilationUnit, "sameResourceCloseManyTimes", "fileOutputStream.close()");
		assertEquals(methodInvocation.size(), 3);
		return methodInvocation;
	}
}