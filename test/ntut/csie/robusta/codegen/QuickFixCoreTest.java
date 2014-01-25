package ntut.csie.robusta.codegen;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndEmptyExample;
import ntut.csie.filemaker.exceptionBadSmells.UserDefineDummyHandlerFish;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.net.httpserver.Authenticator.Success;

public class QuickFixCoreTest {
	String projectNameString;
	String packageNameString;
	String classSimpleNameString;
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	SmellSettings smellSettings;
	Path path;

	public QuickFixCoreTest() {
		projectNameString = "QuickFixCoreTest";
		packageNameString = DummyAndEmptyExample.class.getPackage().getName();
		classSimpleNameString = DummyAndEmptyExample.class.getSimpleName();
	}
	
	@Before
	public void setUp() throws Exception {
		// 準備測試檔案樣本內容
		javaProjectMaker = new JavaProjectMaker(projectNameString);
		javaProjectMaker.setJREDefaultContainer();
		
		// 新增欲載入的library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		
		// 若example code中有robustness notation則有此行可以讓編譯通過
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.FOLDERNAME_LIB_JAR + JavaProjectMaker.FOLDERNAME_LIB_JAR);

		// 建立新的檔案DummyAndEmptyExample
		javaFile2String = new JavaFileToString();
		javaFile2String.read(DummyAndEmptyExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				DummyAndEmptyExample.class.getPackage().getName(),
				DummyAndEmptyExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + DummyAndEmptyExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		// 繼續建立測試用的UserDefineDummyHandlerFish
		javaFile2String.clear();
		javaFile2String.read(UserDefineDummyHandlerFish.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefineDummyHandlerFish.class.getPackage().getName(),
				UserDefineDummyHandlerFish.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefineDummyHandlerFish.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		path = new Path(PathUtils.getPathOfClassUnderSrcFolder(DummyAndEmptyExample.class, projectNameString));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// 如果xml檔案存在，則刪除之
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// 刪除專案
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testForGreenLight() {
	}
	
	@Ignore
	public void testPerformChange() throws Exception {
		fail("目前不知道如何在Unit Test中抓到EditorPart，所以未實作");
	}
	
	@Ignore
	public void testApplyChange() throws Exception {
		fail("目前不知道如何在Unit Test中抓到EditorPart，所以未實作");
	}
	
}
