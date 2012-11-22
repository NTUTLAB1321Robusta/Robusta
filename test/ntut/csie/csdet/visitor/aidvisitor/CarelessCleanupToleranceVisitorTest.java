package ntut.csie.csdet.visitor.aidvisitor;


import static org.junit.Assert.*;

import java.io.File;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.tolerance.CarelessCleanupToleranceExample;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CarelessCleanupToleranceVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	CarelessCleanupToleranceVisitor carelessCleanupToleranceVisitor;
	SmellSettings smellSettings;
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "CarelessCleanupToleranceExampleProject";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String.read(CarelessCleanupToleranceExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				CarelessCleanupToleranceExample.class.getPackage().getName(),
				CarelessCleanupToleranceExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + CarelessCleanupToleranceExample.class.getPackage().getName() + ";%n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		Path ccExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(CarelessCleanupToleranceExample.class, testProjectName));
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
	public void test1stMethodDeclaration() throws Exception {
		carelessCleanupToleranceVisitor = new CarelessCleanupToleranceVisitor();
		MethodDeclaration md = (MethodDeclaration) NodeFinder.perform(compilationUnit, 280, 368);
		md.accept(carelessCleanupToleranceVisitor);
		assertFalse(carelessCleanupToleranceVisitor.isTolerable());
	}
	
	@Test
	public void test2ndMethodDeclaration() throws Exception {
		carelessCleanupToleranceVisitor = new CarelessCleanupToleranceVisitor();
		MethodDeclaration md = (MethodDeclaration) NodeFinder.perform(compilationUnit, 654, 354);
		md.accept(carelessCleanupToleranceVisitor);
		assertFalse(carelessCleanupToleranceVisitor.isTolerable());
	}
	
	@Test
	public void test3rdMethodDeclaration() throws Exception {
		carelessCleanupToleranceVisitor = new CarelessCleanupToleranceVisitor();
		MethodDeclaration md = (MethodDeclaration) NodeFinder.perform(compilationUnit, 1014, 181);
		md.accept(carelessCleanupToleranceVisitor);
		assertTrue(carelessCleanupToleranceVisitor.isTolerable());
	}
	
	@Test
	public void test4thMethodDeclaration() throws Exception {
		carelessCleanupToleranceVisitor = new CarelessCleanupToleranceVisitor();
		MethodDeclaration md = (MethodDeclaration) NodeFinder.perform(compilationUnit, 1201, 181);
		md.accept(carelessCleanupToleranceVisitor);
		assertTrue(carelessCleanupToleranceVisitor.isTolerable());
	}
	
	@Test
	public void test5thMethodDeclaration() throws Exception {
		carelessCleanupToleranceVisitor = new CarelessCleanupToleranceVisitor();
		MethodDeclaration md = (MethodDeclaration) NodeFinder.perform(compilationUnit, 1388, 173);
		md.accept(carelessCleanupToleranceVisitor);
		assertTrue(carelessCleanupToleranceVisitor.isTolerable());
	}
	
	@Test
	public void test6thMethodDeclaration() throws Exception {
		carelessCleanupToleranceVisitor = new CarelessCleanupToleranceVisitor();
		MethodDeclaration md = (MethodDeclaration) NodeFinder.perform(compilationUnit, 1567, 199);
		md.accept(carelessCleanupToleranceVisitor);
		assertFalse(carelessCleanupToleranceVisitor.isTolerable());
	}
	
	@Test
	public void test7thMethodDeclaration() throws Exception {
		carelessCleanupToleranceVisitor = new CarelessCleanupToleranceVisitor();
		MethodDeclaration md = (MethodDeclaration) NodeFinder.perform(compilationUnit, 1772, 229);
		md.accept(carelessCleanupToleranceVisitor);
		assertFalse(carelessCleanupToleranceVisitor.isTolerable());
	}
	
	@Test
	public void test8thMethodDeclaration() throws Exception {
		carelessCleanupToleranceVisitor = new CarelessCleanupToleranceVisitor();
		MethodDeclaration md = (MethodDeclaration) NodeFinder.perform(compilationUnit, 2007, 193);
		md.accept(carelessCleanupToleranceVisitor);
		assertFalse(carelessCleanupToleranceVisitor.isTolerable());
	}

	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
