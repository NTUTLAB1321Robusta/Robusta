package ntut.csie.csdet.report;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.analyzer.CommonExample;
import ntut.csie.analyzer.UserDefineDummyHandlerFish;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ReportBuilderTest {
	JavaFileToString javaFileToString;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	ReportBuilder reportBuilder;
	BadSmellDataStorage badSmellDataStorage;
	IProject project;
	String projectName;
	SmellSettings smellSettings;
	
	public ReportBuilderTest() {
		projectName = "DummyHandlerTest";
	}

	@Before
	public void setUp() throws Exception {
		
		javaFileToString = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();

		javaProjectMaker
				.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR
						+ "/log4j-1.2.15.jar");
		javaProjectMaker.packageAgileExceptionClassesToJarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);

		javaFileToString.read(CommonExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				CommonExample.class.getPackage().getName(),
				CommonExample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ CommonExample.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		javaFileToString.read(UserDefineDummyHandlerFish.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefineDummyHandlerFish.class.getPackage().getName(),
				UserDefineDummyHandlerFish.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ UserDefineDummyHandlerFish.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		
		CreateSettings();
		
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		reportBuilder = new ReportBuilder(project, new NullProgressMonitor());

		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(CommonExample.class, projectName));
		
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
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
	public void testCountFileLOC() throws Exception {
		
		/** correct class file path*/
		Method countFileLOC = ReportBuilder.class.getDeclaredMethod("countFileLOC", String.class);
		countFileLOC.setAccessible(true);
		// LOC = line of code
		assertEquals(287, countFileLOC.invoke(reportBuilder, "/" + PathUtils.getPathOfClassUnderSrcFolder(CommonExample.class, projectName)));
		/** wrong class file path*/
		assertEquals(0, countFileLOC.invoke(reportBuilder, "not/exist/example.java"));
	}
	
	@Test
	public void testGetSourcePaths() throws Exception {
		IJavaProject javaPrj = JavaCore.create(project);
		IPackageFragmentRoot[] roots = javaPrj.getAllPackageFragmentRoots();
		
		// precondition
		for(int i = 0; i < roots.length; i++) {
			if(i == roots.length - 1)
				assertEquals(JavaProjectMaker.FOLDERNAME_SOURCE, roots[i].getElementName());
			else
				assertTrue(roots[i].getPath().toString().endsWith(".jar"));
		}
		
		// begin to test
		Method getSourcePaths = ReportBuilder.class.getDeclaredMethod("getSourcePaths", IJavaProject.class);
		getSourcePaths.setAccessible(true);
		List<IPackageFragmentRoot> srcPaths = (List)getSourcePaths.invoke(reportBuilder, javaPrj);
		// Assert that there is get only one src folder(others are jars)
		assertEquals(1, srcPaths.size());
		assertEquals("F/DummyHandlerTest/src", srcPaths.get(0).getUnderlyingResource().toString());
	}
	
	
	
	@Test
	public void testSetSmellInfo() throws Exception {
		Method setSmellInfo = ReportBuilder.class.getDeclaredMethod("setSmellInfo", ICompilationUnit.class, PackageModel.class, String.class);
		setSmellInfo.setAccessible(true);
		
		Field model = ReportBuilder.class.getDeclaredField("model");
		model.setAccessible(true);
		ReportModel reportModel = (ReportModel)model.get(reportBuilder); 
		
		IJavaProject javaPrj = JavaCore.create(project);
		List<IPackageFragmentRoot> root = reportBuilder.getSourcePaths(javaPrj);
		for(int i = 0; i < root.size(); i++) {
			IJavaElement[] packages = root.get(i).getChildren();
			for(IJavaElement iJavaElement : packages) {
				if (iJavaElement.getElementType() == IJavaElement.PACKAGE_FRAGMENT) {
					IPackageFragment iPackageFgt = (IPackageFragment) iJavaElement;
					ICompilationUnit[] compilationUnits = iPackageFgt.getCompilationUnits();
					for(int j = 0; j < compilationUnits.length; j++) {
						PackageModel newPackageModel = new PackageModel();
						newPackageModel.setPackageName(iPackageFgt.getElementName());
						setSmellInfo.invoke(reportBuilder, compilationUnits[j], newPackageModel, iPackageFgt.getPath().toString());
						reportModel.addPackageModel(newPackageModel);
					}
				}
			}
		}
		assertEquals(25, reportModel.getTryCounter());
		assertEquals(25, reportModel.getCatchCounter());
		assertEquals(2, reportModel.getFinallyCounter());
		assertEquals(5, reportModel.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP));
		// 14 dummy handler in methods and 1 dummy handler in an initializer
		assertEquals(15, reportModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER));
		assertEquals(1, reportModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK));
		assertEquals(3, reportModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT));
		assertEquals(24, reportModel.getAllSmellSize());
	}
	
	@Test
	public void testAnalysisProject() throws Exception {
		Method analysisProject = ReportBuilder.class.getDeclaredMethod("analysisProject", IProject.class);
		analysisProject.setAccessible(true);
		
		Field model = ReportBuilder.class.getDeclaredField("model");
		model.setAccessible(true);
		ReportModel reportModel = (ReportModel)model.get(reportBuilder);
		analysisProject.invoke(reportBuilder, project);
		
		assertEquals(1, reportModel.getPackagesSize());
	}
	

	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}