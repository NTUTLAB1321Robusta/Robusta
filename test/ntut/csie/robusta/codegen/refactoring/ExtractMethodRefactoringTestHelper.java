package ntut.csie.robusta.codegen.refactoring;

import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.thrown.ThrownExceptionInFinallyBlockVisitor;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.junit.buildpath.BuildPathSupport;

public class ExtractMethodRefactoringTestHelper {
	private IProject project;
	private String projectName = "TestProject";
	private JavaFileToString javaFileToString;
	private JavaProjectMaker javaProjectMaker;
	private IJavaProject javaProject;
	private SmellSettings smellSettings;
	public ExtractMethodRefactoringTestHelper(String projectName) throws Exception {
		super();
		this.projectName = projectName;
		javaFileToString = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		//Load libraries
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/slf4j-api-1.5.0.jar");
		javaProjectMaker.addClasspathEntryToBuildPath(BuildPathSupport.getJUnit4ClasspathEntry(), null);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		javaProject = JavaCore.create(getProject());
	}
	
	public void loadClass(Class clazz) throws Exception	{
		javaFileToString.read(clazz, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				clazz.getPackage().getName(),
				clazz.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ clazz.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();		
	}
	
	public void InitailSettingForOnlyTEIFB() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_THROWNEXCEPTIONINFINALLYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	public CompilationUnit getCompilationUnit(Class clazz) throws Exception {
		IType type = getJavaProject().findType(clazz.getName());
		CompilationUnit unit = parse(type.getCompilationUnit());
		return unit;
	}
	
	private CompilationUnit parse(ICompilationUnit unit) {
        ASTParser parser = ASTParser.newParser(AST.JLS3);
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        parser.setSource(unit);
        parser.setResolveBindings(true);
        return (CompilationUnit) parser.createAST(null); // parse
	}
	
	public List<MarkerInfo> getTEIFBList(CompilationUnit compilationUnit) throws Exception{
		ThrownExceptionInFinallyBlockVisitor visitor = new ThrownExceptionInFinallyBlockVisitor(compilationUnit);
		compilationUnit.accept(visitor);
		return visitor.getThrownInFinallyList();
	}
	
	public void cleanUp() throws Exception {
		javaProjectMaker.deleteProject();
	}

	public IJavaProject getJavaProject() {
		return javaProject;
	}

	public IProject getProject() {
		return project;
	}
}
