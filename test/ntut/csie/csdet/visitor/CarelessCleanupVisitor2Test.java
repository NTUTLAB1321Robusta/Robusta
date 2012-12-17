package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassImplementCloseableWithoutThrowException;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassWithNotThrowingExceptionCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupDog;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupWeather;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CarelessCleanupVisitor2Test {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	CarelessCleanupVisitor2 carelessCleanupVisitor;
	SmellSettings smellSettings;

	@Before
	public void setUp() throws Exception {
		String testProjectName = "CarelessCleanupExampleProject";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String.read(CarelessCleanupExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				CarelessCleanupExample.class.getPackage().getName(),
				CarelessCleanupExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + CarelessCleanupExample.class.getPackage().getName() + ";" + String.format("%n")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(ClassWithNotThrowingExceptionCloseable.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassWithNotThrowingExceptionCloseable.class.getPackage().getName(),
				ClassWithNotThrowingExceptionCloseable.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + ClassWithNotThrowingExceptionCloseable.class.getPackage().getName() + ";" + System.getProperty("line.separator")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(ClassImplementCloseable.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassImplementCloseable.class.getPackage().getName(),
				ClassImplementCloseable.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + ClassImplementCloseable.class.getPackage().getName() + ";" + System.getProperty("line.separator")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		/* 測試使用者設定Pattern時候使用 */
		javaFile2String.read(UserDefinedCarelessCleanupWeather.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefinedCarelessCleanupWeather.class.getPackage().getName(),
				UserDefinedCarelessCleanupWeather.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefinedCarelessCleanupWeather.class.getPackage().getName() + ";" + System.getProperty("line.separator")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(UserDefinedCarelessCleanupDog.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefinedCarelessCleanupDog.class.getPackage().getName(),
				UserDefinedCarelessCleanupDog.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefinedCarelessCleanupDog.class.getPackage().getName() + ";" + System.getProperty("line.separator")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(ClassImplementCloseableWithoutThrowException.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassImplementCloseableWithoutThrowException.class.getPackage().getName(),
				ClassImplementCloseableWithoutThrowException.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + ClassImplementCloseableWithoutThrowException.class.getPackage().getName() + ";" + System.getProperty("line.separator")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		Path ccExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(CarelessCleanupExample.class, testProjectName));
		// Create AST to parse
		
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);
		// 建立XML
		CreateSettings();
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		carelessCleanupVisitor = new CarelessCleanupVisitor2(compilationUnit);
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
		File settingFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(settingFile.exists()) {
			assertTrue(settingFile.delete());
		}
	}
	
	// line 470 與495 in CarelessCleaupExample 不算是 careless cleanup
	
	@Test
	public void testGetCarelessCleanupListWithoutExtraRules() {
		/* 重新產生設定檔 */
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		// 因為Setup時候已經產出一個有ExtraRule的設定檔，現在要特地把ExtraRule移除掉來測試。
		smellSettings.removeExtraRule(
			SmellSettings.SMELL_CARELESSCLEANUP,
			SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		// 重新產生Visitor，使得設定值有存入CarelessCleaupVisitor
		carelessCleanupVisitor = new CarelessCleanupVisitor2(compilationUnit);
		
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				28, carelessCleanupSmellCount);
	}
	
	@Test
	public void testGetCarelessCleanupList() {
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				31, carelessCleanupSmellCount);
	}
	
	@Test
	public void testGetCarelessCleanupListWithUserDefiendLibs() throws Exception {	
		// 產生設定檔
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupWeather.class.getName() + ".*", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		// 重新產生Visitor，使得設定值有存入CarelessCleaupVisitor
		carelessCleanupVisitor = new CarelessCleanupVisitor2(compilationUnit);
		
		// 偵測結果
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				37, carelessCleanupSmellCount);
	}
	
	@Test
	public void testGetCarelessCleanupListWithUserDefiendFullQualifiedMethods() throws Exception {	
		// 產生設定檔
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupWeather.class.getName() + ".bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		// 重新產生Visitor，使得設定值有存入CarelessCleaupVisitor
		carelessCleanupVisitor = new CarelessCleanupVisitor2(compilationUnit);
		
		// 偵測結果
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				33, carelessCleanupSmellCount);
	}
	
	@Test
	public void testGetCarelessCleanupListWithUserDefiendMethods() throws Exception {	
		// 產生設定檔
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		// 重新產生Visitor，使得設定值有存入CarelessCleaupVisitor
		carelessCleanupVisitor = new CarelessCleanupVisitor2(compilationUnit);
		
		// 偵測結果
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				34, carelessCleanupSmellCount);
	}
	
	@Test
	public void testGetCarelessCleanupListWithUserDefiendOnlyMethods() throws Exception {	
		// 產生設定檔
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern("rain", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		// 重新產生Visitor，使得設定值有存入CarelessCleaupVisitor
		carelessCleanupVisitor = new CarelessCleanupVisitor2(compilationUnit);
		
		// 偵測結果
		int carelessCleanupSmellCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(carelessCleanupVisitor);
		compilationUnit.accept(carelessCleanupVisitor);
		if (carelessCleanupVisitor.getCarelessCleanupList() != null) {
			carelessCleanupSmellCount = carelessCleanupVisitor.getCarelessCleanupList().size();
		}
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor.getCarelessCleanupList()),
				33, carelessCleanupSmellCount);
	}
	
	/**
	 * 紀錄所有badSmell內容以及行號
	 * @param badSmellList
	 * @return
	 */
	private String colloectBadSmellListContent(List<MarkerInfo> badSmellList) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		for (int i = 0; i < badSmellList.size(); i++) {
			MarkerInfo m = badSmellList.get(i);
			sb.append(m.getLineNumber()).append("\t").append(m.getStatement()).append("\n");
		}
		return sb.toString();
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
