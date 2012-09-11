package ntut.csie.csdet.refactor;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.OverLoggingVisitor;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingIntegrationExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingJavaLogExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingLog4JExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingSelf4JExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheFirstOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheSecondOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheThirdOrderClass;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OverLoggingRefactorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit overLoggingJavaLogExampleUnit;
	CompilationUnit overLoggingLog4JExampleUnit;
	CompilationUnit overLoggingSelf4JExampleUnit;
	SmellSettings smellSettings;
	OverLoggingVisitor overLoggingVisitor;
	OverLoggingRefactor overLoggingRefactor;
	String projectName;
	
	@Before
	public void setUp() throws Exception {
		projectName = "OverLoggingExampleProject";
		// 讀取測試檔案樣本內容
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		// 新增欲載入的library
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/lib/RL.jar");
		javaProjectMaker.addJarFromProjectToBuildPath("lib/log4j-1.2.15.jar");
		javaProjectMaker.addJarFromProjectToBuildPath("lib/slf4j-api-1.5.0.jar");
		javaProjectMaker.setJREDefaultContainer();
		
		// 根據測試檔案樣本內容建立新的檔案
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
		
		// 建立XML
		CreateSettings();
		
		Path path = new Path(	projectName + "/" + JavaProjectMaker.FOLDERNAME_SOURCE + "/" +
								OverLoggingJavaLogExample.class.getPackage().getName().replace(".", "/") + 
								"/" + OverLoggingJavaLogExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION);
		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		overLoggingJavaLogExampleUnit = (CompilationUnit) parser.createAST(null); 
		overLoggingJavaLogExampleUnit.recordModifications();
		
		path = new Path(	projectName + "/" + JavaProjectMaker.FOLDERNAME_SOURCE + "/" +
							OverLoggingLog4JExample.class.getPackage().getName().replace(".", "/") + 
							"/" + OverLoggingLog4JExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION);
		
		// Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		overLoggingLog4JExampleUnit = (CompilationUnit) parser.createAST(null); 
		overLoggingLog4JExampleUnit.recordModifications();
		
		path = new Path(	projectName + "/" + JavaProjectMaker.FOLDERNAME_SOURCE + "/" +
							OverLoggingSelf4JExample.class.getPackage().getName().replace(".", "/") + 
							"/" + OverLoggingSelf4JExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION);

		// Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		overLoggingSelf4JExampleUnit = (CompilationUnit) parser.createAST(null); 
		overLoggingSelf4JExampleUnit.recordModifications();
		overLoggingRefactor = new OverLoggingRefactor();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		// 如果xml檔案存在，則刪除之
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// 刪除專案
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testIsExistence() throws Exception {
		/** size of methodNodeList is 0 */
		Method isExistence = OverLoggingRefactor.class.getDeclaredMethod("isExistence", ASTNode.class);
		isExistence.setAccessible(true);
		// test target
		assertFalse((Boolean)isExistence.invoke(overLoggingRefactor, ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theThirdOrderInTheSameClassWithJavaLog")));
		
		/** size of methodNodeList > 0 and there is no duplicate method */
		Field methodNodeList = OverLoggingRefactor.class.getDeclaredField("methodNodeList");
		methodNodeList.setAccessible(true);
		List<ASTNode> methodList = (List)methodNodeList.get(overLoggingRefactor);
		methodList.add(ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theThirdOrderInTheSameClassWithJavaLog"));
		// check precondition
		assertEquals(1, methodList.size());
		// test target
		assertFalse((Boolean)isExistence.invoke(overLoggingRefactor, ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theSecondOrderInTheSameClassWithJavaLog")));
		
		/** size of methodNodeList > 0 and there are duplicate methods */
		// test target
		assertTrue((Boolean)isExistence.invoke(overLoggingRefactor, ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theThirdOrderInTheSameClassWithJavaLog")));
	}
	
	@Test
	public void testAddFixList() throws Exception {
		/** size of unitList is 0 */
		Field unitList = OverLoggingRefactor.class.getDeclaredField("unitList");
		unitList.setAccessible(true);
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(overLoggingRefactor, overLoggingJavaLogExampleUnit);
		List<CompilationUnit> cuList = (List)unitList.get(overLoggingRefactor);
		// check precondition
		assertEquals(0, cuList.size());
		// test target
		Method addFixList = OverLoggingRefactor.class.getDeclaredMethod("addFixList");
		addFixList.setAccessible(true);
		addFixList.invoke(overLoggingRefactor);
		// check postcondition
		assertEquals(1, cuList.size());
		
		/** there are duplicate methods */
		// check precondition
		assertEquals(1, cuList.size());
		// test target
		addFixList.invoke(overLoggingRefactor);
		// check postcondition
		assertEquals(1, cuList.size());
		
		/** there is no duplicate methods */
		// check precondition
		assertEquals(1, cuList.size());
		actRoot.set(overLoggingRefactor, overLoggingLog4JExampleUnit);
		// test target
		addFixList.invoke(overLoggingRefactor);
		// check postcondition
		assertEquals(2, cuList.size());
	}
	
	@Test
	public void testGetIsKeepTrace() throws Exception {
		Method getIsKeepTrace = OverLoggingRefactor.class.getDeclaredMethod("getIsKeepTrace", IMethod.class, IMethod.class);
		getIsKeepTrace.setAccessible(true);
		IMethod method = (IMethod)ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theThirdOrderInTheSameClassWithJavaLog").resolveBinding().getJavaElement();
		// test target 第一個method目前沒有任何意義，只是為了符合參數規定，但裡面實際沒有功用，所以才兩個傳一樣的參數
		assertTrue((Boolean)getIsKeepTrace.invoke(overLoggingRefactor, method, method));
		
		method = (IMethod)ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theSecondOrderInTheSameClassWithJavaLog").resolveBinding().getJavaElement();
		// test target
		assertTrue((Boolean)getIsKeepTrace.invoke(overLoggingRefactor, method, method));
	}
	
	@Test
	public void testDeleteCatchStatement() {
		
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
