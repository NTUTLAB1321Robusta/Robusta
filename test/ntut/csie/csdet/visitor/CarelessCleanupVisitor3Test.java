package ntut.csie.csdet.visitor;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupWithExtraRules;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.FileUtils;
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

public class CarelessCleanupVisitor3Test {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	CarelessCleanupVisitor2 carelessCleanupVisitor;
	SmellSettings smellSettings;
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "CarelessCleanupWithExtraRulesExampleProject";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String.read(CarelessCleanupWithExtraRules.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				CarelessCleanupWithExtraRules.class.getPackage().getName(),
				CarelessCleanupWithExtraRules.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + CarelessCleanupWithExtraRules.class.getPackage().getName() + ";" + String.format("%n")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		/* 測試使用者設定Pattern時候使用 */
		javaFile2String.read(FileUtils.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				FileUtils.class.getPackage().getName(),
				FileUtils.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + FileUtils.class.getPackage().getName() + ";" + System.getProperty("line.separator")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		Path ccExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(CarelessCleanupWithExtraRules.class, testProjectName));
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
	
	@Test
	public void testSettingsWithExtraRules() throws Exception {
		compilationUnit.accept(carelessCleanupVisitor);
		assertEquals(
				colloectBadSmellListContent(carelessCleanupVisitor
						.getCarelessCleanupList()),
				7, carelessCleanupVisitor.getCarelessCleanupList().size());
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
		smellSettings.addCarelessCleanupPattern("*.close", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
