package ntut.csie.csdet.refactor;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.OverLoggingVisitor;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.RuntimeEnvironmentProjectReader;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingIntegrationExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingJavaLogExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingLog4JExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingSelf4JExample;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheFirstOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheSecondOrderClass;
import ntut.csie.filemaker.exceptionBadSmells.OverLogging.OverLoggingTheThirdOrderClass;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
		javaProjectMaker.addJarFromTestProjectToBuildPath("/lib/Tag.jar");
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
	public void testIsExistenceWithMethodNodeListSizeIsZero() throws Exception {
		/** size of methodNodeList is 0 */
		Method isExistence = OverLoggingRefactor.class.getDeclaredMethod("isExistence", MethodDeclaration.class);
		isExistence.setAccessible(true);
		// test target
		assertFalse((Boolean)isExistence.invoke(overLoggingRefactor, ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theThirdOrderInTheSameClassWithJavaLog")));
	}
	
	@Test
	public void testIsExistenceWithNoDuplicateMethod() throws Exception {
		/** size of methodNodeList > 0 and there is no duplicate method */
		Method isExistence = OverLoggingRefactor.class.getDeclaredMethod("isExistence", MethodDeclaration.class);
		isExistence.setAccessible(true);
		Field methodNodeList = OverLoggingRefactor.class.getDeclaredField("methodNodeList");
		methodNodeList.setAccessible(true);
		List<MethodDeclaration> methodList = (List)methodNodeList.get(overLoggingRefactor);
		methodList.add(ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theThirdOrderInTheSameClassWithJavaLog"));
		// check precondition
		assertEquals(1, methodList.size());
		// test target
		assertFalse((Boolean)isExistence.invoke(overLoggingRefactor, ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theSecondOrderInTheSameClassWithJavaLog")));
	}
	
	@Test
	public void testIsExistenceWithDuplicateMethods() throws Exception {
		/** size of methodNodeList > 0 and there are duplicate methods */
		Field methodNodeList = OverLoggingRefactor.class.getDeclaredField("methodNodeList");
		methodNodeList.setAccessible(true);
		List<MethodDeclaration> methodList = (List)methodNodeList.get(overLoggingRefactor);
		methodList.add(ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theThirdOrderInTheSameClassWithJavaLog"));
		// check precondition
		assertEquals(1, methodList.size());
		// test target
		Method isExistence = OverLoggingRefactor.class.getDeclaredMethod("isExistence", MethodDeclaration.class);
		isExistence.setAccessible(true);
		assertTrue((Boolean)isExistence.invoke(overLoggingRefactor, ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theThirdOrderInTheSameClassWithJavaLog")));
	}
	
	@Test
	public void testAddFixListWithUnitListSizeIsZero() throws Exception {
		/** size of unitList is 0 */
		Field unitList = OverLoggingRefactor.class.getDeclaredField("unitList");
		unitList.setAccessible(true);
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(overLoggingRefactor, overLoggingJavaLogExampleUnit);
		List<?> cuList = (List<?>)unitList.get(overLoggingRefactor);
		// check precondition
		assertEquals(0, cuList.size());
		// test target
		Method addFixList = OverLoggingRefactor.class.getDeclaredMethod("addFixList");
		addFixList.setAccessible(true);
		addFixList.invoke(overLoggingRefactor);
		// check postcondition
		assertEquals(1, cuList.size());
	}
	
	@Test
	public void testAddFixListWithDuplicateMethods() throws Exception {
		/** there are duplicate methods */
		Field unitList = OverLoggingRefactor.class.getDeclaredField("unitList");
		unitList.setAccessible(true);
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(overLoggingRefactor, overLoggingJavaLogExampleUnit);
		List<?> cuList = (List<?>)unitList.get(overLoggingRefactor);
		Method addFixList = OverLoggingRefactor.class.getDeclaredMethod("addFixList");
		addFixList.setAccessible(true);
		assertEquals(0, cuList.size());
		addFixList.invoke(overLoggingRefactor);
		// check precondition
		assertEquals(1, cuList.size());
		// test target
		addFixList.invoke(overLoggingRefactor);
		// check postcondition
		assertEquals(1, cuList.size());
	}
	
	@Test
	public void testAddFixListWithNoDuplicateMethod() throws Exception {
		/** there is no duplicate methods */
		// check precondition
		Field unitList = OverLoggingRefactor.class.getDeclaredField("unitList");
		unitList.setAccessible(true);
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(overLoggingRefactor, overLoggingJavaLogExampleUnit);
		List<?> cuList = (List<?>)unitList.get(overLoggingRefactor);
		Method addFixList = OverLoggingRefactor.class.getDeclaredMethod("addFixList");
		addFixList.setAccessible(true);
		assertEquals(0, cuList.size());
		addFixList.invoke(overLoggingRefactor);
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
		assertFalse((Boolean)getIsKeepTrace.invoke(overLoggingRefactor, method, method));
		
		method = (IMethod)ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theSecondOrderInTheSameClassWithJavaLog").resolveBinding().getJavaElement();
		// test target
		assertTrue((Boolean)getIsKeepTrace.invoke(overLoggingRefactor, method, method));
	}
	
	@Test
	public void testDeleteCatchStatementWithIncorrectStatement() throws Exception {
		/** delete the incorrect statement */
		List<TryStatement> tryList = ASTNodeFinder.getTryStatementNodeListByMethodDeclarationName(overLoggingJavaLogExampleUnit, "theSecondOrderInTheSameClassWithJavaLog");
		CatchClause catchClause = (CatchClause)tryList.get(0).catchClauses().get(0);
		SingleVariableDeclaration singleVariableDeclaration = catchClause.getException();
		MarkerInfo markerInfo = new MarkerInfo(	RLMarkerAttribute.CS_OVER_LOGGING, singleVariableDeclaration.resolveBinding().getType(), catchClause.toString(), catchClause.getStartPosition(), 
												overLoggingJavaLogExampleUnit.getLineNumber(ASTNodeFinder.getNodeFromSpecifiedClass(OverLoggingJavaLogExample.class, projectName, 33).getStartPosition()), 
												singleVariableDeclaration.getType().toString());
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(overLoggingRefactor, overLoggingJavaLogExampleUnit);
		// check precondition
		List<?> statements = catchClause.getBody().statements();
		assertEquals(2, statements.size());
		assertEquals("javaLogger.log(Level.WARNING,e.getMessage() + \"theSecondOrderInTheSameClassWithJavaLog\");\n", statements.get(0).toString());
		assertEquals("throw e;\n", statements.get(1).toString());
		// test target
		Method deleteCatchStatement = OverLoggingRefactor.class.getDeclaredMethod("deleteCatchStatement", CatchClause.class, MarkerInfo.class);
		deleteCatchStatement.setAccessible(true);
		deleteCatchStatement.invoke(overLoggingRefactor, catchClause, markerInfo);
		// check postcondition
		assertEquals(2, statements.size());
		assertEquals("javaLogger.log(Level.WARNING,e.getMessage() + \"theSecondOrderInTheSameClassWithJavaLog\");\n", statements.get(0).toString());
		assertEquals("throw e;\n", statements.get(1).toString());
	}
	
	@Test
	public void testDeleteCatchStatementWithcorrectStatement() throws Exception {
		/** delete the correct statement */
		List<TryStatement> tryList = ASTNodeFinder.getTryStatementNodeListByMethodDeclarationName(overLoggingJavaLogExampleUnit, "theSecondOrderInTheSameClassWithJavaLog");
		CatchClause catchClause = (CatchClause)tryList.get(0).catchClauses().get(0);
		SingleVariableDeclaration singleVariableDeclaration = catchClause.getException();
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_OVER_LOGGING, singleVariableDeclaration.resolveBinding().getType(), catchClause.toString(), catchClause.getStartPosition(), 
				overLoggingJavaLogExampleUnit.getLineNumber(ASTNodeFinder.getNodeFromSpecifiedClass(OverLoggingJavaLogExample.class, projectName, 32).getStartPosition()), 
				singleVariableDeclaration.getType().toString());
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(overLoggingRefactor, overLoggingJavaLogExampleUnit);
		List<?> statements = catchClause.getBody().statements();
		// check precondition
		assertEquals(2, statements.size());
		assertEquals("javaLogger.log(Level.WARNING,e.getMessage() + \"theSecondOrderInTheSameClassWithJavaLog\");\n", statements.get(0).toString());
		assertEquals("throw e;\n", statements.get(1).toString());
		// test target
		Method deleteCatchStatement = OverLoggingRefactor.class.getDeclaredMethod("deleteCatchStatement", CatchClause.class, MarkerInfo.class);
		deleteCatchStatement.setAccessible(true);
		deleteCatchStatement.invoke(overLoggingRefactor, catchClause, markerInfo);
		// check postcondition
		assertEquals(1, statements.size());
		assertEquals("throw e;\n", statements.get(0).toString());
	}
	
	@Test
	public void testDeleteMessageWithNoMessageDeleted() throws Exception {
		/** there is no message which will be deleted */
		List<TryStatement> tryList = ASTNodeFinder.getTryStatementNodeListByMethodDeclarationName(overLoggingSelf4JExampleUnit, "theSecondOrderInTheSameClassWithSelf4J");
		CatchClause catchClause = (CatchClause)tryList.get(0).catchClauses().get(0);
		List<?> statements = catchClause.getBody().statements();
		// check precondition
		assertEquals(2, statements.size());
		assertEquals("self4jLogger.error(e.getMessage() + \"theSecondOrderInTheSameClassWithSelf4J\");\n", statements.get(0).toString());
		assertEquals("throw e;\n", statements.get(1).toString());
		// test target
		Method deleteMessage = OverLoggingRefactor.class.getDeclaredMethod("deleteMessage", CompilationUnit.class);
		deleteMessage.setAccessible(true);
		deleteMessage.invoke(overLoggingRefactor, overLoggingSelf4JExampleUnit);
		// check postcondition
		assertEquals(2, statements.size());
		assertEquals("self4jLogger.error(e.getMessage() + \"theSecondOrderInTheSameClassWithSelf4J\");\n", statements.get(0).toString());
		assertEquals("throw e;\n", statements.get(1).toString());
	}
	
	@Test
	public void testDeleteMessageWithDeletingMessage() throws Exception {
		Field methodNodeList = OverLoggingRefactor.class.getDeclaredField("methodNodeList");
		methodNodeList.setAccessible(true);
		List<MethodDeclaration> methodList = (List<MethodDeclaration>)methodNodeList.get(overLoggingRefactor);
		
		Field loggingList = OverLoggingRefactor.class.getDeclaredField("loggingList");
		loggingList.setAccessible(true);
		List<List<MarkerInfo>> logList = (List)loggingList.get(overLoggingRefactor);
		
		methodList.add(ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingSelf4JExampleUnit, "theSecondOrderInTheSameClassWithSelf4J"));
		List<TryStatement> tryList= ASTNodeFinder.getTryStatementNodeListByMethodDeclarationName(overLoggingSelf4JExampleUnit, "theSecondOrderInTheSameClassWithSelf4J");
		List<CatchClause> catchList = tryList.get(0).catchClauses();
		List<MarkerInfo> markerInfoList = new ArrayList<MarkerInfo>();
		markerInfoList.add(new MarkerInfo(	null, null, null, catchList.get(0).getStartPosition(), 36, null));
		logList.add(markerInfoList);
		
		// check precondition
		assertEquals(2, catchList.get(0).getBody().statements().size());
		assertEquals("self4jLogger.error(e.getMessage() + \"theSecondOrderInTheSameClassWithSelf4J\");\n", catchList.get(0).getBody().statements().get(0).toString());
		assertEquals("throw e;\n", catchList.get(0).getBody().statements().get(1).toString());
		// test target
		Method deleteMessage = OverLoggingRefactor.class.getDeclaredMethod("deleteMessage", CompilationUnit.class);
		deleteMessage.setAccessible(true);
		deleteMessage.invoke(overLoggingRefactor, overLoggingSelf4JExampleUnit);
		// check postcondition
		assertEquals(1, catchList.get(0).getBody().statements().size());
		assertEquals("throw e;\n", catchList.get(0).getBody().statements().get(0).toString());
	}
	
	@Test
	public void testTransMethodNode() throws Exception {
		MethodDeclaration methodDeclaration = ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingSelf4JExampleUnit, "theThirdOrderInTheSameClassWithSelf4J");
		IMethod method = (IMethod)methodDeclaration.resolveBinding().getJavaElement();
		
		// check precondition
		assertEquals(methodDeclaration.getName().getFullyQualifiedName(), method.getElementName());
		// test target
		Method transMethodNode = OverLoggingRefactor.class.getDeclaredMethod("transMethodNode", IMethod.class);
		transMethodNode.setAccessible(true);
		assertEquals(method.getElementName(), ((MethodDeclaration)transMethodNode.invoke(overLoggingRefactor, method)).getName().getFullyQualifiedName());
	}
	
	@Test
	public void testFindMethodIndexWithBeingIndex() throws Exception {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		overLoggingSelf4JExampleUnit.accept(methodCollector);
		// check precondition
		assertEquals(12, methodCollector.getMethodList().size());
		// test target
		Method findMethodIndex = OverLoggingRefactor.class.getDeclaredMethod("findMethodIndex", IMethod.class, List.class);
		findMethodIndex.setAccessible(true);
		assertEquals(4, findMethodIndex.invoke(overLoggingRefactor, (IMethod)ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingSelf4JExampleUnit, "calleeInOutterClassWithSelf4J").resolveBinding().getJavaElement(), methodCollector.getMethodList()));
	}
	
	@Test
	public void testFindMethodIndexWithNonexistIndex() throws Exception {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		overLoggingSelf4JExampleUnit.accept(methodCollector);
		// check precondition
		assertEquals(12, methodCollector.getMethodList().size());
		// test target
		Method findMethodIndex = OverLoggingRefactor.class.getDeclaredMethod("findMethodIndex", IMethod.class, List.class);
		findMethodIndex.setAccessible(true);
		assertEquals(-1, findMethodIndex.invoke(overLoggingRefactor, (IMethod)ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theSecondOrderInTheSameClassWithJavaLog").resolveBinding().getJavaElement(), methodCollector.getMethodList()));
	}
	
	@Test
	public void testBindMethodOnlyKnowMethodInfo() throws Exception {
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(overLoggingRefactor, overLoggingJavaLogExampleUnit);
		
		Field currentMethodNode = OverLoggingRefactor.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		MethodDeclaration md = (MethodDeclaration)currentMethodNode.get(overLoggingRefactor);
		
		Field currentLoggingList = OverLoggingRefactor.class.getDeclaredField("currentLoggingList");
		currentLoggingList.setAccessible(true);
		List<MarkerInfo> currentLogList = (List)currentLoggingList.get(overLoggingRefactor);
		
		Field methodNodeList = OverLoggingRefactor.class.getDeclaredField("methodNodeList");
		methodNodeList.setAccessible(true);
		List<MethodDeclaration> methodList = (List)methodNodeList.get(overLoggingRefactor);
		
		Field loggingList = OverLoggingRefactor.class.getDeclaredField("loggingList");
		loggingList.setAccessible(true);
		List<List<MarkerInfo>> logList = (List)loggingList.get(overLoggingRefactor);
		// check precondition
		assertNull(md);
		assertEquals(0, currentLogList.size());
		assertEquals(0, methodList.size());
		assertEquals(0, logList.size());
		// test target
		Method bindMethod = OverLoggingRefactor.class.getDeclaredMethod("bindMethod", IMethod.class);
		bindMethod.setAccessible(true);
		bindMethod.invoke(overLoggingRefactor, (IMethod)ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingJavaLogExampleUnit, "theSecondOrderInTheSameClassWithJavaLog").resolveBinding().getJavaElement());
		// check postcondition
		md = (MethodDeclaration)currentMethodNode.get(overLoggingRefactor);
		currentLogList = (List)currentLoggingList.get(overLoggingRefactor);
		methodList = (List)methodNodeList.get(overLoggingRefactor);
		logList = (List)loggingList.get(overLoggingRefactor);
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=java.io.IOException.class)}) " +
						"public void theSecondOrderInTheSameClassWithJavaLog() throws IOException {\n" +
						"  try {\n" +
						"    theThirdOrderInTheSameClassWithJavaLog();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    javaLogger.log(Level.WARNING,e.getMessage() + \"theSecondOrderInTheSameClassWithJavaLog\");\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", md.toString());
		assertEquals(1, currentLogList.size());
		assertEquals(	"catch (IOException e) {\n" +
						"  javaLogger.log(Level.WARNING,e.getMessage() + \"theSecondOrderInTheSameClassWithJavaLog\");\n" +
						"  throw e;\n" +
						"}\n", currentLogList.get(0).getStatement());
		assertEquals(1, methodList.size());
		assertEquals(md.toString(), methodList.get(0).toString());
		assertEquals(1, logList.size());
		assertEquals(1, logList.get(0).size());
		assertEquals(currentLogList.get(0).getStatement(), logList.get(0).get(0).getStatement());
	}
	
	@Test
	public void testBindMethodOnlyKnowMethodIndex() throws Exception {
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(overLoggingRefactor, overLoggingJavaLogExampleUnit);
		
		Field currentMethodNode = OverLoggingRefactor.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		MethodDeclaration md = (MethodDeclaration)currentMethodNode.get(overLoggingRefactor);
		
		Field currentLoggingList = OverLoggingRefactor.class.getDeclaredField("currentLoggingList");
		currentLoggingList.setAccessible(true);
		List<MarkerInfo> currentLogList = (List)currentLoggingList.get(overLoggingRefactor);
		
		Field methodNodeList = OverLoggingRefactor.class.getDeclaredField("methodNodeList");
		methodNodeList.setAccessible(true);
		List<MethodDeclaration> methodList = (List)methodNodeList.get(overLoggingRefactor);
		
		Field loggingList = OverLoggingRefactor.class.getDeclaredField("loggingList");
		loggingList.setAccessible(true);
		List<List<MarkerInfo>> logList = (List)loggingList.get(overLoggingRefactor);
		// check precondition
		assertNull(md);
		assertEquals(0, currentLogList.size());
		assertEquals(0, methodList.size());
		assertEquals(0, logList.size());
		// test target
		Method bindMethod = OverLoggingRefactor.class.getDeclaredMethod("bindMethod", int.class);
		bindMethod.setAccessible(true);
		bindMethod.invoke(overLoggingRefactor, 1);
		// check postcondition
		md = (MethodDeclaration)currentMethodNode.get(overLoggingRefactor);
		currentLogList = (List)currentLoggingList.get(overLoggingRefactor);
		methodList = (List)methodNodeList.get(overLoggingRefactor);
		logList = (List)loggingList.get(overLoggingRefactor);
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=java.io.IOException.class)}) " +
						"public void theSecondOrderInTheSameClassWithJavaLog() throws IOException {\n" +
						"  try {\n" +
						"    theThirdOrderInTheSameClassWithJavaLog();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    javaLogger.log(Level.WARNING,e.getMessage() + \"theSecondOrderInTheSameClassWithJavaLog\");\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", md.toString());
		assertEquals(1, currentLogList.size());
		assertEquals(	"catch (IOException e) {\n" +
						"  javaLogger.log(Level.WARNING,e.getMessage() + \"theSecondOrderInTheSameClassWithJavaLog\");\n" +
						"  throw e;\n" +
						"}\n", currentLogList.get(0).getStatement());
		assertEquals(1, methodList.size());
		assertEquals(md.toString(), methodList.get(0).toString());
		assertEquals(1, logList.size());
		assertEquals(1, logList.get(0).size());
		assertEquals(currentLogList.get(0).getStatement(), logList.get(0).get(0).getStatement());
	}
	
	@Test
	public void testObtainResourceWithIllegalResource() throws Exception {
		Field actOpenable = OverLoggingRefactor.class.getDeclaredField("actOpenable");
		actOpenable.setAccessible(true);
		
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		
		// check precondition
		assertNull(actOpenable.get(overLoggingRefactor));
		assertNull(actRoot.get(overLoggingRefactor));
		// test target
		assertFalse((Boolean)overLoggingRefactor.obtainResource(null));
	}
	
	@Test
	public void testObtainResourceWithlegalResource() throws Exception {
		Field actOpenable = OverLoggingRefactor.class.getDeclaredField("actOpenable");
		actOpenable.setAccessible(true);
		
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		
		// check precondition
		assertNull(actOpenable.get(overLoggingRefactor));
		assertNull(actRoot.get(overLoggingRefactor));
		// test target
		assertTrue((Boolean)overLoggingRefactor.obtainResource(RuntimeEnvironmentProjectReader.getIResource(projectName, OverLoggingJavaLogExample.class.getPackage().getName(), OverLoggingJavaLogExample.class.getSimpleName())));
		// check postcondition
		assertNotNull(actOpenable.get(overLoggingRefactor));
		assertEquals(overLoggingJavaLogExampleUnit.toString(), actRoot.get(overLoggingRefactor).toString());
	}
	
	@Test
	public void testTraceCallerMethodFromMidway() throws Exception {
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(overLoggingRefactor, overLoggingJavaLogExampleUnit);
		
		Field methodNodeList = OverLoggingRefactor.class.getDeclaredField("methodNodeList");
		methodNodeList.setAccessible(true);
		List<MethodDeclaration> methodList = (List)methodNodeList.get(overLoggingRefactor);
		
		Field loggingList = OverLoggingRefactor.class.getDeclaredField("loggingList");
		loggingList.setAccessible(true);
		List<List<MarkerInfo>> logList = (List)loggingList.get(overLoggingRefactor);
		// check precondition
		assertEquals(0, methodList.size());
		assertEquals(0, logList.size());
		// test target
		Method traceCallerMethod = OverLoggingRefactor.class.getDeclaredMethod("traceCallerMethod", MethodDeclaration.class);
		traceCallerMethod.setAccessible(true);
		traceCallerMethod.invoke(overLoggingRefactor, ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingLog4JExampleUnit, "theThirdOrderInTheSameClassWithLog4J"));
		// check postcondition
		methodList = (List)methodNodeList.get(overLoggingRefactor);
		logList = (List)loggingList.get(overLoggingRefactor);
		assertEquals(1, methodList.size());
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=java.io.IOException.class)}) " +
						"public void theSecondOrderInTheSameClassWithLog4J() throws IOException {\n" +
						"  try {\n" +
						"    theThirdOrderInTheSameClassWithLog4J();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    log4jLogger.error(e.getMessage() + \"theSecondOrderInTheSameClassWithLog4J\");\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", methodList.get(0).toString());
		assertEquals(1, logList.size());
		assertEquals(1, logList.get(0).size());
		assertEquals(	"catch (IOException e) {\n" +
						"  log4jLogger.error(e.getMessage() + \"theSecondOrderInTheSameClassWithLog4J\");\n" +
						"  throw e;\n" +
						"}\n", logList.get(0).get(0).getStatement());
	}
	
	@Test
	public void testTraceCalleeMethodFromMidway() throws Exception {
		Field actRoot = OverLoggingRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(overLoggingRefactor, overLoggingJavaLogExampleUnit);
		
		Field methodNodeList = OverLoggingRefactor.class.getDeclaredField("methodNodeList");
		methodNodeList.setAccessible(true);
		List<MethodDeclaration> methodList = (List)methodNodeList.get(overLoggingRefactor);
		
		Field loggingList = OverLoggingRefactor.class.getDeclaredField("loggingList");
		loggingList.setAccessible(true);
		List<List<MarkerInfo>> logList = (List)loggingList.get(overLoggingRefactor);
		// check precondition
		assertEquals(0, methodList.size());
		assertEquals(0, logList.size());
		// test target
		Method traceCalleeMethod = OverLoggingRefactor.class.getDeclaredMethod("traceCalleeMethod", MethodDeclaration.class);
		traceCalleeMethod.setAccessible(true);
		traceCalleeMethod.invoke(overLoggingRefactor, ASTNodeFinder.getMethodDeclarationNodeByName(overLoggingLog4JExampleUnit, "theThirdOrderInTheSameClassWithLog4J"));
		// check postcondition
		methodList = (List)methodNodeList.get(overLoggingRefactor);
		logList = (List)loggingList.get(overLoggingRefactor);
		assertEquals(1, methodList.size());
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=java.io.IOException.class)}) public void theFourthOrderInTheSameClassWithLog4J() throws IOException {\n" +
						"  try {\n" +
						"    throw new IOException(\"IOException throws in callee\");\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    e.printStackTrace();\n" +
						"    log4jLogger.error(e.getMessage() + \"theFourthOrderInTheSameClassWithLog4J\");\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", methodList.get(0).toString());
		assertEquals(1, logList.size());
		assertEquals(1, logList.get(0).size());
		assertEquals(	"catch (IOException e) {\n" +
						"  e.printStackTrace();\n" +
						"  log4jLogger.error(e.getMessage() + \"theFourthOrderInTheSameClassWithLog4J\");\n" +
						"  throw e;\n" +
						"}\n", logList.get(0).get(0).getStatement());
	}
	
	@Ignore
	public void testRefator() {
		fail("功能有問題且會呼叫applyChange，故先不測");
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OVERLOGGING_DETECTWRAPPINGEXCEPTION);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_OVERLOGGING, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.addOverLoggingPattern(org.slf4j.Logger.class.getPackage().getName() + "." + org.slf4j.Logger.class.getSimpleName(), true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
