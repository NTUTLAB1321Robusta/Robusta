package ntut.csie.analyzer.over;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.over.OverLoggingVisitor;
import ntut.csie.csdet.data.MarkerInfo;
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
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OverLoggingVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit overLoggingJavaLogExampleUnit;
	SmellSettings smellSettings;
	OverLoggingVisitor overLoggingVisitor;
	String projectName;

	@Before
	public void setUp() throws Exception {
		projectName = "OverLoggingExampleProject";
		
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);

		javaProjectMaker.packageAgileExceptionClassesToJarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/slf4j-api-1.5.0.jar");
		javaProjectMaker.setJREDefaultContainer();
		
		javaFile2String.read(OverLoggingIntegrationExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingIntegrationExample.class.getPackage().getName(),
										OverLoggingIntegrationExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingIntegrationExample.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingJavaLogExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingJavaLogExample.class.getPackage().getName(),
										OverLoggingJavaLogExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingJavaLogExample.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingLog4JExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingLog4JExample.class.getPackage().getName(),
										OverLoggingLog4JExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingLog4JExample.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingSelf4JExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingSelf4JExample.class.getPackage().getName(),
										OverLoggingSelf4JExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingSelf4JExample.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingTheFirstOrderClass.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingTheFirstOrderClass.class.getPackage().getName(),
										OverLoggingTheFirstOrderClass.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingTheFirstOrderClass.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingTheSecondOrderClass.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingTheSecondOrderClass.class.getPackage().getName(),
										OverLoggingTheSecondOrderClass.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingTheSecondOrderClass.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(OverLoggingTheThirdOrderClass.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(OverLoggingTheThirdOrderClass.class.getPackage().getName(),
										OverLoggingTheThirdOrderClass.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
										"package " + OverLoggingTheThirdOrderClass.class.getPackage().getName()
										+ ";\n" + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		CreateSettings();
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(OverLoggingJavaLogExample.class, projectName));
		
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);

		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);

		overLoggingJavaLogExampleUnit = (CompilationUnit) parser.createAST(null); 
		overLoggingJavaLogExampleUnit.recordModifications();
		
		path = new Path(PathUtils.getPathOfClassUnderSrcFolder(OverLoggingLog4JExample.class, projectName));
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testVisit() {
		/* OverLoggingJavaLogExample */
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		overLoggingJavaLogExampleUnit.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		List<MarkerInfo> markerInfoList = new ArrayList<MarkerInfo>();
		
		for(MethodDeclaration node : methodList) {
			OverLoggingVisitor olVisitor = new OverLoggingVisitor(overLoggingJavaLogExampleUnit);
			node.accept(olVisitor);
			markerInfoList.addAll(olVisitor.getBadSmellCollected());
		}
		
		
		assertEquals(6, markerInfoList.size());
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}