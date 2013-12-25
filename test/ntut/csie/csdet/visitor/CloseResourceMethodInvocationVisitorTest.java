package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.ClassCanCloseButNotImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.ClassImplementCloseableWithoutThrowException;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.UserDefinedCarelessCleanupDog;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.UserDefinedCarelessCleanupWeather;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CloseResourceMethodInvocationVisitorTest {
	JavaFileToString javaFileToString;
	JavaProjectMaker javaProjectMaker;
	SmellSettings smellSettings;
	private String projectName = "testProject";
	private IProject project;
	private IJavaProject javaProject;
	private CloseResourceMethodInvocationVisitor visitor;
	
	@Before
	public void setUp() throws Exception {
		// 讀取測試檔案樣本內容
		javaFileToString = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName );
		javaProjectMaker.setJREDefaultContainer();
		
		// 新增欲載入的library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/slf4j-api-1.5.0.jar");
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		javaProject = JavaCore.create(project);
		
		loadClass(CarelessCleanupExample.class);
		loadClass(ClassCanCloseButNotImplementCloseable.class);
		loadClass(ClassImplementCloseable.class);
		loadClass(UserDefinedCarelessCleanupWeather.class);
		loadClass(UserDefinedCarelessCleanupDog.class);
		loadClass(ClassImplementCloseableWithoutThrowException.class);
		
		InitailSetting();
	}
	
	private void InitailSetting() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.ATTRIBUTE_ISDETECTING, true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
	}
	
	private void loadClass(Class clazz) throws Exception
	{
		javaFileToString.read(clazz, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				clazz.getPackage().getName(),
				clazz.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ clazz.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();		
	}
	
	private CompilationUnit getCompilationUnit(Class clazz) throws Exception {
		IType type = javaProject.findType(clazz.getName());
		CompilationUnit unit = parse(type.getCompilationUnit());
		return unit;
	}

	@After
	public void tearDown() throws Exception {
		File settingFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(settingFile.exists()) {
			assertTrue(settingFile.delete());
		}
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testGetCloseMethodInvocationList_WithExtraRule_Return35Item() throws Exception {
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		collectCloseMethodInvocation();
		assertEquals(35, visitor.getCloseMethodInvocationMap().size());
		assertEquals(39, visitor.getCloseMethodInvocationList().size());
	}
	
	@Test
	public void testGetCloseMethodInvocationListWithOutExtraRule() throws Exception {
		collectCloseMethodInvocation();
		assertEquals(32, visitor.getCloseMethodInvocationMap().size());
		assertEquals(35, visitor.getCloseMethodInvocationList().size());
	}
	
	@Test
	public void testGetCloseMethodInvocationListWithUserDefiendLibs() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupWeather.class.getName() + ".*", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		collectCloseMethodInvocation();
		assertEquals(34, visitor.getCloseMethodInvocationMap().size());
		assertEquals(42, visitor.getCloseMethodInvocationList().size());
	}
	
	@Test
	public void testGetCloseMethodInvocationListWithUserDefinedMethods() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		collectCloseMethodInvocation();
		assertEquals(34, visitor.getCloseMethodInvocationMap().size());
		assertEquals(38, visitor.getCloseMethodInvocationList().size());
	}
	
	@Test
	public void testGetCloseMethodInvocationListWithUserDefinedFullQualifiedMethods() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupWeather.class.getName() + ".bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		collectCloseMethodInvocation();
		assertEquals(34, visitor.getCloseMethodInvocationMap().size());
		assertEquals(37, visitor.getCloseMethodInvocationList().size());
	}
	
	private void collectCloseMethodInvocation() throws Exception
	{
		CompilationUnit unit = getCompilationUnit(CarelessCleanupExample.class);
		visitor = new CloseResourceMethodInvocationVisitor(unit);
		unit.accept(visitor);
	}

}
