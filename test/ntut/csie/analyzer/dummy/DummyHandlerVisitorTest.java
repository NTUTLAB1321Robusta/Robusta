package ntut.csie.analyzer.dummy;

import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.dummy.example.DifferentSignature;
import ntut.csie.analyzer.dummy.example.DummyInNestedTryAndOtherStructure;
import ntut.csie.analyzer.dummy.example.EmptyCatchBlock;
import ntut.csie.analyzer.dummy.example.Initializer;
import ntut.csie.analyzer.dummy.example.MultiBadSmellInOneMethodDeclaration;
import ntut.csie.analyzer.dummy.example.MultiPrintAndLogInOneCatch;
import ntut.csie.analyzer.dummy.example.NoCatchBlock;
import ntut.csie.analyzer.dummy.example.PrintAndSomethingElse;
import ntut.csie.analyzer.dummy.example.PrintWithCommentOnly;
import ntut.csie.analyzer.dummy.example.SimplestJavaLogger;
import ntut.csie.analyzer.dummy.example.SimplestLog4J;
import ntut.csie.analyzer.dummy.example.SimplestPrintStackTrace;
import ntut.csie.analyzer.dummy.example.SimplestSystemPrint;
import ntut.csie.analyzer.dummy.example.UserDefinedClassDeclaration;
import ntut.csie.analyzer.dummy.example.UserDefinedClass;
import ntut.csie.analyzer.dummy.example.UserDefinedFullName;
import ntut.csie.analyzer.dummy.example.UserDefinedMethod;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.testutility.Assertor;
import ntut.csie.testutility.TestEnvironmentBuilder;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DummyHandlerVisitorTest {

	private TestEnvironmentBuilder environmentBuilder;
	private DummyHandlerVisitor adVisitor;
	SmellSettings smellSettings;

	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("DummyHandlerExampleProject");
		environmentBuilder.createEnvironment();

		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	@Test
	public void testNoCatchBlock() throws Exception {
		// Initialized
		Class<?> testedClass = NoCatchBlock.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testEmptyCatchBlock() throws Exception {
		// Initialized
		Class<?> testedClass = EmptyCatchBlock.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testSimplestPrintStackTrace() throws Exception {
		// Initialized
		Class<?> testedClass = SimplestPrintStackTrace.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		Assertor.assertMarkerInfoListSize(1, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testSimplestPrintStackTraceWithoutDetection() throws Exception {
		// Initialized
		Class<?> testedClass = SimplestPrintStackTrace.class;
		environmentBuilder.loadClass(testedClass);
		detectSystemPrints();
		detectLog4j();
		detectJavaLogger();
		
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
	}
	
	@Test
	public void testSimplestSystemPrint() throws Exception {
		// Initialized
		Class<?> testedClass = SimplestSystemPrint.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		Assertor.assertMarkerInfoListSize(4, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testSimplestSystemPrintWithoutDetection() throws Exception {
		// Initialized
		Class<?> testedClass = SimplestSystemPrint.class;
		environmentBuilder.loadClass(testedClass);
		detectPrintStackTrace();
		detectLog4j();
		detectJavaLogger();
		
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testSimplestJavaLogger() throws Exception {
		// Initialized
		Class<?> testedClass = SimplestJavaLogger.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		Assertor.assertMarkerInfoListSize(2, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testSimplestJavaLoggerWithoutDetection() throws Exception {
		// Initialized
		Class<?> testedClass = SimplestJavaLogger.class;
		environmentBuilder.loadClass(testedClass);
		detectPrintStackTrace();
		detectSystemPrints();
		detectLog4j();
		
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
	}
	
	@Test
	public void testSimplestLog4J() throws Exception {
		// Initialized
		Class<?> testedClass = SimplestLog4J.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		Assertor.assertMarkerInfoListSize(1, visitCompilationAndGetSmellList(testedClass));
	}
	
	@Test
	public void testSimplestLog4JWithoutDetection() throws Exception {
		// Initialized
		Class<?> testedClass = SimplestLog4J.class;
		environmentBuilder.loadClass(testedClass);
		detectPrintStackTrace();
		detectSystemPrints();
		detectJavaLogger();
		
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testInitializer() throws Exception {
		// Initialized
		Class<?> testedClass = Initializer.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		Assertor.assertMarkerInfoListSize(1, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testPrintWithCommentOnly() throws Exception {
		// Initialized
		Class<?> testedClass = PrintWithCommentOnly.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		Assertor.assertMarkerInfoListSize(1, visitCompilationAndGetSmellList(testedClass));
	}
	
	@Test
	public void testDifferentSignature() throws Exception {
		// Initialized
		Class<?> testedClass = DifferentSignature.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		Assertor.assertMarkerInfoListSize(4, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testPrintAndSomethingElse() throws Exception {
		// Initialized
		Class<?> testedClass = PrintAndSomethingElse.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testPrintInNestedTryAndOtherStructure() throws Exception {
		// Initialized
		Class<?> testedClass = DummyInNestedTryAndOtherStructure.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();

		Assertor.assertMarkerInfoListSize(3, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testMultiBadSmellInOneMethodDeclaration() throws Exception {
		// Initialized
		Class<?> testedClass = MultiBadSmellInOneMethodDeclaration.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();

		Assertor.assertMarkerInfoListSize(3, visitCompilationAndGetSmellList(testedClass));
	}
	
	@Test
	public void testMultiPrintAndLogInOneCatch() throws Exception {
		// Initialized
		Class<?> testedClass = MultiPrintAndLogInOneCatch.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		Assertor.assertMarkerInfoListSize(1, visitCompilationAndGetSmellList(testedClass));
	}
	
	@Test
	public void testUserDefinedMethod() throws Exception {
		// Initialized
		Class<?> testedClass = UserDefinedMethod.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		// Before setting
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
		
		// After setting
		addUserDefinedPattern("*.toString", true);
		Assertor.assertMarkerInfoListSize(1, visitCompilationAndGetSmellList(testedClass));

		addUserDefinedPattern("*.toString", false);
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
		
		addUserDefinedPattern("*.toCharArray", true);
		Assertor.assertMarkerInfoListSize(1, visitCompilationAndGetSmellList(testedClass));
	}
	
	@Test
	public void testUserDefinedClass() throws Exception {
		// Initialized
		Class<?> testedClass = UserDefinedClass.class;
		environmentBuilder.loadClass(testedClass);
		environmentBuilder.loadClass(UserDefinedClassDeclaration.class);
		detectAllExtraRules();
		
		// Before setting
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
		
		// After setting
		String testClassPattern = UserDefinedClassDeclaration.class.getName() + ".*";
		addUserDefinedPattern(testClassPattern, true);
		Assertor.assertMarkerInfoListSize(1, visitCompilationAndGetSmellList(testedClass));
		addUserDefinedPattern(testClassPattern, false);
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
	}

	@Test
	public void testUserDefinedFullName() throws Exception {
		// Initialized
		Class<?> testedClass = UserDefinedFullName.class;
		environmentBuilder.loadClass(testedClass);
		detectAllExtraRules();
		
		// Before setting
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
		
		// After setting
		addUserDefinedPattern("java.util.ArrayList.add", true);
		Assertor.assertMarkerInfoListSize(1, visitCompilationAndGetSmellList(testedClass));
		addUserDefinedPattern("java.util.ArrayList.add", false);
		Assertor.assertMarkerInfoListSize(0, visitCompilationAndGetSmellList(testedClass));
	}

	private List<MarkerInfo> visitCompilationAndGetSmellList(Class clazz)
			throws JavaModelException {
		CompilationUnit unit = environmentBuilder.getCompilationUnit(clazz);
		adVisitor = new DummyHandlerVisitor(unit);
		unit.accept(adVisitor);

		return adVisitor.getDummyList();
	}

	private void detectAllExtraRules() {
		detectPrintStackTrace();
		detectSystemPrints();
		detectLog4j();
		detectJavaLogger();
	}

	private void detectPrintStackTrace() {
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

	private void detectSystemPrints() {
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrintln);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

	private void detectLog4j() {
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private void detectJavaLogger() {
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

	private void addUserDefinedPattern(String pattern, boolean isDetected) {
		smellSettings.addDummyHandlerPattern(pattern, isDetected);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
