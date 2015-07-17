package ntut.csie.analyzer.careless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import ntut.csie.analyzer.careless.closingmethod.ResourceCloser;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.testutility.TestEnvironmentBuilder;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
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
		environmentBuilder.createEnvironment();

		environmentBuilder.loadClass(MethodInvocationMayInterruptByExceptionCheckerExample.class);

		compilationUnit = environmentBuilder.getCompilationUnit(MethodInvocationMayInterruptByExceptionCheckerExample.class);

		checker = new MethodInvocationMayInterruptByExceptionChecker();
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
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
				"invokeGetResourceAndCloseItWithInterface",
				"resourceManager.getResourceWithInterface().close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));

		methodInvocation = getMethodInvocationByMethodNameAndCode(
				"invokeGetResourceAndCloseItNotImpCloseable",
				"resourceManager.getResourceNotImpCloseable().close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithUserDefinedClosedMethodWithCloseableArgument()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeByUserDefinedMethod",
				"ResourceCloser.closeResourceDirectly(is)");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithResourceJustBeCreated()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"createAndCloseDirectlyWithNewFile", "fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithInTryBlock() throws Exception {
		// First "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation methodInvocation = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 2641 - 1, 24);
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithInCatchBlock()
			throws Exception {
		// Second "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation methodInvocation = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 2710 - 1, 24);
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithFirstStatementInCatchBlock()
			throws Exception {
		// Third "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation methodInvocation = (MethodInvocation) NodeFinder
				.perform(compilationUnit, 2806 - 1, 24);
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithIntDeclare() throws Exception {
		// Third "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableIntDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithStrtingDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableStringDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithCharDeclare() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableCharDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithbooleanDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableBooleanDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithIntAssignt() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableIntAssignment",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithStrtingAssign()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableStringAssignment",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithCharAssign() throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableCharAssignment",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithBooleanAssign()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"variableBooleanAssignment",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithSpectIntDeclare()
			throws Exception {
		// Third "fileOutputStream.close()" in method
		// "sameResourceCloseManyTimes"
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"suspectVariableIntDeclarationOrAssignment",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithSpectStrtingDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"suspectVariableStringDeclarationOrAssignment",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithSpectCharDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"suspectVariableCharDeclarationOrAssignment",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithsuSpectVariableDeclarationOrAssignment()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"suspectVariableBooleanDeclarationOrAssignment",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithObjectDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"specialVariableDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithObjectNullDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"specialVariableDeclarationWithNull",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMayInterruptByExceptionWithSpecialObjectNullDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"specialVariableAssignmentWithNull",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideInsideComparingBooleanStateIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenResourceCloseInsideInsideComparingBooleanStateIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideElseStatementAfterCheckingBooleanIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideElseStatementAfterCheckingBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideElseStatementAfterCheckingBooleanIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenResourceCloseInsidElseStatementAfterComparingBooleanStateIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsidElseStatementAfterComparingBooleanStateIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingBooleanIfStatementContainVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingBooleanIfStatementContainVariableDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationWhenResourceCloseAfterCheckingBooleanIfStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingBooleanIfStatementContainMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenResourceCloseAfterBooleanComparingIfStatementContainVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanComparingIfStatementContainVariableDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenResourceCloseAfterCompareBooleanStateIfStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCompareBooleanStateIfStatementContainMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterBooleanCheckingIfStatementContainBooleanCheckingIfStatementAndVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfStatementContainBooleanCheckingIfStatementAndVariableDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenresourceCloseAfterBooleanCheckingIfStatementContainBooleanCheckingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfStatementContainBooleanCheckingIfStatementAndMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterBooleanCheckingIfStatementContainNestedBooleanComparingIfStatementAndVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfStatementContainNestedBooleanComparingIfStatementAndVariableDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenresourceCloseAfterBooleanCheckingIfStatementContainNestedBooleanComparingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfStatementContainNestedBooleanComparingIfStatementAndMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterBooleanCheckingIfElseStatementContainVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfElseStatementContainVariableDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterBooleanCheckingIfElseStatementContainBooleanCheckingIfStatementAndVariableDeclare()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfElseStatementContainBooleanCheckingIfStatementAndVariableDeclare",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenResourceCloseAfterBooleanCheckingIfElseStatementContainBooleanCheckingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfElseStatementContainBooleanCheckingIfStatementAndMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanComparingIfStatementCaughtWhenresourceCloseAfterBooleanCheckingIfElseStatementContainBooleanComparingIfStatementAndVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterBooleanCheckingIfElseStatementContainBooleanComparingIfStatementAndVariableDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingtwoBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingtwoBooleanIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterBooleanComparingIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingThreeAndOperandBooleanIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingthreeBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingThreeBooleanIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingtwoBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingtwoBooleanIfElseStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingThreeAndOperandBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingThreeBooleanIfElseStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingThreeBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingThreeBooleanIfElseStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideBooleanComparingIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideBooleanComparingIfElseStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseInsideCheckingBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingBooleanIfElseStatement",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	//sibiling test
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingtwoBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingtwoBooleanIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingThreeAndOperandBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingThreeAndOperandBooleanIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingThreeBooleanIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingThreeBooleanIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingtwoBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingtwoBooleanIfElseStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingThreeAndOperandBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingThreeAndOperandBooleanIfElseStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsBooleanCheckingIfstatementExemptedWhenResourceCloseAfterCheckingThreeBooleanIfElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingThreeBooleanIfElseStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingTwoBooleanIfStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingTwoBooleanIfStatementContainMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResouRceCloseAfterCheckingMultiBooleanBooleanIfStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingMultiBooleanBooleanIfStatementContainMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingMultiBooleanBooleanIfStatementContainVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingMultiBooleanBooleanIfStatementContainVariableDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingTwoBooleanIfStatementAfterMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingTwoBooleanIfStatementAfterMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingTwoBooleanIfStatementBeforeMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingTwoBooleanIfStatementBeforeMethodInvocation",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingMultiBooleanBooleanIfStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingMultiBooleanBooleanIfStatementContainMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingMultiBooleanBooleanElseStatementContainMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideCheckingMultiBooleanBooleanElseStatementContainMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterMultiBooleanOperandCheckingIfStatementContainMultiBooleanOperandCheckingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterMultiBooleanOperandCheckingIfStatementContainMultiBooleanOperandCheckingIfStatementAndMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndMethodInvocation()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInsideMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndMethodInvocation",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndVariableDeclaration()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterMultiBooleanCheckingIfElseStatementContainMultiBooleanCheckingIfStatementAndVariableDeclaration",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInThePrefixIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInThePrefixIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInThePrefixElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInThePrefixElseStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInThePrefixElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInThePrefixElseIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterPrefixIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterPrefixIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterPrefixElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterPrefixElseStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterPrefixElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterPrefixElseIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsNullIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsNullIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsNullIfStatementAndAMethodInvocationInsideIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsNullIfStatementAndAMethodInvocationInsideIfStatement",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsNullIfStatementAndAMethodInvocationInsideElseStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsNullIfStatementAndAMethodInvocationInsideElseStatement",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsNullElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsNullElseIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsNullElseIfStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsNullElseIfStatementAndAMethodInvocationInside",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsSameIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsSameIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInCheckingInstanceIsSameIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInCheckingInstanceIsSameIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsSameElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsSameElseIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInCheckingInstanceIsSameElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInCheckingInstanceIsSameElseIfStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsSameIfStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsSameIfStatementAndAMethodInvocationInside",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterCheckingInstanceIsSameElseIfStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsSameElseIfStatementAndAMethodInvocationInside",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingInstanceIsSameIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterCheckingInstanceIsSameElseIfStatementAndAMethodInvocationInside",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInsideCheckingQualifiNameIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInTheCheckingQualifiedNameSameIfStatement",
				"qualifier.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterTheCheckingQualifiedNameSameIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTheCheckingQualifiedNameSameIfStatement",
				"qualifier.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInTheCheckingQualifiedNameSameElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInTheCheckingQualifiedNameSameElseIfStatement",
				"qualifier.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterTheCheckingQualifiedNameSameElseIfStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTheCheckingQualifiedNameSameElseIfStatement",
				"qualifier.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterTheCheckingQualifiedNameSameIfStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTheCheckingQualifiedNameSameIfStatementAndAMethodInvocationInside",
				"qualifier.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterTheCheckingQualifiedNameSameElseStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTheCheckingQualifiedNameSameElseStatementAndAMethodInvocationInside",
				"qualifier.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterTheCheckingQualifiedNameSameElseIfStatementAndAMethodInvocationInside()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterTheCheckingQualifiedNameSameElseIfStatementAndAMethodInvocationInside",
				"qualifier.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariableAssignmentStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariableAssignmentStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariableAssignmenWithInfixExpressionStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariableAssignmenWithInfixExpressionStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariableAssignmentWithInfixExpressionAndExtandOperandStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariableAssignmentWithInfixExpressionAndExtandOperandStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariableAssignmentWithParenthesizeExpression()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariableAssignmentWithParenthesizeExpression",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterMultiVariableAssignmentStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterMultiVariableAssignmentStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariablePrefixExpressionStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariablePrefixExpressionStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterVariablePostfixExpressionStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterVariablePostfixExpressionStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseInTheSynchronizedStatement()
			throws Exception {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseInTheSynchronizedStatement",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	private MethodInvocation getMethodInvocationByMethodNameAndCode(
			String methodName, String code) {
		List<MethodInvocation> methodInvocation = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(compilationUnit, methodName, code);
		assertEquals(methodInvocation.size(), 1);
		return methodInvocation.get(0);
	}
}