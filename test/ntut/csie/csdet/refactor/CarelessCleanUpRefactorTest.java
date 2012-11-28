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
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.CarelessCleanupVisitor;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.RuntimeEnvironmentProjectReader;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassImplementCloseableWithoutThrowException;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.ClassWithNotThrowingExceptionCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupDog;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.UserDefinedCarelessCleanupWeather;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CarelessCleanUpRefactorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	SmellSettings smellSettings;
	CarelessCleanUpRefactor refactor;
	IJavaElement javaElement;
	IMarker tempMarker;

	@Before
	public void setUp() throws Exception {
		String testProjectName = "CarelessCleanupExampleProject";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String.read(CarelessCleanupExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				CarelessCleanupExample.class.getPackage().getName(),
				CarelessCleanupExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + CarelessCleanupExample.class.getPackage().getName() + ";" + String.format("%n")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(ClassWithNotThrowingExceptionCloseable.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassWithNotThrowingExceptionCloseable.class.getPackage().getName(),
				ClassWithNotThrowingExceptionCloseable.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + ClassWithNotThrowingExceptionCloseable.class.getPackage().getName() + ";" + String.format("%n")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(ClassImplementCloseable.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassImplementCloseable.class.getPackage().getName(),
				ClassImplementCloseable.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + ClassImplementCloseable.class.getPackage().getName() + ";" + String.format("%n")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		/* 測試使用者設定Pattern時候使用 */
		javaFile2String.read(UserDefinedCarelessCleanupWeather.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefinedCarelessCleanupWeather.class.getPackage().getName(),
				UserDefinedCarelessCleanupWeather.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefinedCarelessCleanupWeather.class.getPackage().getName() + ";" + String.format("%n")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(UserDefinedCarelessCleanupDog.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefinedCarelessCleanupDog.class.getPackage().getName(),
				UserDefinedCarelessCleanupDog.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefinedCarelessCleanupDog.class.getPackage().getName() + ";" + String.format("%n")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		javaFile2String.read(ClassImplementCloseableWithoutThrowException.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassImplementCloseableWithoutThrowException.class.getPackage().getName(),
				ClassImplementCloseableWithoutThrowException.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + ClassImplementCloseableWithoutThrowException.class.getPackage().getName() + ";" + String.format("%n")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		Path ccExamplePath = new Path(testProjectName + "/"
				+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(CarelessCleanupExample.class.getName())
				+ JavaProjectMaker.JAVA_FILE_EXTENSION);
		
		javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(ccExamplePath));
		tempMarker = javaElement.getResource().createMarker("test.test");
		
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);
		
		CreateSettings();
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		
		refactor = new CarelessCleanUpRefactor();
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
	public void testFindMethod() throws Exception {
		tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, "6");
		refactor.setMarker(tempMarker);
		
		Field actOpenable = CarelessCleanUpRefactor.class.getDeclaredField("actOpenable");
		actOpenable.setAccessible(true);
		
		Field actRoot = CarelessCleanUpRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		
		Field currentMethodNode = CarelessCleanUpRefactor.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		
		// check precondition
		assertNull(actOpenable.get(refactor));
		assertNull(actRoot.get(refactor));
		assertNull(currentMethodNode.get(refactor));
		
		// test
		Method findMethod = CarelessCleanUpRefactor.class.getDeclaredMethod("findMethod", IResource.class);
		findMethod.setAccessible(true);
		assertTrue((Boolean)findMethod.invoke(refactor, javaElement.getResource()));
		
		// check postcondition
		assertEquals("CarelessCleanupExample.java (not open) [in ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup [in src [in CarelessCleanupExampleProject]]]", actOpenable.get(refactor).toString());
		assertNotNull(actRoot.get(refactor));
		assertEquals(	"/** \n" + 
						" * 會被CarelessCleanupVisitor在fileOutputStream.close();加上mark(兩處)\n" +
						" * @param context\n" +
						" * @param outputFile\n" +
						" * @throws IOException\n" + 
						" */\n" +
						"@Robustness(value={@RTag(level=1,exception=java.io.FileNotFoundException.class),@RTag(level=1,exception=java.io.IOException.class)}) public void y2_closeStreamInCatchClause(byte[] context,File outputFile) throws IOException {\n" +
						"  FileOutputStream fileOutputStream=null;\n" +
						"  try {\n" +
						"    fileOutputStream=new FileOutputStream(outputFile);\n" +
						"    fileOutputStream.write(context);\n" +
						"  }\n" +
						" catch (  FileNotFoundException e) {\n" +
						"    fileOutputStream.close();\n" +
						"    throw e;\n" +
						"  }\n" +
						"catch (  IOException e) {\n" +
						"    fileOutputStream.close();\n" +
						"    throw e;\n" +
						"  }\n" +
						" finally {\n" +
						"    System.out.println(\"Close nothing at all.\");\n" +
						"  }\n" +
						"}\n", currentMethodNode.get(refactor).toString());
	}
	
	@Test
	public void testGetCurrentMethodNode() throws Exception {
		tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, "6");
		refactor.setMarker(tempMarker);
		ASTNode node = refactor.getCurrentMethodNode();
		assertEquals(ASTNode.METHOD_DECLARATION, node.getNodeType());
		assertEquals(	"/** \n" + 
						" * 會被CarelessCleanupVisitor在fileOutputStream.close();加上mark(兩處)\n" +
						" * @param context\n" +
						" * @param outputFile\n" +
						" * @throws IOException\n" + 
						" */\n" +
						"@Robustness(value={@RTag(level=1,exception=java.io.FileNotFoundException.class),@RTag(level=1,exception=java.io.IOException.class)}) public void y2_closeStreamInCatchClause(byte[] context,File outputFile) throws IOException {\n" +
						"  FileOutputStream fileOutputStream=null;\n" +
						"  try {\n" +
						"    fileOutputStream=new FileOutputStream(outputFile);\n" +
						"    fileOutputStream.write(context);\n" +
						"  }\n" +
						" catch (  FileNotFoundException e) {\n" +
						"    fileOutputStream.close();\n" +
						"    throw e;\n" +
						"  }\n" +
						"catch (  IOException e) {\n" +
						"    fileOutputStream.close();\n" +
						"    throw e;\n" +
						"  }\n" +
						" finally {\n" +
						"    System.out.println(\"Close nothing at all.\");\n" +
						"  }\n" +
						"}\n", node.toString());
	}
	
	@Test
	public void testSetNewMethodName() throws Exception {
		String currentName = "methodNameisFine";
		String errorName = "1MethodName";
		String emptyName = "";
		String specialName1 = "@Name";
		String specialName2 = "name%";
		
		Field methodName = CarelessCleanUpRefactor.class.getDeclaredField("methodName");
		methodName.setAccessible(true);
		
		assertEquals(	"<FATALERROR\n" +
						"\t\n" +
						"FATALERROR: Method Name is empty\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n>", refactor.setNewMethodName(emptyName).toString());
		assertNull(methodName.get(refactor));
		assertEquals("<OK\n>", refactor.setNewMethodName(currentName).toString());
		assertEquals(currentName, methodName.get(refactor));
		assertEquals(	"<FATALERROR\n" +
						"\t\n" +
						"FATALERROR: 1MethodName is not a valid Java identifer\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n>", refactor.setNewMethodName(errorName).toString());
		assertEquals(	"<FATALERROR\n" +
						"\t\n" +
						"FATALERROR: @Name is not a valid Java identifer\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n>", refactor.setNewMethodName(specialName1).toString());
		assertEquals(	"<FATALERROR\n" +
						"\t\n" +
						"FATALERROR: name% is not a valid Java identifer\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n>", refactor.setNewMethodName(specialName2).toString());
	}
	
	/**
	 * setNewMethodModifierType和setNewMethodLogType一起測
	 * @throws Exception
	 */
	@Test
	public void testSetNewType() throws Exception {
		String someType = "somethingType";
		
		Field modifierType = CarelessCleanUpRefactor.class.getDeclaredField("modifierType");
		modifierType.setAccessible(true);
		
		assertNull(modifierType.get(refactor));
		assertEquals(	"<FATALERROR\n" +
						"\t\n" +
						"FATALERROR: Some Field is empty\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n>", refactor.setNewMethodModifierType("").toString());
		assertNull(modifierType.get(refactor));
		assertEquals("<OK\n>", refactor.setNewMethodModifierType(someType).toString());
		assertEquals(someType, modifierType.get(refactor));
		
		String log = "e.printStackTrace()";
		
		Field logType = CarelessCleanUpRefactor.class.getDeclaredField("logType");
		logType.setAccessible(true);
		
		assertNull(logType.get(refactor));
		assertEquals(	"<FATALERROR\n" +
						"\t\n" +
						"FATALERROR: Some Field is empty\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n>", refactor.setNewMethodLogType("").toString());
		assertNull(logType.get(refactor));
		assertEquals("<OK\n>", refactor.setNewMethodLogType(log).toString());
		assertEquals(log, logType.get(refactor));
	}
	
	@Test
	public void testDetectIfStatementSize() throws Exception {
		CarelessCleanupVisitor visitor = new CarelessCleanupVisitor(compilationUnit);
		compilationUnit.accept(visitor);
		
		tempMarker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, "11");
		refactor.setMarker(tempMarker);
		
		Field CarelessCleanUpList = CarelessCleanUpRefactor.class.getDeclaredField("CarelessCleanUpList");
		CarelessCleanUpList.setAccessible(true);
		CarelessCleanUpList.set(refactor, visitor.getCarelessCleanupList());
		
		Method findSmellMessage = CarelessCleanUpRefactor.class.getDeclaredMethod("findSmellMessage");
		findSmellMessage.setAccessible(true);
		findSmellMessage.invoke(refactor);
		
		ASTMethodCollector mVisitor = new ASTMethodCollector();
		compilationUnit.accept(mVisitor);
		MethodDeclaration md = (MethodDeclaration)mVisitor.getMethodList().get(11);
		
		// delete bad smell in the if statement with size == 1 and block
		Method detectIfStatementSize = CarelessCleanUpRefactor.class.getDeclaredMethod("detectIfStatementSize", Block.class);
		detectIfStatementSize.setAccessible(true);
		assertTrue((Boolean)detectIfStatementSize.invoke(refactor, ((TryStatement)md.getBody().statements().get(0)).getBody()));
		
		// can't delete bad smell in the if statement with size > 1
		tempMarker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, "12");
		refactor.setMarker(tempMarker);
		
		findSmellMessage.invoke(refactor);
		
		md = (MethodDeclaration)mVisitor.getMethodList().get(12);
		assertFalse((Boolean)detectIfStatementSize.invoke(refactor, ((TryStatement)md.getBody().statements().get(0)).getBody()));
		
		// delete bad smell in the if statement with size == 1 and non-block
		
		MarkerInfo marker = new MarkerInfo(null, null, null, 8364, 280, null);
		Field smellMessage = CarelessCleanUpRefactor.class.getDeclaredField("smellMessage");
		smellMessage.setAccessible(true);
		smellMessage.set(refactor, marker);
		
		md = (MethodDeclaration)mVisitor.getMethodList().get(14);
		assertTrue((Boolean)detectIfStatementSize.invoke(refactor, ((TryStatement)md.getBody().statements().get(0)).getBody()));
	}
	
	@Test
	public void testCheckInitialConditions() throws Exception {
		tempMarker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, "0");
		tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, "11");
		refactor.setMarker(tempMarker);
		
		// if statement with size == 1
		assertEquals("<OK\n>", refactor.checkInitialConditions(null).toString());
		
		tempMarker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, "0");
		tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, "12");
		refactor.setMarker(tempMarker);
		
		// if statement with size > 1
		assertEquals(	"<FATALERROR\n" +
						"\t\n" +
						"FATALERROR: 判斷式內有其它的程式碼\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n>", refactor.checkInitialConditions(null).toString());
	}
	
	@Test
	public void testFindSmellMessage() throws Exception {
		tempMarker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, "0");
		tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, "12");
		refactor.setMarker(tempMarker);
		
		CarelessCleanupVisitor visitor = new CarelessCleanupVisitor(compilationUnit);
		compilationUnit.accept(visitor);
		
		Field CarelessCleanUpList = CarelessCleanUpRefactor.class.getDeclaredField("CarelessCleanUpList");
		CarelessCleanUpList.setAccessible(true);
		CarelessCleanUpList.set(refactor, visitor.getCarelessCleanupList());
	}
	
	@Test
	public void testFindTryStatement() throws Exception {
		MethodDeclaration md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "y_closeStreamWithElseBigTry");
		
		Field currentMethodNode = CarelessCleanUpRefactor.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(refactor, md);
		
		MarkerInfo marker = new MarkerInfo(null, null, null, 7349, 238, null);
		Field smellMessage = CarelessCleanUpRefactor.class.getDeclaredField("smellMessage");
		smellMessage.setAccessible(true);
		smellMessage.set(refactor, marker);
		
		// if statement contain bad smell
		Method findTryStatement = CarelessCleanUpRefactor.class.getDeclaredMethod("findTryStatement");
		findTryStatement.setAccessible(true);
		assertEquals(	"try {\n" +
						"  if (fileOutputStream != null) {\n" +
						"    fileOutputStream.close();\n" +
						"  }\n" +
						" else {\n" +
						"    System.out.println(\"Stream cannot be closed.\");\n" +
						"  }\n" +
						"}\n" +
						" catch (IOException e) {\n" +
						"  e.printStackTrace();\n" +
						"}\n", findTryStatement.invoke(refactor).toString());
		
		// for statement contain bad smell
		marker = new MarkerInfo(null, null, null, 12858, 444, null);
		smellMessage.set(refactor, marker);
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "y_multiNestedStatementWithTryBlock");
		currentMethodNode.set(refactor, md);
		assertEquals(	"try {\n" +
						"  if (a == 5) {\n" +
						"    FileWriter fw=new FileWriter(\"filepath\");\n" +
						"    fw.write(\"fileContents\");\n" +
						"    fw.close();\n" +
						"  }\n" +
						"}\n" +
						" catch (IOException e) {\n" +
						"  throw e;\n" +
						"}\n", findTryStatement.invoke(refactor).toString());
		
		// while statement conatin bad smell
		marker = new MarkerInfo(null, null, null, 13454, 469, null);
		smellMessage.set(refactor, marker);
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "y_closeStreamInFinallyButThrowsExceptionInCatchAndFinally");
		currentMethodNode.set(refactor, md);
		assertEquals(	"try {\n" +
						"  if (a == 5) {\n" +
						"    fw=new FileWriter(\"filepath\");\n" +
						"    fw.write(\"fileContents\");\n" +
						"  }\n" +
						"}\n" +
						" catch (IOException e) {\n" +
						"  throw e;\n" +
						"}\n" +
						" finally {\n" +
						"  fw.close();\n" +
						"}\n", findTryStatement.invoke(refactor).toString());
	}
	
	@Test
	public void testAddFinallyBlock() throws Exception {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		MethodDeclaration md = (MethodDeclaration)methodCollector.getMethodList().get(0);
		
		TryStatement tryStatement = (TryStatement)md.getBody().statements().get(1);
		// check precondition
		assertNull(tryStatement.getFinally());
		// test
		Method addFinallyBlock = CarelessCleanUpRefactor.class.getDeclaredMethod("addFinallyBlock", AST.class, TryStatement.class);
		addFinallyBlock.setAccessible(true);
		addFinallyBlock.invoke(refactor, md.getAST(), tryStatement);
		// check postcondition
		assertNotNull(tryStatement.getFinally());
	}

	@Test
	public void testDeleteBlockStatement() throws Exception {
		String nameOfWillBeTestedMethod = "y_closeStreamInTryBlock";
		MethodDeclaration md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, nameOfWillBeTestedMethod);
		TryStatement tryStatement = ASTNodeFinder.getTryStatementNodeListByMethodDeclarationName(compilationUnit, nameOfWillBeTestedMethod).get(0);
		/**
		 * 關閉串流的程式碼startposition是1580
		 */
		MarkerInfo marker = new MarkerInfo(null, null, null, 1580, 54, null);
		Field smellMessage = CarelessCleanUpRefactor.class.getDeclaredField("smellMessage");
		smellMessage.setAccessible(true);
		smellMessage.set(refactor, marker);
		
		Method deleteBlockStatement = CarelessCleanUpRefactor.class.getDeclaredMethod("deleteBlockStatement", Block.class, AST.class);
		deleteBlockStatement.setAccessible(true);
		assertTrue((Boolean)deleteBlockStatement.invoke(refactor, tryStatement.getBody(), md.getAST()));
	}
	
	@Test
	public void testDeleteCleanUpLine() throws Exception {
		String nameOfWillBeTestedMethod = "y_closeStreamWithMultiStatementInThenBigTry";
		MethodDeclaration md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, nameOfWillBeTestedMethod);
		TryStatement tryStatement = ASTNodeFinder.getTryStatementNodeListByMethodDeclarationName(compilationUnit, nameOfWillBeTestedMethod).get(0);
		
		MarkerInfo marker = new MarkerInfo(null, null, null, 7854-1, 256, null);
		Field smellMessage = CarelessCleanUpRefactor.class.getDeclaredField("smellMessage");
		smellMessage.setAccessible(true);
		smellMessage.set(refactor, marker);
		
		// check precondition
		assertEquals(	"try {\n" +
						"  if (fileOutputStream != null) {\n" +
						"    fileOutputStream.close();\n" +
						"    System.out.println(\"Stream cannot be closed.\");\n" +
						"  }\n" +
						"}\n" +
						" catch (IOException e) {\n" +
						"  e.printStackTrace();\n" +
						"}\n", tryStatement.toString());
		
		// delete bad smell in try block
		Method deleteCleanUpLine = CarelessCleanUpRefactor.class.getDeclaredMethod("deleteCleanUpLine", AST.class, TryStatement.class);
		deleteCleanUpLine.setAccessible(true);
		deleteCleanUpLine.invoke(refactor, md.getAST(), tryStatement);
		
		// check postcondition
		assertEquals(	"try {\n" +
						"}\n" +
						" catch (IOException e) {\n" +
						"  e.printStackTrace();\n" +
						"}\n", tryStatement.toString());
		
		nameOfWillBeTestedMethod = "y2_closeStreamInCatchClause";
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, nameOfWillBeTestedMethod);
		tryStatement = ASTNodeFinder.getTryStatementNodeListByMethodDeclarationName(compilationUnit, nameOfWillBeTestedMethod).get(0);
		marker = new MarkerInfo(null, null, null, 5079, 161, null);
		smellMessage.set(refactor, marker);
		
		// check precondition
		assertEquals(	"try {\n" +
						"  fileOutputStream=new FileOutputStream(outputFile);\n" +
						"  fileOutputStream.write(context);\n" +
						"}\n" +
						" catch (FileNotFoundException e) {\n" +
						"  fileOutputStream.close();\n" +
						"  throw e;\n" +
						"}\n" +
						"catch (IOException e) {\n" +
						"  fileOutputStream.close();\n" +
						"  throw e;\n" +
						"}\n" +
						" finally {\n" +
						"  System.out.println(\"Close nothing at all.\");\n" +
						"}\n", tryStatement.toString());
		
		// delete bad smell in catch block
		deleteCleanUpLine.invoke(refactor, md.getAST(), tryStatement);
		
		// check postcondition
		assertEquals(	"try {\n" +
						"  fileOutputStream=new FileOutputStream(outputFile);\n" +
						"  fileOutputStream.write(context);\n" +
						"}\n" +
						" catch (FileNotFoundException e) {\n" +
						"  throw e;\n" +
						"}\n" +
						"catch (IOException e) {\n" +
						"  fileOutputStream.close();\n" +
						"  throw e;\n" +
						"}\n" +
						" finally {\n" +
						"  System.out.println(\"Close nothing at all.\");\n" +
						"}\n", tryStatement.toString());
		
		nameOfWillBeTestedMethod = "y_closeStreamInFinallyButThrowsExceptionInCatchAndFinally";
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, nameOfWillBeTestedMethod);
		tryStatement = ASTNodeFinder.getTryStatementNodeListByMethodDeclarationName(compilationUnit, nameOfWillBeTestedMethod).get(0);
		marker = new MarkerInfo(null, null, null, 13566, 471, null);
		smellMessage.set(refactor, marker);
		
		// check precondition
		assertEquals(	"try {\n" +
						"  if (a == 5) {\n" +
						"    fw=new FileWriter(\"filepath\");\n" +
						"    fw.write(\"fileContents\");\n" +
						"  }\n" +
						"}\n" +
						" catch (IOException e) {\n" +
						"  throw e;\n" +
						"}\n" +
						" finally {\n" +
						"  fw.close();\n" +
						"}\n", tryStatement.toString());
		
		// delete bad smell in finally block
		deleteCleanUpLine.invoke(refactor, md.getAST(), tryStatement);
		
		// check postcondition
		assertEquals(	"try {\n" +
						"  if (a == 5) {\n" +
						"    fw=new FileWriter(\"filepath\");\n" +
						"    fw.write(\"fileContents\");\n" +
						"  }\n" +
						"}\n" +
						" catch (IOException e) {\n" +
						"  throw e;\n" +
						"}\n" +
						" finally {\n" +
						"}\n", tryStatement.toString());
	}
	
	@Test
	public void testMoveInstance() throws Exception {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		MethodDeclaration md = (MethodDeclaration)methodCollector.getMethodList().get(31);
		TryStatement tryStatement = (TryStatement)md.getBody().statements().get(0);
		
		Field currentMethodNode = CarelessCleanUpRefactor.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(refactor, md);
		
		Field tryIndex = CarelessCleanUpRefactor.class.getDeclaredField("tryIndex");
		tryIndex.setAccessible(true);
		tryIndex.set(refactor, 0);
		
		Field cleanUpExpressionStatement = CarelessCleanUpRefactor.class.getDeclaredField("cleanUpExpressionStatement");
		cleanUpExpressionStatement.setAccessible(true);
		cleanUpExpressionStatement.set(refactor, tryStatement.getBody().statements().get(2));
		
		// check precondition
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=java.io.IOException.class)}) public void moveInstance() throws IOException {\n" +
						"  try {\n" +
						"    FileOutputStream fi=new FileOutputStream(\"\");\n" +
						"    fi.write(1);\n" +
						"    fi.close();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", md.toString());
		
		// test
		Method moveInstance = CarelessCleanUpRefactor.class.getDeclaredMethod("moveInstance", AST.class, TryStatement.class);
		moveInstance.setAccessible(true);
		moveInstance.invoke(refactor, md.getAST(), tryStatement);
		
		// check postcondition
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=java.io.IOException.class)}) " +
						"public void moveInstance() throws IOException {\n" +
						"  FileOutputStream fi=null;\n" +
						"  try {\n" +
						"    fi=new FileOutputStream(\"\");\n" +
						"    fi.write(1);\n" +
						"    fi.close();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    throw e;\n" +
						"  }\n" +
						"}\n", md.toString());
	}
	
	/**
	 * FIXME - 未完全模擬正確
	 * @throws Exception
	 */
	@Test
	public void testAddImportPackage() throws Exception {
		Field actRoot = CarelessCleanUpRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(refactor, compilationUnit);
		
		// check precondition
		List<?> imports = compilationUnit.imports();
		assertEquals(9, imports.size());
		assertEquals("import java.io.Closeable;\n", imports.get(0).toString());
		assertEquals("import java.io.File;\n", imports.get(1).toString());
		assertEquals("import java.io.FileInputStream;\n", imports.get(2).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(3).toString());
		assertEquals("import java.io.FileOutputStream;\n", imports.get(4).toString());
		assertEquals("import java.io.FileWriter;\n", imports.get(5).toString());
		assertEquals("import java.io.IOException;\n", imports.get(6).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.RTag;\n", imports.get(7).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.Robustness;\n", imports.get(8).toString());
		
		// 模擬加入一個method，而需要import原本沒有的package
		ImportDeclaration imp = compilationUnit.getAST().newImportDeclaration();
		imp.setName(compilationUnit.getAST().newName("java.io.IOError"));
		compilationUnit.imports().add(imp);
		// test
		Method addImportPackage = CarelessCleanUpRefactor.class.getDeclaredMethod("addImportPackage", IType.class);
		addImportPackage.setAccessible(true);
		addImportPackage.invoke(refactor, RuntimeEnvironmentProjectReader.getType(javaProjectMaker.getProjectName(), UserDefinedCarelessCleanupDog.class.getPackage().getName(), UserDefinedCarelessCleanupDog.class.getSimpleName()));
		// check postcondition
		assertEquals(10, imports.size());
		assertEquals("import java.io.Closeable;\n", imports.get(0).toString());
		assertEquals("import java.io.File;\n", imports.get(1).toString());
		assertEquals("import java.io.FileInputStream;\n", imports.get(2).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(3).toString());
		assertEquals("import java.io.FileOutputStream;\n", imports.get(4).toString());
		assertEquals("import java.io.FileWriter;\n", imports.get(5).toString());
		assertEquals("import java.io.IOException;\n", imports.get(6).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.RTag;\n", imports.get(7).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.Robustness;\n", imports.get(8).toString());
		assertEquals("import java.io.IOError;\n", imports.get(9).toString());
	}
	
//	@Test
	public void testCreateCallerMethod() throws Exception {
		fail("");
	}
	
	@Test
	public void testCreateNewMethod() throws Exception {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		MethodDeclaration md = (MethodDeclaration)methodCollector.getMethodList().get(31);
		TryStatement tryStatement = (TryStatement)md.getBody().statements().get(0);
		
		ExpressionStatement mi = (ExpressionStatement)tryStatement.getBody().statements().get(2);
		
		// check precondition
		assertEquals("fi.close();\n", mi.toString());
		// test
		Method createNewMethod = CarelessCleanUpRefactor.class.getDeclaredMethod("createNewMethod", AST.class, Expression.class, String.class);
		createNewMethod.setAccessible(true);
		// check postcondition
		assertEquals("close(fi)", createNewMethod.invoke(refactor, md.getAST(), ((MethodInvocation)mi.getExpression()).getExpression(), "close").toString());
	}
	
	@Test
	public void testAddMethodInFinally() throws Exception {
		/** try statement without finally and new close method itself, expression statement is "fileOutputStream.close();" */
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		MethodDeclaration md = (MethodDeclaration)methodCollector.getMethodList().get(1);
		TryStatement tryStatement = (TryStatement)md.getBody().statements().get(1);
		Block finallyBlock = md.getAST().newBlock();
		tryStatement.setFinally(finallyBlock);
		
		Field actRoot = CarelessCleanUpRefactor.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(refactor, compilationUnit);
		
		Field currentMethodNode = CarelessCleanUpRefactor.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(refactor, md);
		
		Field tryIndex = CarelessCleanUpRefactor.class.getDeclaredField("tryIndex");
		tryIndex.setAccessible(true);
		tryIndex.set(refactor, 0);
		
		Field methodName = CarelessCleanUpRefactor.class.getDeclaredField("methodName");
		methodName.setAccessible(true);
		methodName.set(refactor, "close");
		
		Field modifierType = CarelessCleanUpRefactor.class.getDeclaredField("modifierType");
		modifierType.setAccessible(true);
		modifierType.set(refactor, "private");
		
		Field logType = CarelessCleanUpRefactor.class.getDeclaredField("logType");
		logType.setAccessible(true);
		logType.set(refactor, "e.printStackTrace();");
		
		Field cleanUpExpressionStatement = CarelessCleanUpRefactor.class.getDeclaredField("cleanUpExpressionStatement");
		cleanUpExpressionStatement.setAccessible(true);
		ExpressionStatement es = (ExpressionStatement)tryStatement.getBody().statements().remove(2);
		cleanUpExpressionStatement.set(refactor, es);
		
		// check precondition
		assertEquals(	"/** \n" +
						" * 會被CarelessCleanupVisitor在fileOutputStream.close();加上mark\n" +
						" * @param context\n" +
						" * @param outputFile\n" +
						" */\n" +
						"@Robustness(value={@RTag(level=1,exception=java.lang.RuntimeException.class)}) public void y_closeStreamInTryBlock(byte[] context,File outputFile){\n" +
						"  FileOutputStream fileOutputStream=null;\n" +
						"  try {\n" +
						"    fileOutputStream=new FileOutputStream(outputFile);\n" +
						"    fileOutputStream.write(context);\n" +
						"  }\n" +
						" catch (  FileNotFoundException e) {\n" +
						"    throw new RuntimeException(e);\n" +
						"  }\n" +
						"catch (  IOException e) {\n" +
						"    throw new RuntimeException(e);\n" +
						"  }\n" +
						" finally {\n" +
						"  }\n" +
						"}\n", md.toString());
		assertEquals("fileOutputStream.close();\n", es.toString());
		// test
		Method addMethodInFinally = CarelessCleanUpRefactor.class.getDeclaredMethod("addMethodInFinally", AST.class, Block.class);
		addMethodInFinally.setAccessible(true);
		addMethodInFinally.invoke(refactor, md.getAST(), tryStatement.getFinally());
		// check postcondition
		assertEquals(	"/** \n" +
				" * 會被CarelessCleanupVisitor在fileOutputStream.close();加上mark\n" +
				" * @param context\n" +
				" * @param outputFile\n" +
				" */\n" +
				"@Robustness(value={@RTag(level=1,exception=java.lang.RuntimeException.class)}) public void y_closeStreamInTryBlock(byte[] context,File outputFile){\n" +
				"  FileOutputStream fileOutputStream=null;\n" +
				"  try {\n" +
				"    fileOutputStream=new FileOutputStream(outputFile);\n" +
				"    fileOutputStream.write(context);\n" +
				"  }\n" +
				" catch (  FileNotFoundException e) {\n" +
				"    throw new RuntimeException(e);\n" +
				"  }\n" +
				"catch (  IOException e) {\n" +
				"    throw new RuntimeException(e);\n" +
				"  }\n" +
				" finally {\n" +
				"    close(fileOutputStream);\n" +
				"  }\n" +
				"}\n", md.toString());
		
		/** try statement with finally block and new close method itself, expression statement is "closeStreamWithoutThrowingException(fi);" */
		md = (MethodDeclaration)methodCollector.getMethodList().get(30);
		tryStatement = (TryStatement)md.getBody().statements().get(1);
		currentMethodNode.set(refactor, md);
		es = (ExpressionStatement)((CatchClause)tryStatement.catchClauses().get(0)).getBody().statements().remove(0);
		cleanUpExpressionStatement.set(refactor, es);
		// test
		addMethodInFinally.invoke(refactor, md.getAST(), tryStatement.getFinally());
		// check postcondition
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=java.io.IOException.class)}) public void uy_closeStreaminOuterMethodInCatch() throws IOException {\n" +
						"  FileOutputStream fi=null;\n" +
						"  try {\n" +
						"    fi=new FileOutputStream(\"\");\n" +
						"    fi.write(1);\n" +
						"  }\n" +
						" catch (  FileNotFoundException e) {\n" +
						"    throw e;\n" +
						"  }\n" +
						" finally {\n" +
						"    closeStreamWithoutThrowingException(fi);\n" +
						"  }\n" +
						"}\n", md.toString());
	}
	
//	@Test
	public void testAddMethodInFinally_WithoutFinallyBlockAndUsingExsitedMethod() throws Exception {
		MethodDeclaration md = null;
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "moveInstance");
		/** 
		 * try statement without finally block and using existing method, 
		 * expression statement is "fileOutputStream.close();"
		 */
		TryStatement tryStatement = (TryStatement)md.getBody().statements().get(0);
		Block finallyBlock = md.getAST().newBlock();
		tryStatement.setFinally(finallyBlock);
		
		Field currentMethodNode = CarelessCleanUpRefactor.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(refactor, md);

		Field cleanUpExpressionStatement = CarelessCleanUpRefactor.class.getDeclaredField("cleanUpExpressionStatement");
		cleanUpExpressionStatement.setAccessible(true);
		
		ExpressionStatement es = (ExpressionStatement)tryStatement.getBody().statements().remove(2);
		cleanUpExpressionStatement.set(refactor, es);
		refactor.setIsRefactoringMethodExist(true);
		
		Field existingMethod = CarelessCleanUpRefactor.class.getDeclaredField("existingMethod");
		existingMethod.setAccessible(true);
		IMethod method = RuntimeEnvironmentProjectReader.getType(javaProjectMaker.getProjectName(), CarelessCleanupExample.class.getPackage().getName(), CarelessCleanupExample.class.getSimpleName()).getMethod("closeStreamWithoutThrowingExceptionBigTry", new String[]{"(Ljava.io.FileOutputStream;)V"});
		existingMethod.set(refactor, method);
		
		// check precondition
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=java.io.IOException.class)}) public void moveInstance() throws IOException {\n" +
						"  try {\n" +
						"    FileOutputStream fi=new FileOutputStream(\"\");\n" +
						"    fi.write(1);\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    throw e;\n" +
						"  }\n" +
						" finally {\n" +
						"  }\n" +
						"}\n", md.toString());
		assertEquals("fi.close();\n", es.toString());
		// test
		Method addMethodInFinally = CarelessCleanUpRefactor.class.getDeclaredMethod("addMethodInFinally", AST.class, Block.class);
		addMethodInFinally.setAccessible(true);
		fail("Because this test case use private method \"createCallerMethod\", which is not tested yet, so we don't test it.");
		addMethodInFinally.invoke(refactor, md.getAST(), tryStatement.getFinally());
		// check postcondition
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=java.io.IOException.class)}) public void moveInstance() throws IOException {\n" +
						"  try {\n" +
						"    FileOutputStream fi=new FileOutputStream(\"\");\n" +
						"    fi.write(1);\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    throw e;\n" +
						"  }\n" +
						" finally {\n" +
						"    closeStreamWithoutThrowingExceptionBigTry(fi);\n" +
						"  }\n" +
						"}\n", md.toString());
	}
	
//	@Test
	public void testExtractMethod() throws Exception {
		fail("");
	}
	
//	@Test
	public void testApplyChange() throws Exception {
		fail("");
	}
	
//	@Test
	public void testCreateChange() throws Exception {
		fail("");
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
