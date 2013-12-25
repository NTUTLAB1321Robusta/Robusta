package ntut.csie.csdet.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.NestedTryStatementExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.ClassCanCloseButNotImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.ClassImplementCloseableWithoutThrowException;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.UserDefinedCarelessCleanupDog;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.UserDefinedCarelessCleanupWeather;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingIntegrationExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingJavaLogExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingLog4JExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingSelf4JExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheFirstOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheSecondOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheThirdOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithCatchRuntimeExceptionExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTry;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTryAtLastStatement;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTryAtMiddleStatement;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutCatchRightExceptionExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutStatementExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedmainProgramWithTryAtFirstStatement;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
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
		
		// 讀取測試檔案樣本內容
		javaFileToString = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		
		// 新增欲載入的library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/slf4j-api-1.5.0.jar");
		javaProjectMaker.addClasspathEntryToBuildPath(BuildPathSupport.getJUnit4ClasspathEntry(), null);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		
		// 根據測試檔案樣本內容建立新的檔案
		loadClass(NestedTryStatementExample.class);
		loadClass(CarelessCleanupExample.class);
		loadClass(ClassImplementCloseable.class);
		loadClass(ClassImplementCloseableWithoutThrowException.class);
		loadClass(ClassCanCloseButNotImplementCloseable.class);
		loadClass(UserDefinedCarelessCleanupDog.class);
		loadClass(UserDefinedCarelessCleanupWeather.class);
		loadClass(OverLoggingIntegrationExample.class);
		loadClass(OverLoggingJavaLogExample.class);
		loadClass(OverLoggingLog4JExample.class);
		loadClass(OverLoggingSelf4JExample.class);
		loadClass(OverLoggingTheFirstOrderClass.class);
		loadClass(OverLoggingTheSecondOrderClass.class);
		loadClass(OverLoggingTheThirdOrderClass.class);
		loadClass(UnprotectedMainProgramExample.class);
		loadClass(UnprotectedMainProgramWithCatchRuntimeExceptionExample.class);
		loadClass(UnprotectedMainProgramWithoutStatementExample.class);
		loadClass(UnprotectedMainProgramWithoutTryExample.class);
		loadClass(UnprotectedMainProgramWithTry.class);
		loadClass(UnprotectedmainProgramWithTryAtFirstStatement.class);
		loadClass(UnprotectedMainProgramWithTryAtLastStatement.class);
		loadClass(UnprotectedMainProgramWithTryAtMiddleStatement.class);
		loadClass(UnprotectedMainProgramWithoutCatchRightExceptionExample.class);

		InitailSetting();
		
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		reportModel = new ReportModel();
		reportModel.setProjectName(project.getName());
		reportModel.setBuildTime();
		reportBuilder = new ReportBuilder(project, reportModel);
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
	 * 全勾設定擋除了 Dummy & Empty 沒勾
	 * 以及自定義的部分也已經加入了
	 * 因為在別的 class 已經處理過了
	 * 很長的檔案路徑字串記得要改
	 */
	private void CreateAllSettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EMPTYCATCHBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYSTATEMENT, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.addCarelessCleanupPattern("ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupDog", isDetecting);
		smellSettings.addCarelessCleanupPattern("ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupWeather", isDetecting);
		
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_OVERLOGGING, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.addOverLoggingPattern("org.slf4j.Logger", isDetecting);
		
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	
	private void DisableAllSmellDetect() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_EMPTYCATCHBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYSTATEMENT, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_THROWNEXCEPTIONINFINALLYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);	
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_OVERLOGGING, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
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
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.addCarelessCleanupPattern("ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupDog", isDetecting);
		smellSettings.addCarelessCleanupPattern("ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupWeather", isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private void CreateOverloggingSettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_OVERLOGGING, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.addOverLoggingPattern("org.slf4j.Logger", isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private void CreateCarelessCleanupWithoutExtraRuleSettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addCarelessCleanupPattern("ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupDog", isDetecting);
		smellSettings.addCarelessCleanupPattern("ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupWeather", isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private void CreateCarelessCleanupWithoutUserDefinitionSettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private void CreateOverloggingWithoutExtraRuleSettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_OVERLOGGING, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addOverLoggingPattern("org.slf4j.Logger", isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private void CreateOverloggingWithoutUserDefinition() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_OVERLOGGING, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
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

	@Test
	public void testCountFileLOC() throws Exception {
		/** 正確路徑下的class file */
		Method countFileLOC = ReportBuilder.class.getDeclaredMethod("countFileLOC", String.class);
		countFileLOC.setAccessible(true);
		// 檢查測試專案檔案的行數
		assertEquals(537, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(NestedTryStatementExample.class, projectName)));
		assertEquals(737, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(CarelessCleanupExample.class, projectName)));
		assertEquals(199, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingJavaLogExample.class, projectName)));
		assertEquals(174, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingLog4JExample.class, projectName)));
		assertEquals(159, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingSelf4JExample.class, projectName)));
		assertEquals(55, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingTheFirstOrderClass.class, projectName)));
		assertEquals(55, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingTheSecondOrderClass.class, projectName)));
		assertEquals(52, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingTheThirdOrderClass.class, projectName)));
		/** 路徑不正確或者不存在的class file */
		assertEquals(0, countFileLOC.invoke(reportBuilder, "not/exist/example.java"));
		/** 副檔名不是java的文字檔案是可以被計算出行數的，這邊是因為沒有產生此檔案，故等於不存在的檔案 */
		assertEquals(0, countFileLOC.invoke(reportBuilder, "/ReportBuilderIntergrationTest/src/ntut/csie/filemaker/exceptionBadSmells/NestedTryStatementExample.txt"));
	}
	
	private void invokeAnalysis() throws Exception {
		Method getFilterSettings = ReportBuilder.class.getDeclaredMethod("getFilterSettings");
		Method analysisProject = ReportBuilder.class.getDeclaredMethod("analysisProject", IProject.class);
		getFilterSettings.setAccessible(true);
		analysisProject.setAccessible(true);
		getFilterSettings.invoke(reportBuilder);
		analysisProject.invoke(reportBuilder, project);
	}
	
	@Test
	public void testDummyHandlerAndEmptyCatchBlockBadSmellReport() throws Exception {
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_THROWN_EXCEPTION_IN_FINALLY_BLOCK) == 0);
		
		CreateDummyAndIgnoreSettings();
		
		invokeAnalysis();
		
		assertEquals(59, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(10, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_THROWN_EXCEPTION_IN_FINALLY_BLOCK));
		assertEquals(69, reportModel.getAllSmellSize());
	}
	
	@Test
	public void testOnlyNestedTryBadSmellReport() throws Exception {
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN) == 0);
		CreateNestedTrySettings();

		invokeAnalysis();
		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(27, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(27, reportModel.getAllSmellSize());
	}
	
	@Test
	public void testOnlyUnprotectedMainProgramBadSmellReport() throws Exception {
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN) == 0);
		CreateUnprotectedMainProgramSettings();

		invokeAnalysis();
		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		assertEquals(6, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(6, reportModel.getAllSmellSize());
	}
	
	@Test
	public void testOnlyCarelessCleanupBadSmellReport() throws Exception {
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN) == 0);
		CreateCarelessCleanupSettings();

		invokeAnalysis();
		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(38, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(38, reportModel.getAllSmellSize());
	}

	@Test
	public void testCreateOverloggingBadSmellReport() throws Exception {
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN) == 0);
		CreateOverloggingSettings();

		invokeAnalysis();
		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(26, reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(26, reportModel.getAllSmellSize());
	}
	
	@Test
	public void testOnlyCarelessCleanupWithoutExtraRuleBadSmellReport() throws Exception {
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN) == 0);
		CreateCarelessCleanupWithoutExtraRuleSettings();

		invokeAnalysis();
				
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(35, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(35, reportModel.getAllSmellSize());
	}
	
	@Test
	public void testOnlyCarelessCleanupWithoutUserDefinitionBadSmellReport() throws Exception {
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN) == 0);
		CreateCarelessCleanupWithoutUserDefinitionSettings();

		invokeAnalysis();
		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(31, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(31, reportModel.getAllSmellSize());
	}
	
	@Test
	public void testOnlyOverloggingWithoutExtraRuleBadSmellReport() throws Exception {
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN) == 0);
		CreateOverloggingWithoutExtraRuleSettings();

		invokeAnalysis();
		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(7, reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(7, reportModel.getAllSmellSize());
	}
	
	@Test
	public void testOnlyOverloggingWithoutUserDefinitioneBadSmellReport() throws Exception {
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN) == 0);
		CreateOverloggingWithoutUserDefinition();

		invokeAnalysis();
		
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(18, reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		assertEquals(0, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(18, reportModel.getAllSmellSize());
	}
	
	/**
	 * 設定檔全勾
	 * 正常測試情況
	 * 測試除了 DummyHandler & EmptyCatchBlock 以外的報表
	 * 以及相關資訊
	 * @throws Exception
	 */
	@Test
	public void testRemainBadSmellReport() throws Exception {
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING) == 0);
		assertTrue(reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN) == 0);
		CreateAllSettings();
		
		invokeAnalysis();
		
		assertEquals(0,reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(0,reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(38, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		assertEquals(27, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(26, reportModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING));
		assertEquals(6, reportModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN));
		assertEquals(145, reportModel.getTryCounter());
		assertEquals(156, reportModel.getCatchCounter());
		assertEquals(31, reportModel.getFinallyCounter());
		assertEquals(4, reportModel.getPackagesSize());
		assertEquals(projectName, reportModel.getProjectName());
		assertEquals(2196, reportModel.getTotalLine());
		assertEquals(97, reportModel.getAllSmellSize());
	}
}