package ntut.csie.testutility;

import static org.junit.Assert.assertTrue;

import java.io.File;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class TestEnvironmentBuilder {

	JavaFileToString javaFileToString;
	JavaProjectMaker javaProjectMaker;
	SmellSettings smellSettings;
	private String projectName;
	private IProject project;
	private IJavaProject javaProject;

	public TestEnvironmentBuilder() {
		this.projectName = "TestProject";
	}
	
	public TestEnvironmentBuilder(String projectName) {
		this.projectName = projectName;
	}
	
	public void createTestEnvironment() throws Exception {
		// Load the context from the test sample
		javaFileToString = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		
		// Add some library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/slf4j-api-1.5.0.jar");
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		javaProject = JavaCore.create(project);
		
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

	public void loadClass(Class clazz) throws Exception {
		javaFileToString.read(clazz, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(clazz.getPackage().getName(),
				clazz.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + clazz.getPackage().getName() + ";\n"
						+ javaFileToString.getFileContent());
		javaFileToString.clear();
	}

	public CompilationUnit getCompilationUnit(Class clazz) throws JavaModelException {
		IType type = javaProject.findType(clazz.getName());
		CompilationUnit unit = parse(type.getCompilationUnit());
		return unit;
	}

	public void cleanTestEnvironment() throws Exception {
		File settingFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(settingFile.exists()) {
			assertTrue(settingFile.delete());
		}
		javaProjectMaker.deleteProject();
	}

	public void accept(Class clazz, ASTVisitor visitor) throws JavaModelException {
		CompilationUnit unit = getCompilationUnit(clazz);
		unit.accept(visitor);
	}

	public SmellSettings getSmellSettings() {
		return smellSettings;
	}

}
