package ntut.csie.analyzer.empty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import ntut.csie.analyzer.UserDefineDummyHandlerFish;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EmptyCatchBlockVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	EmptyCatchBlockVisitor emptyCatchBlockVisitor;
	
	public EmptyCatchBlockVisitorTest() {
	}
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "EmptyCatchBlockTest";
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();
		
		// 新增欲載入的library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");

		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String = new JavaFileToString();
		javaFile2String.read(EmptyCatchBlockExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				EmptyCatchBlockExample.class.getPackage().getName(),
				EmptyCatchBlockExample.class.getSimpleName() +  JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + EmptyCatchBlockExample.class.getPackage().getName() + ";\n"
						+ javaFile2String.getFileContent());

		// 繼續建立測試用的UserDefineDummyHandlerFish
		javaFile2String.clear();
		javaFile2String.read(UserDefineDummyHandlerFish.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefineDummyHandlerFish.class.getPackage().getName(),
				UserDefineDummyHandlerFish.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefineDummyHandlerFish.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(EmptyCatchBlockExample.class, testProjectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		File smellSettingsFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(smellSettingsFile.exists()) {
			smellSettingsFile.delete();
		}
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testVisitNode_withSettingFileAndIsDetectingFalse() {
		int emptyCatchSmellCount = 0;
		SmellSettings smellSetting = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSetting.setSmellTypeAttribute(SmellSettings.SMELL_EMPTYCATCHBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, false);
		smellSetting.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		assertTrue(new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH).exists());
		emptyCatchBlockVisitor = new EmptyCatchBlockVisitor(compilationUnit);
		compilationUnit.accept(emptyCatchBlockVisitor);
		if(emptyCatchBlockVisitor.getEmptyCatchList() != null)
			emptyCatchSmellCount = emptyCatchBlockVisitor.getEmptyCatchList().size();
		
		// 驗證總共抓到幾個bad smell
		assertEquals(0, emptyCatchSmellCount);
	}
	
	@Test
	public void testVisitNode_withSettingFileAndIsDetectingTrue() {
		int emptySmellCount = 0;
		SmellSettings smellSetting = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSetting.setSmellTypeAttribute(SmellSettings.SMELL_EMPTYCATCHBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, true);
		smellSetting.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		assertTrue(new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH).exists());
		emptyCatchBlockVisitor = new EmptyCatchBlockVisitor(compilationUnit);
		compilationUnit.accept(emptyCatchBlockVisitor);
		if(emptyCatchBlockVisitor.getEmptyCatchList() != null)
			emptySmellCount = emptyCatchBlockVisitor.getEmptyCatchList().size();
		
		// 驗證總共抓到幾個bad smell
		assertEquals(7, emptySmellCount);
	}

	@Test
	public void testVisitNode_withoutSettingFile() {
		int emptyCatchSmellCount = 0;
		emptyCatchBlockVisitor = new EmptyCatchBlockVisitor(compilationUnit);
		compilationUnit.accept(emptyCatchBlockVisitor);
		if(emptyCatchBlockVisitor.getEmptyCatchList() != null)
			emptyCatchSmellCount = emptyCatchBlockVisitor.getEmptyCatchList().size();
		
		// 驗證總共抓到幾個bad smell
		assertEquals(7, emptyCatchSmellCount);
	}
}
