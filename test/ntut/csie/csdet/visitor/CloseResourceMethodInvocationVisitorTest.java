package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupBaseExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.CloseResourceMethodInvocationExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassCanCloseButNotImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassImplementCloseableWithoutThrowException;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ResourceCloser;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.UserDefinedCarelessCleanupClass;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.UserDefinedCarelessCleanupMethod;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		javaProject = JavaCore.create(project);
		
		loadClass(CloseResourceMethodInvocationExample.class);
		loadClass(ClassCanCloseButNotImplementCloseable.class);
		loadClass(ClassImplementCloseable.class);
		loadClass(UserDefinedCarelessCleanupMethod.class);
		loadClass(UserDefinedCarelessCleanupClass.class);
		loadClass(ClassImplementCloseableWithoutThrowException.class);
		loadClass(ResourceCloser.class);
		
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

	private void loadClass(Class clazz) throws Exception {
		javaFileToString.read(clazz, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(clazz.getPackage().getName(),
				clazz.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + clazz.getPackage().getName() + ";\n"
						+ javaFileToString.getFileContent());
		javaFileToString.clear();
	}

	private CompilationUnit getCompilationUnit(Class clazz) throws JavaModelException {
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

	@Ignore
	public void testGetCloseMethodInvocationListWithExtraRule() throws Exception {
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		collectCloseMethodInvocation(CloseResourceMethodInvocationExample.class);
		assertEquals(39, visitor.getCloseMethodInvocations().size());
	}
	
	@Test
	public void testGetCloseMethodInvocationListWithOutAnyExtraRule() throws Exception {
		collectCloseMethodInvocation(CloseResourceMethodInvocationExample.class);
		assertEquals(4, visitor.getCloseMethodInvocations().size());
	}
	
	@Ignore
	public void testGetCloseMethodInvocationListWithUserDefiendLibs() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupMethod.class.getName() + ".*", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		collectCloseMethodInvocation(null);
		assertEquals(42, visitor.getCloseMethodInvocations().size());
	}
	
	@Ignore
	public void testGetCloseMethodInvocationListWithUserDefinedMethods() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		collectCloseMethodInvocation(null);
		assertEquals(38, visitor.getCloseMethodInvocations().size());
	}
	
	@Ignore
	public void testGetCloseMethodInvocationListWithUserDefinedFullQualifiedMethods() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupMethod.class.getName() + ".bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		collectCloseMethodInvocation(null);
		assertEquals(37, visitor.getCloseMethodInvocations().size());
	}

	private void collectCloseMethodInvocation(Class clazz) throws JavaModelException {
		CompilationUnit unit = getCompilationUnit(clazz);
		visitor = new CloseResourceMethodInvocationVisitor(unit);
		unit.accept(visitor);
	}

}
