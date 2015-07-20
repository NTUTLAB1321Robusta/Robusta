package ntut.csie.analyzer.careless;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.careless.closingmethod.CloseResourceMethodInvocationExample;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.testutility.Assertor;
import ntut.csie.testutility.TestEnvironmentBuilder;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CarelessCleanupDefinitionTest {

	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnit;
	private MethodInvocationMayInterruptByExceptionChecker checker;
	private CloseResourceMethodInvocationVisitor visitor;
	private File fakeSmellSettingFile;

	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("CarelessCleanupDefinitionTestProject");
		environmentBuilder.createEnvironment();

		environmentBuilder.loadClass(CarelessCleanupDefinitionExample.class);
		compilationUnit = environmentBuilder.getCompilationUnit(CarelessCleanupDefinitionExample.class);

		checker = new MethodInvocationMayInterruptByExceptionChecker();
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	/*
	 *  The following tests use CloseResourceMethodInvocationVisitor to check if certain statements would be
	 *  treated as close statement.
	 */
	@Test
	public void testNonclosableResourceClosing() throws Exception {
		List<MethodInvocation> carelessCleanupSuspectList = visitCompilationAndGetSmellList(CarelessCleanupDefinitionExample.class);
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"noncloseableResourceClosing",
				"nonCloseableResource.close()");
		assertFalse(contains(carelessCleanupSuspectList, methodInvocation));
	}

	@Test
	public void testClosableResourceClosing() throws Exception {
		List<MethodInvocation> carelessCleanupSuspectList = visitCompilationAndGetSmellList(CarelessCleanupDefinitionExample.class);
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeableResourceClosing",
				"closeableResource.close()");
		assertTrue(contains(carelessCleanupSuspectList, methodInvocation));
	}

	@Test
	public void testClosableResourceClosingMethodNotNamedClose() throws Exception {
		List<MethodInvocation> carelessCleanupSuspectList = visitCompilationAndGetSmellList(CarelessCleanupDefinitionExample.class);
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeableResourceClosingMethodNotNamedClose",
				"resource.cleanUp()");
		assertFalse(contains(carelessCleanupSuspectList, methodInvocation));
	}
	
	@Test
	public void testCloseMethodNotNamedClosePassedInCloseableResource() throws Exception {
		MockCloseResourceMethodInvocationVisitor closeResourceMethodInvocationCollector;
		MethodInvocation methodInvocation;
		List<MethodInvocation> carelessCleanupSuspectList;
		
		// set up
		// create fake smell setting file to simulate user chooses to detect Careless Cleanup and 
		// checks "Also detect this bad smell out of try statement"
		fakeSmellSettingFile = new File("fakeSmellSettingFile.xml");
		createFakeSmellSettingFileDetectingCCAndCheckingAlsoDetectThisBadSmellOutOfTry(fakeSmellSettingFile);
		
		try {
			closeResourceMethodInvocationCollector = new MockCloseResourceMethodInvocationVisitor(compilationUnit);
			methodInvocation = getMethodInvocationByMethodNameAndCode("closeMethodNotNamedClosePassedInCloseableResource", "cleanUp(fileInputStream)");
			compilationUnit.accept(closeResourceMethodInvocationCollector);
			carelessCleanupSuspectList = closeResourceMethodInvocationCollector.getCloseMethodInvocations();
		} finally {
			// tear down
			// delete fake smell setting file
			fakeSmellSettingFile.delete();
		}
		
		assertTrue(contains(carelessCleanupSuspectList, methodInvocation));
	}
	
	@Test
	public void testcloseMethodNotNamedClosePassedInAutoCloseableResource() throws Exception {
		MockCloseResourceMethodInvocationVisitor closeResourceMethodInvocationCollector;
		MethodInvocation methodInvocation;
		List<MethodInvocation> carelessCleanupSuspectList;
		
		// set up
		// create fake smell setting file to simulate user chooses to detect Careless Cleanup and 
		// checks "Also detect this bad smell out of try statement"
		fakeSmellSettingFile = new File("fakeSmellSettingFile.xml");
		createFakeSmellSettingFileDetectingCCAndCheckingAlsoDetectThisBadSmellOutOfTry(fakeSmellSettingFile);
		
		try {
			closeResourceMethodInvocationCollector = new MockCloseResourceMethodInvocationVisitor(compilationUnit);
			methodInvocation = getMethodInvocationByMethodNameAndCode("closeMethodNotNamedClosePassedInAutoCloseableResource", "cleanUp(os)");
			compilationUnit.accept(closeResourceMethodInvocationCollector);
			carelessCleanupSuspectList = closeResourceMethodInvocationCollector.getCloseMethodInvocations();
		} finally {
			// tear down
			// delete fake smell setting file
			fakeSmellSettingFile.delete();
		}
		
		assertTrue(contains(carelessCleanupSuspectList, methodInvocation));
	}

	/*
	 *  The following tests use MethodInvocationMayInterruptByExceptionChecker to check if certain statements
	 *  would be treated as would or have possibility to raise exception
	 */
	@Test
	public void testExceptionBeforeLastResourceAssignment() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"exceptionBeforeLastResourceAssignment",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testExceptionBeforeLastResourceAssignmentThatMayNotBeExecuted() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"exceptionBeforeLastResourceAssignmentThatMayNotBeExecuted",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testAStatementInBetweenDetectionRange() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"aStatementInBetweenDetectionRange",
				"fis.close()");
		assertTrue(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIfStatementCheckingBooleanVariableInBetweenDetectionRange() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"ifStatementCheckingBooleanVariableInBetweenDetectionRange",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testIfStatementCheckingResourceIsNotNullContainClose() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"ifStatementCheckingResourceIsNotNullContainClose",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIfStatementCheckingResourceIsSameContainClose() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"ifStatementCheckingResourceIsSameContainClose",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIfStatementCheckingResourceIsSameBeforeClose() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"ifStatementCheckingResourceIsSameBeforeClose",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testTryBlockCatchingAllPossibleExceptionInBetweenDetectionRange() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"tryBlockCatchingAllPossibleExceptionInBetweenDetectionRange",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testObjectDeclarationWithoutAssignmentInBetweenDetectionRange() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"objectDeclarationWithoutAssignmentInBetweenDetectionRange",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}

	@Test
	public void testPrimitiveVariableDeclarationWithAssignmentInBetweenDetectionRange() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"primitiveVariableDeclarationWithAssignmentInBetweenDetectionRange",
				"fis.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceClosingInIfStatementCheckingQualifiedName() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceClosingInIfStatementCheckingQualifiedName",
				"qualifier.close()");
		assertFalse(checker.isMayInterruptByException(methodInvocation));
	}
	
	@Test
	public void testIsMethodInvocationCaughtWhenResourceCloseAfterExpressionStatement() {
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"resourceCloseAfterExpressionStatement",
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

	/* 
	 *  utility methods
	 */
	private MethodInvocation getMethodInvocationByMethodNameAndCode(
			String methodName, String code) {
		List<MethodInvocation> methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						methodName, code);
		assertEquals(methodInvocation.size(), 1);

		return methodInvocation.get(0);
	}

	private List<MethodInvocation> visitCompilationAndGetSmellList(Class clazz)
			throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(clazz);
		visitor = new CloseResourceMethodInvocationVisitor(compilationUnit);
		compilationUnit.accept(visitor);
		List<MethodInvocation> miList = visitor.getCloseMethodInvocations();
		return miList;
	}
	
	private boolean contains(List<MethodInvocation> list, MethodInvocation methodInvocationPassedIn) {
		boolean contains = false;
		for(MethodInvocation methodInvocationInList : list) {
			if(methodInvocationInList.toString().equals(methodInvocationPassedIn.toString()))
				contains = true;
		}
		return contains;
	}
	
	private void createFakeSmellSettingFileDetectingCCAndCheckingAlsoDetectThisBadSmellOutOfTry(File fakeSmellSettingFile) throws IOException {
		Writer writer = null;

		try {
		    writer = new BufferedWriter(new OutputStreamWriter(
		          new FileOutputStream(fakeSmellSettingFile), "utf-8"));
		    writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
		    		"<CodeSmells><SmellTypes name=\"EmptyCatchBlock\" isDetecting=\"false\" />" +
		    		"<SmellTypes name=\"DummyHandler\" isDetecting=\"false\" />" +
		    		"<SmellTypes name=\"NestedTryStatement\" isDetecting=\"false\" />" +
		    		"<SmellTypes name=\"UnprotectedMainProgram\" isDetecting=\"false\" />" +
		    		"<SmellTypes name=\"CarelessCleanup\" isDetecting=\"true\">" +
		    		"<extraRule name=\"DetectOutOfTryStatement\" /></SmellTypes>" +
		    		"<SmellTypes name=\"OverLogging\" isDetecting=\"false\" />" +
		    		"<SmellTypes name=\"ExceptionThrownFromFinallyBlock\" isDetecting=\"false\" />" +
		    		"<Preferences name=\"ShowRLAnnotationWarning\" enable=\"false\" /></CodeSmells>");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
		   writer.close();
		}
	}
	
	public class MockCloseResourceMethodInvocationVisitor extends CloseResourceMethodInvocationVisitor {
		public MockCloseResourceMethodInvocationVisitor(CompilationUnit node) {
			super(node);
			// override constructor to use MockUserDefinedMethodAnalyzer.
			userDefinedMethodAnalyzer = new MockUserDefinedMethodAnalyzer(SmellSettings.SMELL_CARELESSCLEANUP);
		}
	}
	
	public class MockUserDefinedMethodAnalyzer extends UserDefinedMethodAnalyzer {
		public MockUserDefinedMethodAnalyzer(String smellName) {
			super(smellName);
			// override constant SETTINGFILEPATH to use customized setting file for SmellSettings.
			SmellSettings smellSettings = new SmellSettings(fakeSmellSettingFile.getPath());
			methodTreeMap = smellSettings.getSmellSettings(SmellSettings.SMELL_CARELESSCLEANUP);
			isEnable = smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP);
		}
	}
}
