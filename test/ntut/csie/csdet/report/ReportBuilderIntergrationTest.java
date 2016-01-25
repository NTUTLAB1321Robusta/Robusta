package ntut.csie.csdet.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.careless.CarelessCleanupBaseExample;
import ntut.csie.analyzer.careless.MethodInvocationBeforeClose;
import ntut.csie.analyzer.careless.closingmethod.ClassCanCloseButNotImplementCloseable;
import ntut.csie.analyzer.careless.closingmethod.ClassImplementCloseable;
import ntut.csie.analyzer.careless.closingmethod.ClassImplementCloseableWithoutThrowException;
import ntut.csie.analyzer.careless.closingmethod.UserDefinedCarelessCleanupClass;
import ntut.csie.analyzer.careless.closingmethod.UserDefinedCarelessCleanupMethod;
import ntut.csie.analyzer.nested.NestedTryStatementExample;
import ntut.csie.analyzer.thrown.ExceptionThrownFromFinallyBlockExample;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramExample;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramWithCatchThrowableExample;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramWithTry;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramWithTryAtLastStatement;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramWithTryAtMiddleStatement;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramWithoutCatchRightExceptionExample;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramWithoutStatementExample;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.analyzer.unprotected.UnprotectedmainProgramWithTryAtFirstStatement;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReportBuilderIntergrationTest {
	private JavaFileToString javaFileToString;
	private JavaProjectMaker javaProjectMaker;
	private ReportBuilder reportBuilder;
	private ReportModel reportModel;
	private IProject project;
	private String projectName;
	private SmellSettings smellSettings;
	private static boolean isDetecting = true;
	private static boolean unDetcting = false;
	
	public ReportBuilderIntergrationTest() {
		projectName = "ReportBuilderIntergrationTest";
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		javaFileToString = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/slf4j-api-1.5.0.jar");
		javaProjectMaker.addClasspathEntryToBuildPath(BuildPathSupport.getJUnit4ClasspathEntry(), null);
		javaProjectMaker.packageAgileExceptionClassesToJarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		
		// load test example
		loadClass(NestedTryStatementExample.class); 
		loadClass(CarelessCleanupBaseExample.class); 
		loadClass(MethodInvocationBeforeClose.class);
		loadClass(ClassImplementCloseable.class);
		loadClass(ClassImplementCloseableWithoutThrowException.class);
		loadClass(ClassCanCloseButNotImplementCloseable.class);
		loadClass(UserDefinedCarelessCleanupClass.class);
		loadClass(UserDefinedCarelessCleanupMethod.class);
		loadClass(UnprotectedMainProgramExample.class);
		loadClass(UnprotectedMainProgramWithCatchThrowableExample.class);
		loadClass(UnprotectedMainProgramWithoutStatementExample.class);	
		loadClass(UnprotectedMainProgramWithoutTryExample.class);
		loadClass(UnprotectedMainProgramWithTry.class);
		loadClass(UnprotectedmainProgramWithTryAtFirstStatement.class);
		loadClass(UnprotectedMainProgramWithTryAtLastStatement.class);
		loadClass(UnprotectedMainProgramWithTryAtMiddleStatement.class);
		loadClass(UnprotectedMainProgramWithoutCatchRightExceptionExample.class);
		loadClass(ExceptionThrownFromFinallyBlockExample.class);
		
		InitailSetting();
		
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		reportBuilder = new ReportBuilder(project, new NullProgressMonitor());
		reportModel = reportBuilder.getReportModel();
	}
	
	private void loadClass(Class clazz) throws Exception {
		javaFileToString.read(clazz, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				clazz.getPackage().getName(),
				clazz.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ clazz.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
	}
	
	private void InitailSetting() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		DisableAllSmellDetect();
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	/**
	 * check all bad smell type on detecting setting page except dummy handler and empty catch block.
	 * load user defined setting.
	 */
	private void CreateAllSettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EMPTYCATCHBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYSTATEMENT, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EXCEPTIONTHROWNFROMFINALLYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);

		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	
	private void DisableAllSmellDetect() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EMPTYCATCHBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYSTATEMENT, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EXCEPTIONTHROWNFROMFINALLYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);	
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	
	private void CreateDummyAndIgnoreSettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EMPTYCATCHBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private void CreateNestedTrySettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYSTATEMENT, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

	private void CreateUnprotectedMainProgramSettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private void CreateCarelessCleanupSettings() {
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private void CreateCarelessCleanupWithUserDefinition() {
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addCarelessCleanupPattern("*.didNotDeclareAnyExceptionButThrowUnchecked", isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		File settingFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(settingFile.exists()) {
			assertTrue(settingFile.delete());
		}
		javaProjectMaker.deleteProject();
	}
	
	private void invokeAnalysis() throws Exception {
		Method analysisProject = ReportBuilder.class.getDeclaredMethod("analysisProject", IProject.class);
		analysisProject.setAccessible(true);
		analysisProject.invoke(reportBuilder, project);
	}
	
	@Test
	public void assertInitialEnvironmentHasNoSmell() {
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK) == 0);
	}
	
	@Test
	public void testDummyHandlerAndEmptyCatchBlockBadSmellReport() throws Exception {
		CreateDummyAndIgnoreSettings();
		
		invokeAnalysis();
		
		assertEquals(39, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(19, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK));
		assertEquals(58, reportModel.getAllSmellSize());
	}
	
	@Test
	public void testOnlyNestedTryBadSmellReport() throws Exception {
		CreateNestedTrySettings();

		invokeAnalysis();
		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(51, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK));
		assertEquals(51, reportModel.getAllSmellSize());
	}
	
	@Test
	public void testOnlyUnprotectedMainProgramBadSmellReport() throws Exception {
		CreateUnprotectedMainProgramSettings();

		invokeAnalysis();
		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(6, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK));
		assertEquals(6, reportModel.getAllSmellSize());
	}
	
	@Test
	public void testOnlyCarelessCleanupBadSmellReport() throws Exception {
		CreateCarelessCleanupSettings();

		invokeAnalysis();
		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		/*
		 * 6 in CarelessCleanupBaseExample
		 * 1 in ExceptionThrownFromFinallyBlockExample
		 */
		assertEquals(7, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK));
		assertEquals(7, reportModel.getAllSmellSize());
	}

	@Test
	public void testOnlyCarelessCleanupWithExtraRule() throws Exception {
		CreateCarelessCleanupWithUserDefinition();

		invokeAnalysis();
		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		/*
		 * 9 in CarelessCleanupBaseExample (with 3 by extra rules)
		 * 1 in ExceptionThrownFromFinallyBlockExample
		 */
		assertEquals(10, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK));
		assertEquals(10, reportModel.getAllSmellSize());
	}
	
	/**
	 * check all bad smell type on detecting setting page.
	 * this testing is focused on generating report except DummyHandler's and EmptyCatchBlock's report
	 * @throws Exception
	 */
	@Test
	public void testRemainBadSmellReport() throws Exception {
		CreateAllSettings();
		
		invokeAnalysis();
		
		assertEquals(39,reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(19,reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		/*
		 * 6 in CarelessCleanupBaseExample (with 3 by extra rules)
		 * 1 in ExceptionThrownFromFinallyBlockExample
		 */
		assertEquals(7, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(51, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(6, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(38, reportModel.getSmellSize(RLMarkerAttribute.CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK));
		assertEquals(110, reportModel.getTryCounter());
		assertEquals(133, reportModel.getCatchCounter()); // some 'catch(', other 'catch ('
		assertEquals(50, reportModel.getFinallyCounter());
		assertEquals(5, reportModel.getPackagesSize());
		assertEquals(projectName, reportModel.getProjectName());
		assertEquals(1384, reportModel.getTotalLine());
		assertEquals(160, reportModel.getAllSmellSize());
	}
}