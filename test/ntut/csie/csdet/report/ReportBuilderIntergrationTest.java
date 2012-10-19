package ntut.csie.csdet.report;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.NestedTryStatementExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassImplementCloseableWithoutThrowException;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassWithNotThrowingExceptionCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupDog;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupWeather;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingIntegrationExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingJavaLogExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingLog4JExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingSelf4JExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheFirstOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheSecondOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheThirdOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTry;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTryAtLastStatement;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTryAtMiddleStatement;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithCatchRuntimeExceptionExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutStatementExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedmainProgramWithTryAtFirstStatement;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
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
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		
		// 根據測試檔案樣本內容建立新的檔案
		javaFileToString.read(NestedTryStatementExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				NestedTryStatementExample.class.getPackage().getName(),
				NestedTryStatementExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ NestedTryStatementExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(CarelessCleanupExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				CarelessCleanupExample.class.getPackage().getName(),
				CarelessCleanupExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ CarelessCleanupExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(ClassImplementCloseable.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassImplementCloseable.class.getPackage().getName(),
				ClassImplementCloseable.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ ClassImplementCloseable.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(ClassImplementCloseableWithoutThrowException.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassImplementCloseableWithoutThrowException.class.getPackage().getName(),
				ClassImplementCloseableWithoutThrowException.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ ClassImplementCloseableWithoutThrowException.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(ClassWithNotThrowingExceptionCloseable.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassWithNotThrowingExceptionCloseable.class.getPackage().getName(),
				ClassWithNotThrowingExceptionCloseable.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ ClassWithNotThrowingExceptionCloseable.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UserDefinedCarelessCleanupDog.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefinedCarelessCleanupDog.class.getPackage().getName(),
				UserDefinedCarelessCleanupDog.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UserDefinedCarelessCleanupDog.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UserDefinedCarelessCleanupWeather.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefinedCarelessCleanupWeather.class.getPackage().getName(),
				UserDefinedCarelessCleanupWeather.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UserDefinedCarelessCleanupWeather.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(OverLoggingIntegrationExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				OverLoggingIntegrationExample.class.getPackage().getName(),
				OverLoggingIntegrationExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ OverLoggingIntegrationExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(OverLoggingJavaLogExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				OverLoggingJavaLogExample.class.getPackage().getName(),
				OverLoggingJavaLogExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ OverLoggingJavaLogExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(OverLoggingLog4JExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				OverLoggingLog4JExample.class.getPackage().getName(),
				OverLoggingLog4JExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ OverLoggingLog4JExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(OverLoggingSelf4JExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				OverLoggingSelf4JExample.class.getPackage().getName(),
				OverLoggingSelf4JExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ OverLoggingSelf4JExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(OverLoggingTheFirstOrderClass.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				OverLoggingTheFirstOrderClass.class.getPackage().getName(),
				OverLoggingTheFirstOrderClass.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ OverLoggingTheFirstOrderClass.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(OverLoggingTheSecondOrderClass.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				OverLoggingTheSecondOrderClass.class.getPackage().getName(),
				OverLoggingTheSecondOrderClass.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ OverLoggingTheSecondOrderClass.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(OverLoggingTheThirdOrderClass.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				OverLoggingTheThirdOrderClass.class.getPackage().getName(),
				OverLoggingTheThirdOrderClass.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ OverLoggingTheThirdOrderClass.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UnprotectedMainProgramExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramExample.class.getPackage().getName(),
				UnprotectedMainProgramExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UnprotectedMainProgramExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UnprotectedMainProgramWithCatchRuntimeExceptionExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithCatchRuntimeExceptionExample.class.getPackage().getName(),
				UnprotectedMainProgramWithCatchRuntimeExceptionExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UnprotectedMainProgramWithCatchRuntimeExceptionExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UnprotectedMainProgramWithoutStatementExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutStatementExample.class.getPackage().getName(),
				UnprotectedMainProgramWithoutStatementExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UnprotectedMainProgramWithoutStatementExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UnprotectedMainProgramWithoutTryExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutTryExample.class.getPackage().getName(),
				UnprotectedMainProgramWithoutTryExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UnprotectedMainProgramWithoutTryExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UnprotectedMainProgramWithTry.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTry.class.getPackage().getName(),
				UnprotectedMainProgramWithTry.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UnprotectedMainProgramWithTry.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UnprotectedmainProgramWithTryAtFirstStatement.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedmainProgramWithTryAtFirstStatement.class.getPackage().getName(),
				UnprotectedmainProgramWithTryAtFirstStatement.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UnprotectedmainProgramWithTryAtFirstStatement.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UnprotectedMainProgramWithTryAtLastStatement.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTryAtLastStatement.class.getPackage().getName(),
				UnprotectedMainProgramWithTryAtLastStatement.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UnprotectedMainProgramWithTryAtLastStatement.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UnprotectedMainProgramWithTryAtMiddleStatement.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTryAtMiddleStatement.class.getPackage().getName(),
				UnprotectedMainProgramWithTryAtMiddleStatement.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UnprotectedMainProgramWithTryAtMiddleStatement.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		InitailSetting();
		
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		reportModel = new ReportModel();
		reportBuilder = new ReportBuilder(project, reportModel);
	}
	
	private void InitailSetting() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	/**
	 * 全勾設定擋除了 Dummy & Ignore 沒勾
	 * 以及自定義的部分也已經加入了
	 * 因為在別的 class 已經處理過了
	 * 很長的檔案路徑字串記得要改
	 */
	private void CreateAllSettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_IGNORECHECKEDEXCEPTION, SmellSettings.ATTRIBUTE_ISDETECTING, unDetcting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
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
	
	private void CreateDummyAndIgnoreSettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_IGNORECHECKEDEXCEPTION, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private void CreateNestedTrySettings() {
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
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
		assertEquals(495, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(NestedTryStatementExample.class, projectName)));
		assertEquals(565, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(CarelessCleanupExample.class, projectName)));
		assertEquals(17, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(ClassImplementCloseable.class, projectName)));
		assertEquals(16, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(ClassImplementCloseableWithoutThrowException.class, projectName)));
		assertEquals(28, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(ClassWithNotThrowingExceptionCloseable.class, projectName)));
		assertEquals(15, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(UserDefinedCarelessCleanupDog.class, projectName)));
		assertEquals(27, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(UserDefinedCarelessCleanupWeather.class, projectName)));
		assertEquals(11, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingIntegrationExample.class, projectName)));
		assertEquals(199, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingJavaLogExample.class, projectName)));
		assertEquals(174, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingLog4JExample.class, projectName)));
		assertEquals(159, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingSelf4JExample.class, projectName)));
		assertEquals(55, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingTheFirstOrderClass.class, projectName)));
		assertEquals(55, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingTheSecondOrderClass.class, projectName)));
		assertEquals(52, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(OverLoggingTheThirdOrderClass.class, projectName)));
		assertEquals(17, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramExample.class, projectName)));
		assertEquals(11, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithCatchRuntimeExceptionExample.class, projectName)));
		assertEquals(6, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithoutStatementExample.class, projectName)));
		assertEquals(8, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithoutTryExample.class, projectName)));
		assertEquals(21, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithTry.class, projectName)));
		assertEquals(13, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(UnprotectedmainProgramWithTryAtFirstStatement.class, projectName)));
		assertEquals(13, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithTryAtLastStatement.class, projectName)));
		assertEquals(13, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(UnprotectedMainProgramWithTryAtMiddleStatement.class, projectName)));
		/** 路徑不正確或者不存在的class file */
		assertEquals(0, countFileLOC.invoke(reportBuilder, "not/exist/example.java"));
	}
	
	/**
	 * DummyHandler & Ignore Report 在其他 Class 已經有進行測試了
	 * 在這邊只是確定設定檔不會影響本身的結果
	 * @throws Exception
	 */
	@Test
	public void testDummyHandlerBadSmellReport() throws Exception {
		CreateAllSettings();
		reportBuilder.run();
		assertTrue(reportModel.getDummyTotalSize() == 0);
		assertTrue(reportModel.getIgnoreTotalSize() == 0);
		CreateDummyAndIgnoreSettings();
		reportBuilder.run();
		assertTrue(reportModel.getDummyTotalSize() > 0);
		assertTrue(reportModel.getIgnoreTotalSize() > 0);
	}
	
	@Test
	public void testOnlyNestedTryBadSmellReport() throws Exception {
		assertTrue(reportModel.getDummyTotalSize() == 0);
		assertTrue(reportModel.getIgnoreTotalSize() == 0);
		assertTrue(reportModel.getCarelessCleanUpTotalSize() == 0);
		assertTrue(reportModel.getNestedTryTotalSize() == 0);
		assertTrue(reportModel.getOverLoggingTotalSize() == 0);
		assertTrue(reportModel.getUnMainTotalSize() == 0);
		CreateNestedTrySettings();
		reportBuilder.run();
		assertEquals(0, reportModel.getDummyTotalSize());
		assertEquals(0, reportModel.getIgnoreTotalSize());
		assertEquals(0, reportModel.getCarelessCleanUpTotalSize());
		assertEquals(25, reportModel.getNestedTryTotalSize());
		assertEquals(0, reportModel.getOverLoggingTotalSize());
		assertEquals(0, reportModel.getUnMainTotalSize());
		assertEquals(25, reportModel.getTotalSmellCount());
	}
	
	@Test
	public void testOnlyUnprotectedMainProgramBadSmellReport() throws Exception {
		assertTrue(reportModel.getDummyTotalSize() == 0);
		assertTrue(reportModel.getIgnoreTotalSize() == 0);
		assertTrue(reportModel.getCarelessCleanUpTotalSize() == 0);
		assertTrue(reportModel.getNestedTryTotalSize() == 0);
		assertTrue(reportModel.getOverLoggingTotalSize() == 0);
		assertTrue(reportModel.getUnMainTotalSize() == 0);
		CreateUnprotectedMainProgramSettings();
		reportBuilder.run();
		assertEquals(0, reportModel.getDummyTotalSize());
		assertEquals(0, reportModel.getIgnoreTotalSize());
		assertEquals(0, reportModel.getCarelessCleanUpTotalSize());
		assertEquals(0, reportModel.getNestedTryTotalSize());
		assertEquals(0, reportModel.getOverLoggingTotalSize());
		assertEquals(6, reportModel.getUnMainTotalSize());
		assertEquals(6, reportModel.getTotalSmellCount());
	}
	
	@Test
	public void testOnlyCarelessCleanupBadSmellReport() throws Exception {
		assertTrue(reportModel.getDummyTotalSize() == 0);
		assertTrue(reportModel.getIgnoreTotalSize() == 0);
		assertTrue(reportModel.getCarelessCleanUpTotalSize() == 0);
		assertTrue(reportModel.getNestedTryTotalSize() == 0);
		assertTrue(reportModel.getOverLoggingTotalSize() == 0);
		assertTrue(reportModel.getUnMainTotalSize() == 0);
		CreateCarelessCleanupSettings();
		reportBuilder.run();
		assertEquals(0, reportModel.getDummyTotalSize());
		assertEquals(0, reportModel.getIgnoreTotalSize());
		assertEquals(30, reportModel.getCarelessCleanUpTotalSize());
		assertEquals(0, reportModel.getNestedTryTotalSize());
		assertEquals(0, reportModel.getOverLoggingTotalSize());
		assertEquals(0, reportModel.getUnMainTotalSize());
		assertEquals(30, reportModel.getTotalSmellCount());
	}

	@Test
	public void testCreateOverloggingBadSmellReport() throws Exception {
		assertTrue(reportModel.getDummyTotalSize() == 0);
		assertTrue(reportModel.getIgnoreTotalSize() == 0);
		assertTrue(reportModel.getCarelessCleanUpTotalSize() == 0);
		assertTrue(reportModel.getNestedTryTotalSize() == 0);
		assertTrue(reportModel.getOverLoggingTotalSize() == 0);
		assertTrue(reportModel.getUnMainTotalSize() == 0);
		CreateOverloggingSettings();
		reportBuilder.run();
		assertEquals(0, reportModel.getDummyTotalSize());
		assertEquals(0, reportModel.getIgnoreTotalSize());
		assertEquals(0, reportModel.getCarelessCleanUpTotalSize());
		assertEquals(0, reportModel.getNestedTryTotalSize());
		assertEquals(26, reportModel.getOverLoggingTotalSize());
		assertEquals(0, reportModel.getUnMainTotalSize());
		assertEquals(26, reportModel.getTotalSmellCount());
	}
	
	@Test
	public void testOnlyCarelessCleanupWithoutExtraRuleBadSmellReport() throws Exception {
		assertTrue(reportModel.getDummyTotalSize() == 0);
		assertTrue(reportModel.getIgnoreTotalSize() == 0);
		assertTrue(reportModel.getCarelessCleanUpTotalSize() == 0);
		assertTrue(reportModel.getNestedTryTotalSize() == 0);
		assertTrue(reportModel.getOverLoggingTotalSize() == 0);
		assertTrue(reportModel.getUnMainTotalSize() == 0);
		CreateCarelessCleanupWithoutExtraRuleSettings();
		reportBuilder.run();
		assertEquals(0, reportModel.getDummyTotalSize());
		assertEquals(0, reportModel.getIgnoreTotalSize());
		assertEquals(28, reportModel.getCarelessCleanUpTotalSize());
		assertEquals(0, reportModel.getNestedTryTotalSize());
		assertEquals(0, reportModel.getOverLoggingTotalSize());
		assertEquals(0, reportModel.getUnMainTotalSize());
		assertEquals(28, reportModel.getTotalSmellCount());
	}
	
	@Test
	public void testOnlyCarelessCleanupWithoutUserDefinitionBadSmellReport() throws Exception {
		assertTrue(reportModel.getDummyTotalSize() == 0);
		assertTrue(reportModel.getIgnoreTotalSize() == 0);
		assertTrue(reportModel.getCarelessCleanUpTotalSize() == 0);
		assertTrue(reportModel.getNestedTryTotalSize() == 0);
		assertTrue(reportModel.getOverLoggingTotalSize() == 0);
		assertTrue(reportModel.getUnMainTotalSize() == 0);
		CreateCarelessCleanupWithoutUserDefinitionSettings();
		reportBuilder.run();
		assertEquals(0, reportModel.getDummyTotalSize());
		assertEquals(0, reportModel.getIgnoreTotalSize());
		assertEquals(26, reportModel.getCarelessCleanUpTotalSize());
		assertEquals(0, reportModel.getNestedTryTotalSize());
		assertEquals(0, reportModel.getOverLoggingTotalSize());
		assertEquals(0, reportModel.getUnMainTotalSize());
		assertEquals(26, reportModel.getTotalSmellCount());
	}
	
	@Test
	public void testOnlyOverloggingWithoutExtraRuleBadSmellReport() throws Exception {
		assertTrue(reportModel.getDummyTotalSize() == 0);
		assertTrue(reportModel.getIgnoreTotalSize() == 0);
		assertTrue(reportModel.getCarelessCleanUpTotalSize() == 0);
		assertTrue(reportModel.getNestedTryTotalSize() == 0);
		assertTrue(reportModel.getOverLoggingTotalSize() == 0);
		assertTrue(reportModel.getUnMainTotalSize() == 0);
		CreateOverloggingWithoutExtraRuleSettings();
		reportBuilder.run();
		assertEquals(0, reportModel.getDummyTotalSize());
		assertEquals(0, reportModel.getIgnoreTotalSize());
		assertEquals(0, reportModel.getCarelessCleanUpTotalSize());
		assertEquals(0, reportModel.getNestedTryTotalSize());
		assertEquals(7, reportModel.getOverLoggingTotalSize());
		assertEquals(0, reportModel.getUnMainTotalSize());
		assertEquals(7, reportModel.getTotalSmellCount());
	}
	
	@Test
	public void testOnlyOverloggingWithoutUserDefinitioneBadSmellReport() throws Exception {
		assertTrue(reportModel.getDummyTotalSize() == 0);
		assertTrue(reportModel.getIgnoreTotalSize() == 0);
		assertTrue(reportModel.getCarelessCleanUpTotalSize() == 0);
		assertTrue(reportModel.getNestedTryTotalSize() == 0);
		assertTrue(reportModel.getOverLoggingTotalSize() == 0);
		assertTrue(reportModel.getUnMainTotalSize() == 0);
		CreateOverloggingWithoutUserDefinition();
		reportBuilder.run();
		assertEquals(0, reportModel.getDummyTotalSize());
		assertEquals(0, reportModel.getIgnoreTotalSize());
		assertEquals(0, reportModel.getCarelessCleanUpTotalSize());
		assertEquals(0, reportModel.getNestedTryTotalSize());
		assertEquals(18, reportModel.getOverLoggingTotalSize());
		assertEquals(0, reportModel.getUnMainTotalSize());
		assertEquals(18, reportModel.getTotalSmellCount());
	}
	
	/**
	 * 設定檔全勾
	 * 正常測試情況
	 * 測試除了 DummyHandler & Ignore 以外的報表
	 * 以及相關資訊
	 * @throws Exception
	 */
	@Test
	public void testRemainBadSmellReport() throws Exception {
		assertTrue(reportModel.getDummyTotalSize() == 0);
		assertTrue(reportModel.getIgnoreTotalSize() == 0);
		assertTrue(reportModel.getCarelessCleanUpTotalSize() == 0);
		assertTrue(reportModel.getNestedTryTotalSize() == 0);
		assertTrue(reportModel.getOverLoggingTotalSize() == 0);
		assertTrue(reportModel.getUnMainTotalSize() == 0);
		CreateAllSettings();
		reportBuilder.run();
		assertEquals(0,reportModel.getDummyTotalSize());
		assertEquals(0,reportModel.getIgnoreTotalSize());
		assertEquals(30, reportModel.getCarelessCleanUpTotalSize());
		assertEquals(25, reportModel.getNestedTryTotalSize());
		assertEquals(26, reportModel.getOverLoggingTotalSize());
		assertEquals(6, reportModel.getUnMainTotalSize());
		assertEquals(130, reportModel.getTryCounter());
		assertEquals(151, reportModel.getCatchCounter());
		assertEquals(21, reportModel.getFinallyCounter());
		assertEquals(4, reportModel.getPackagesSize());
		assertEquals(projectName, reportModel.getProjectName());
		assertEquals(1970, reportModel.getTotalLine());
		assertEquals(87, reportModel.getTotalSmellCount());
	}
}