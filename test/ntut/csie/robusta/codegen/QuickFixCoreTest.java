package ntut.csie.robusta.codegen;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import ntut.csie.analyzer.CommonExample;
import ntut.csie.analyzer.UserDefineDummyHandlerFish;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.util.PathUtils;

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
		packageNameString = CommonExample.class.getPackage().getName();
		classSimpleNameString = CommonExample.class.getSimpleName();
	}
	
	@Before
	public void setUp() throws Exception {
		javaProjectMaker = new JavaProjectMaker(projectNameString);
		javaProjectMaker.setJREDefaultContainer();
		
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		
		javaProjectMaker.packageAgileExceptionClassesToJarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.FOLDERNAME_LIB_JAR + JavaProjectMaker.FOLDERNAME_LIB_JAR);

		javaFile2String = new JavaFileToString();
		javaFile2String.read(CommonExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				CommonExample.class.getPackage().getName(),
				CommonExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + CommonExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		javaFile2String.clear();
		javaFile2String.read(UserDefineDummyHandlerFish.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefineDummyHandlerFish.class.getPackage().getName(),
				UserDefineDummyHandlerFish.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefineDummyHandlerFish.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		path = new Path(PathUtils.getPathOfClassUnderSrcFolder(CommonExample.class, projectNameString));
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testForGreenLight() {
	}
	
	@Ignore
	public void testPerformChange() throws Exception {
		fail("we don't know how to get editor part during testing, so this feature has not been implemented yet");
	}
	
	@Ignore
	public void testApplyChange() throws Exception {
		fail("we don't know how to get editor part during testing, so this feature has not been implemented yet");
	}
	
}
