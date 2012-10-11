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

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.DummyHandlerVisitor;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.filemaker.exceptionBadSmells.NestedTryStatementExample;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.jdom.Element;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RethrowExRefactoringTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javapProjectMaker;
	CompilationUnit compilationUnit;
	RethrowExRefactoring refactoring;
	String testProjectName;
	Path dummyHandlerExamplePath;
	
	public RethrowExRefactoringTest() {
		testProjectName = "RethrowExRefactoringTestProject";
		dummyHandlerExamplePath = new Path(testProjectName + "/"
				+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(DummyAndIgnoreExample.class.getName())
				+ JavaProjectMaker.JAVA_FILE_EXTENSION);
	}

	@Before
	public void setUp() throws Exception {
		// 讀取測試檔案樣本內容
		javaFile2String = new JavaFileToString();
		javaFile2String.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		
		javapProjectMaker = new JavaProjectMaker(testProjectName);
		javapProjectMaker.setJREDefaultContainer();
		// 新增欲載入的library
		javapProjectMaker.addJarFromProjectToBuildPath("lib/log4j-1.2.15.jar");
		// 根據測試檔案樣本內容建立新的檔案
		javapProjectMaker.createJavaFile(
				DummyAndIgnoreExample.class.getPackage().getName(),
				DummyAndIgnoreExample.class.getSimpleName()	+ JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + DummyAndIgnoreExample.class.getPackage().getName()
				+ ";\n" + javaFile2String.getFileContent());
		// 建立Nested try block example file
		javaFile2String = new JavaFileToString();
		javaFile2String.read(NestedTryStatementExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javapProjectMaker.createJavaFile(
				NestedTryStatementExample.class.getPackage().getName(),
				NestedTryStatementExample.class.getSimpleName()	+ JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package "+ NestedTryStatementExample.class.getPackage().getName()
				+";\n" + javaFile2String.getFileContent());
		
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath)));
		parser.setResolveBindings(true);
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
//		unit.recordModifications();
		
		refactoring = new RethrowExRefactoring();
	}

	@After
	public void tearDown() throws Exception {
		File xmlSettingFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(xmlSettingFile.exists()) {
			xmlSettingFile.delete();
		}
		
		// 刪除專案
		javapProjectMaker.deleteProject();
		
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		if(xmlFile.exists()) {
			xmlFile.delete();
//			fail("舊版設定檔不應該存在");
		}
	}
	
	@Test
	public void testGetThrowStatementSourceLine() throws Exception {
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(refactoring, compilationUnit);
		
		Field currentMethodNode = RethrowExRefactoring.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(refactoring, ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_systemOutAndPrintStack"));
		
		Method getThrowStatementSourceLine = RethrowExRefactoring.class.getDeclaredMethod("getThrowStatementSourceLine", int.class);
		getThrowStatementSourceLine.setAccessible(true);
		/** 未反白到try-catch block時為-1 */
		assertEquals(-1, getThrowStatementSourceLine.invoke(refactoring, -1));
		/** 反白到的catch中，沒有throw statement */
		assertEquals(-1, getThrowStatementSourceLine.invoke(refactoring, 0));
		/** 反白到try-catch block且catch中有throw statement */
		currentMethodNode.set(refactoring, ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "false_throwAndSystemOut"));
		assertEquals(208-1, getThrowStatementSourceLine.invoke(refactoring, 0));
	}
	
	@Test
	public void testFindMethod() throws Exception {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath));

		Field problem = RethrowExRefactoring.class.getDeclaredField("problem");
		problem.setAccessible(true);
		problem.set(refactoring, RLMarkerAttribute.CS_INGNORE_EXCEPTION);
		
		refactoring.methodIdx = "1";
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.open(null);
		
		Method findMethod = RethrowExRefactoring.class.getDeclaredMethod("findMethod", IResource.class);
		findMethod.setAccessible(true);
		/** 傳入的resource不是IFile type */
		assertFalse((Boolean)findMethod.invoke(refactoring, javaProject.getResource()));
		/** 傳入的resource是IFile type 且problem是ignore exception*/
		assertTrue((Boolean)findMethod.invoke(refactoring, javaElement.getResource()));
		/** 傳入的resource是IFile type 且problem是dummy handler*/
		problem.set(refactoring, RLMarkerAttribute.CS_DUMMY_HANDLER);
		assertTrue((Boolean)findMethod.invoke(refactoring, javaElement.getResource()));
	}
	
//	@Test
	public void testSelectSourceLine() throws Exception {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath));
		IMarker tempMarker = javaElement.getResource().createMarker("test.test");
		
		Field marker = RethrowExRefactoring.class.getDeclaredField("marker");
		marker.setAccessible(true);
		marker.set(refactoring, tempMarker);
		
		Field problem = RethrowExRefactoring.class.getDeclaredField("problem");
		problem.setAccessible(true);
		problem.set(refactoring, RLMarkerAttribute.CS_INGNORE_EXCEPTION);
		
		refactoring.methodIdx = "16";
		refactoring.catchIdx = 0;
		
		Method selectSourceLine = RethrowExRefactoring.class.getDeclaredMethod("selectSourceLine");
		selectSourceLine.setAccessible(true);
		// FIXME - 因為ITextEditor在unit test中，目前不知道如何focus，故此測試會Error
		assertTrue("因為不知道怎麼Focus一個ITextEditor", (Boolean)selectSourceLine.invoke(refactoring));
	}
	
//	@Test
	public void testChangeAnnotation() throws Exception {
		// FIXME - 由於會使用到selectSourceLine，故一樣有問題，測試等有解時，再補測
		fail("not implement");
	}
	
	@Test
	public void testSetExceptionName() throws Exception {
		Field exceptionType = RethrowExRefactoring.class.getDeclaredField("exceptionType");
		exceptionType.setAccessible(true);
		assertNull(exceptionType.get(refactoring));
		
		RefactoringStatus result = (RefactoringStatus)refactoring.setExceptionName("");
		assertEquals(	"<FATALERROR\n" +
						"\t\n" +
						"FATALERROR: Please Choose an Exception Type\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n" +
						">", result.toString());
		
		result = (RefactoringStatus)refactoring.setExceptionName("Dummy_handler");
		assertEquals("Dummy_handler", exceptionType.get(refactoring));
		assertEquals("<OK\n>", result.toString());
	}
	
	@Test
	public void testAddImportRLDeclaration() throws Exception {
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(refactoring, compilationUnit);
		
		List<?> imports = compilationUnit.imports();
		assertEquals(6, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", imports.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(5).toString());
		
		/** 第一次import，故會import RL和Robustness */
		Method addImportRLDeclaration = RethrowExRefactoring.class.getDeclaredMethod("addImportRLDeclaration");
		addImportRLDeclaration.setAccessible(true);
		addImportRLDeclaration.invoke(refactoring);
		
		imports = compilationUnit.imports();
		assertEquals(8, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", imports.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(5).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.Robustness;\n", imports.get(6).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.RTag;\n", imports.get(7).toString());
		
		/** 第二次import，RL和Robustness已經有了，故不會再import一次 */
		addImportRLDeclaration.invoke(refactoring);
		
		imports = compilationUnit.imports();
		assertEquals(8, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", imports.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(5).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.Robustness;\n", imports.get(6).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.RTag;\n", imports.get(7).toString());
	}
	
	@Test
	public void testAddImportDeclaration() throws Exception {
		/* 參數設定 */
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(refactoring, compilationUnit);
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		IType exType = JavaCore.create(project).findType("java.io.IOException");
		refactoring.setExType(exType);
		
		/** 給予已存在的import則不重複import */
		
		/* 執行測試對象 */
		Method addImportDeclaration = RethrowExRefactoring.class.getDeclaredMethod("addImportDeclaration");
		addImportDeclaration.setAccessible(true);
		addImportDeclaration.invoke(refactoring);
		
		/* 驗證結果 */
		List<?> imports = compilationUnit.imports();
		assertEquals(6, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", imports.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(5).toString());
		
		/** 給予新的import則必須import */
		exType = JavaCore.create(project).findType("java.io.IOError");
		refactoring.setExType(exType);
		addImportDeclaration.invoke(refactoring);
		
		/* 驗證結果 */
		imports = compilationUnit.imports();
		assertEquals(7, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", imports.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(5).toString());
		assertEquals("import java.io.IOError;\n", imports.get(6).toString());
	}
	
	@Test
	public void testGetRLAnnotation() throws Exception {
		Method getRLAnnotation = RethrowExRefactoring.class.getDeclaredMethod("getRLAnnotation", AST.class, int.class, String.class);
		getRLAnnotation.setAccessible(true);
		assertEquals("@RTag(level=2,exception=RuntimeException.class)", getRLAnnotation.invoke(refactoring, compilationUnit.getAST(), 2, "RuntimeException").toString());
	}
	
	@Test
	public void testAddAnnotationRoot() throws Exception {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath));
		
		refactoring.methodIdx = "1";
		
		Field problem = RethrowExRefactoring.class.getDeclaredField("problem");
		problem.setAccessible(true);
		problem.set(refactoring, RLMarkerAttribute.CS_DUMMY_HANDLER);
		
		Method findMethod = RethrowExRefactoring.class.getDeclaredMethod("findMethod", IResource.class);
		findMethod.setAccessible(true);
		findMethod.invoke(refactoring, javaElement.getResource());
		
		assertEquals("<OK\n>", refactoring.setExceptionName("RuntimeException").toString());
		
		Field currentMethodNode = RethrowExRefactoring.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		ASTNode node = (ASTNode)currentMethodNode.get(refactoring);
		
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		CompilationUnit root = (CompilationUnit)actRoot.get(refactoring);
		// 檢查precondition
		assertEquals(6, root.imports().size());
		/** 第一次import Tag */
		Method addAnnotationRoot = RethrowExRefactoring.class.getDeclaredMethod("addAnnotationRoot", AST.class);
		addAnnotationRoot.setAccessible(true);
		addAnnotationRoot.invoke(refactoring, node.getAST());
		// 驗證結果
		assertEquals(8, root.imports().size());
		
		/** 第二次import Tag，已經存在則不重複import */
		ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(root, node.getStartPosition(), 0);
		node.accept(exVisitor);
		List<?> rlList = exVisitor.getExceptionList();
		
		Field currentMethodRLList = RethrowExRefactoring.class.getDeclaredField("currentMethodRLList");
		currentMethodRLList.setAccessible(true);
		currentMethodRLList.set(refactoring, rlList);
		
		addAnnotationRoot.invoke(refactoring, node.getAST());
		assertEquals(8, root.imports().size());
	}
	
	@Test
	public void testDeleteStatement() throws Exception {
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		compilationUnit.accept(catchCollector);
		List<CatchClause> catchList = catchCollector.getMethodList();
		
		Method deleteStatement = RethrowExRefactoring.class.getDeclaredMethod("deleteStatement", List.class);
		deleteStatement.setAccessible(true);
		
		for(int i = 0; i < catchList.size(); i++) {
			List<Statement> statements = catchList.get(i).getBody().statements();
			deleteStatement.invoke(refactoring, statements);
			// 經過deleteStatement method之後，如果還有剩下statement的話，可能是一些其他邏輯
			if(statements.size() > 0) {
				for(int j = 0; j < statements.size(); j++) {
					// 判斷ExpressionStatement不應該為System.out.print、printStackTrace、System.err.print
					if(statements.get(j).getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
						assertFalse(((ExpressionStatement)statements.get(j)).getExpression().toString().contains("System.out.print"));
						assertFalse(((ExpressionStatement)statements.get(j)).getExpression().toString().contains("printStackTrace"));
						assertFalse(((ExpressionStatement)statements.get(j)).getExpression().toString().contains("System.err.print"));
					}
				}
			}
		}
	}
	
	@Test
	public void testAddThrowStatement() throws Exception {
		/* 選定要quick fix的method */
		
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath));
		
		refactoring.methodIdx = "6";
		
		Field problem = RethrowExRefactoring.class.getDeclaredField("problem");
		problem.setAccessible(true);
		problem.set(refactoring, RLMarkerAttribute.CS_DUMMY_HANDLER);
		
		Method findMethod = RethrowExRefactoring.class.getDeclaredMethod("findMethod", IResource.class);
		findMethod.setAccessible(true);
		findMethod.invoke(refactoring, javaElement.getResource());
		
		Field exceptionType = RethrowExRefactoring.class.getDeclaredField("exceptionType");
		exceptionType.setAccessible(true);
		exceptionType.set(refactoring, "RuntimeException");
		
		Field currentMethodNodeField = RethrowExRefactoring.class.getDeclaredField("currentMethodNode");
		currentMethodNodeField.setAccessible(true);
		ASTNode currentMethodNode = (ASTNode)currentMethodNodeField.get(refactoring);
		// 驗證是否抓到預想中的method
		assertEquals(	"public void true_systemErrPrint(){\n" +
						"  FileInputStream fis=null;\n" +
						"  try {\n" +
						"    fis=new FileInputStream(\"\");\n" +
						"    fis.read();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    System.err.println(e);\n" +
						"  }\n" +
						"}\n", currentMethodNode.toString());
		
		/* add throw statement */
		Method addThrowStatement = RethrowExRefactoring.class.getDeclaredMethod("addThrowStatement", CatchClause.class, AST.class);
		addThrowStatement.setAccessible(true);
		// 取得該method的catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		addThrowStatement.invoke(refactoring, catchCollector.getMethodList().get(0), currentMethodNode.getAST());
		// 驗證是否加入throw statement以及刪除System.err.println(e)
		assertEquals(	"public void true_systemErrPrint(){\n" +
						"  FileInputStream fis=null;\n" +
						"  try {\n" +
						"    fis=new FileInputStream(\"\");\n" +
						"    fis.read();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    throw new RuntimeException(e);\n" +
						"  }\n" +
						"}\n", currentMethodNode.toString());
	}
	
	@Test
	public void testCheckMethodThrow() throws Exception {
		Method checkMethodThrow = RethrowExRefactoring.class.getDeclaredMethod("checkMethodThrow", AST.class);
		checkMethodThrow.setAccessible(true);
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<?> methodList = methodCollector.getMethodList();
		ASTNode node = (ASTNode)methodList.get(16);
		
		Field currentMethodNode = RethrowExRefactoring.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(refactoring, node);
		
		List<?> exceptionList = ((MethodDeclaration)node).thrownExceptions();
		
		/** method會拋出指定的exception type的情況，此時就不必把exception type加入到這method的資訊中*/
		refactoring.setExceptionName("IOException");
		// 檢查precondition
		assertEquals(1, exceptionList.size());
		assertEquals("IOException", exceptionList.get(0).toString());
		
		checkMethodThrow.invoke(refactoring, node.getAST());
		
		// 驗證結果
		exceptionList = ((MethodDeclaration)node).thrownExceptions();
		assertEquals(1, exceptionList.size());
		assertEquals("IOException", exceptionList.get(0).toString());
		
		/** method不會拋出指定的exception type的情況，此時就必須把exception type加入到這method的資訊中 */
		refactoring.setExceptionName("RuntimeException");
		// 檢查precondition
		assertEquals(1, exceptionList.size());
		assertEquals("IOException", exceptionList.get(0).toString());
		
		checkMethodThrow.invoke(refactoring, node.getAST());
		
		// 驗證結果
		exceptionList = ((MethodDeclaration)node).thrownExceptions();
		assertEquals(2, exceptionList.size());
		assertEquals("IOException", exceptionList.get(0).toString());
		assertEquals("RuntimeException", exceptionList.get(1).toString());
	}
	
//	@Test
	public void testRethrowException() throws Exception {
		CreateDummyHandlerXML();
		
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath));
		IMarker tempMarker = javaElement.getResource().createMarker("test.test");
		tempMarker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, "0");
		
		Field actOpenable = RethrowExRefactoring.class.getDeclaredField("actOpenable");
		actOpenable.setAccessible(true);
		actOpenable.set(refactoring, (IOpenable)javaElement);
		
		Field marker = RethrowExRefactoring.class.getDeclaredField("marker");
		marker.setAccessible(true);
		marker.set(refactoring, tempMarker);
		
		Field problem = RethrowExRefactoring.class.getDeclaredField("problem");
		problem.setAccessible(true);
		problem.set(refactoring, RLMarkerAttribute.CS_DUMMY_HANDLER);
		
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(refactoring, compilationUnit);
		
		refactoring.setExceptionName("RuntimeException");
		
		ASTNode node = (ASTNode)ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_printStackTrace_public");
		
		ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(compilationUnit, node.getStartPosition(), 0);
		node.accept(exVisitor);
		
		Field currentMethodRLList = RethrowExRefactoring.class.getDeclaredField("currentMethodRLList");
		currentMethodRLList.setAccessible(true);
		currentMethodRLList.set(refactoring, exVisitor.getMethodRLAnnotationList());
		
		Field currentMethodNode = RethrowExRefactoring.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(refactoring, node);
		
		DummyHandlerVisitor visitor = new DummyHandlerVisitor(compilationUnit);
		node.accept(visitor);
		Field currentExList = RethrowExRefactoring.class.getDeclaredField("currentExList");
		currentExList.setAccessible(true);
		currentExList.set(refactoring, visitor.getDummyList());
		
		// check precondition
		assertEquals(	"public void true_printStackTrace_public(){\n" +
						"  FileInputStream fis=null;\n" +
						"  try {\n" +
						"    fis=new FileInputStream(\"\");\n" +
						"    fis.read();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    e.printStackTrace();\n" +
						"  }\n" +
						"}\n", node.toString());
		
		Method rethrowException = RethrowExRefactoring.class.getDeclaredMethod("rethrowException");
		rethrowException.setAccessible(true);
		rethrowException.invoke(refactoring);
		
		// verify postcondition
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=RuntimeException.class)}) " +
						"public void true_printStackTrace_public(){\n" +
						"  FileInputStream fis=null;\n" +
						"  try {\n" +
						"    fis=new FileInputStream(\"\");\n" +
						"    fis.read();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    throw new RuntimeException(e);\n" +
						"  }\n" +
						"}\n", node.toString());
	}
	
//	@Test
	public void testCollectChange() throws Exception {
		CreateDummyHandlerXML();
		
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath));
		IMarker tempMarker = javaElement.getResource().createMarker("test.test");
		tempMarker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE, RLMarkerAttribute.CS_DUMMY_HANDLER);
		tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, "1");
		tempMarker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, "0");
		
		Field marker = RethrowExRefactoring.class.getDeclaredField("marker");
		marker.setAccessible(true);
		marker.set(refactoring, tempMarker);
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		IType exType = JavaCore.create(project).findType("java.io.IOError");
		refactoring.setExType(exType);
		refactoring.setExceptionName("IOError");
		
		/** check precondition */
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<?> methodList = methodCollector.getMethodList();
		ASTNode node = (ASTNode)methodList.get(1);
		// 檢查選取到的method
		assertEquals(	"public void true_printStackTrace_public(){\n" +
						"  FileInputStream fis=null;\n" +
						"  try {\n" +
						"    fis=new FileInputStream(\"\");\n" +
						"    fis.read();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    e.printStackTrace();\n" +
						"  }\n" +
						"}\n", node.toString());
		// 檢查import的數量以及名稱
		List<?> imports = compilationUnit.imports();
		assertEquals(6, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", imports.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(5).toString());
		
		Method collectChange = RethrowExRefactoring.class.getDeclaredMethod("collectChange", IResource.class);
		collectChange.setAccessible(true);
		collectChange.invoke(refactoring, tempMarker.getResource());
		
		/** verify postcondition */
		Field currentMethodNode = RethrowExRefactoring.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		// 驗證選取的method的改變
		assertEquals(	"@Robustness(value={@RTag(level=1,exception=IOError.class)}) " +
						"public void true_printStackTrace_public() throws IOError {\n" +
						"  FileInputStream fis=null;\n" +
						"  try {\n" +
						"    fis=new FileInputStream(\"\");\n" +
						"    fis.read();\n" +
						"  }\n" +
						" catch (  IOException e) {\n" +
						"    throw new IOError(e);\n" +
						"  }\n" +
						"}\n", currentMethodNode.get(refactoring).toString());
		// 驗證import數量以及名稱
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		List<?> newImports = ((CompilationUnit)actRoot.get(refactoring)).imports(); 
		assertEquals(9, newImports.size());
		assertEquals("import java.io.FileInputStream;\n", newImports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", newImports.get(1).toString());
		assertEquals("import java.io.IOException;\n", newImports.get(2).toString());
		assertEquals("import java.util.ArrayList;\n", imports.get(3).toString());
		assertEquals("import java.util.logging.Level;\n", newImports.get(4).toString());
		assertEquals("import org.apache.log4j.Logger;\n", newImports.get(5).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.Robustness;\n", newImports.get(6).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.RTag;\n", newImports.get(7).toString());
		assertEquals("import java.io.IOError;\n", newImports.get(8).toString());
	}
	
//	@Test
	public void testCreateChange() throws Exception {
		CreateDummyHandlerXML();
		
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath));
		IMarker tempMarker = javaElement.getResource().createMarker("test.test");
		tempMarker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE, RLMarkerAttribute.CS_DUMMY_HANDLER);
		tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, "1");
		tempMarker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, "0");
		
		Field marker = RethrowExRefactoring.class.getDeclaredField("marker");
		marker.setAccessible(true);
		marker.set(refactoring, tempMarker);
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		IType exType = JavaCore.create(project).findType("java.io.IOError");
		refactoring.setExType(exType);
		refactoring.setExceptionName("IOError");
		
		Method collectChange = RethrowExRefactoring.class.getDeclaredMethod("collectChange", IResource.class);
		collectChange.setAccessible(true);
		collectChange.invoke(refactoring, tempMarker.getResource());
		
		Change result = refactoring.createChange(null);
		assertEquals("Rethrow Unchecked Exception", result.getName());
		assertEquals(	"DummyAndIgnoreExample.java [in ntut.csie.exceptionBadSmells [in src [in DummyHandlerTest]]]\n" +
						"  package ntut.csie.exceptionBadSmells\n" +
						"  import java.io.FileInputStream\n" +
						"  import java.io.FileNotFoundException\n" +
						"  import java.io.IOException\n" +
						"  import java.util.logging.Level\n" +
						"  import org.apache.log4j.Logger\n" +
						"  class DummyAndIgnoreExample\n" +
						"    Logger log4j\n" +
						"    java.util.logging.Logger javaLog\n" +
						"    DummyAndIgnoreExample()\n" +
						"    void true_printStackTrace_public()\n" +
						"    void test()\n" +
						"    void true_printStackTrace_protected()\n" +
						"    void true_printStackTrace_private()\n" +
						"    void true_systemTrace()\n" +
						"    void true_systemErrPrint()\n" +
						"    void true_systemOutPrint()\n" +
						"    void true_systemOutPrintlnWithE()\n" +
						"    void true_systemOutPrintlnWithoutE()\n" +
						"    void true_systemOutAndPrintStack()\n" +
						"    void true_Log4J()\n" +
						"    void true_javaLogInfo()\n" +
						"    void true_javaLogDotLog()\n" +
						"    void true_DummyHandlerInNestedTry()\n" +
						"    void false_IgnoredException()\n" +
						"    void false_throwAndPrint()\n" +
						"    void false_throwAndSystemOut()\n" +
						"    void false_rethrowRuntimeException()\n" +
						"    void false_systemOut()\n" +
						"    void false_systemOutNotInTryStatement()", result.getModifiedElement().toString());
		
	}
	
	@Test
	public void testCheckInitialConditions() throws Exception {
		RefactoringStatus result = refactoring.checkInitialConditions(null);
		assertEquals("<OK\n>", result.toString());
	}
	
	@Test
	public void testCheckFinalConditions() throws Exception {
		CreateDummyHandlerXML();
		
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath));
		IMarker tempMarker = javaElement.getResource().createMarker("test.test");
		tempMarker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE, RLMarkerAttribute.CS_DUMMY_HANDLER);
		tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, "1");
		tempMarker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, "0");
		
		Field marker = RethrowExRefactoring.class.getDeclaredField("marker");
		marker.setAccessible(true);
		marker.set(refactoring, tempMarker);
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		IType exType = JavaCore.create(project).findType("java.io.IOError");
		refactoring.setExType(exType);
		refactoring.setExceptionName("IOError");
		
		RefactoringStatus result = refactoring.checkInitialConditions(null);
		assertEquals("<OK\n>", result.toString());
	}
	
	@Test
	public void testApplyChange() throws Exception {
		compilationUnit.recordModifications();
		
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath));
		
		Field actOpenable = RethrowExRefactoring.class.getDeclaredField("actOpenable");
		actOpenable.setAccessible(true);
		actOpenable.set(refactoring, (IOpenable)javaElement);
		
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(refactoring, compilationUnit);
		
		// 檢查precondition
		Field textFileChange = RethrowExRefactoring.class.getDeclaredField("textFileChange");
		textFileChange.setAccessible(true);
		TextFileChange text = (TextFileChange)textFileChange.get(refactoring); 
		assertNull(text);
		// 執行測試對象
		Method applyChange = RethrowExRefactoring.class.getDeclaredMethod("applyChange");
		applyChange.setAccessible(true);
		applyChange.invoke(refactoring);
		// 驗證結果
		text = (TextFileChange)textFileChange.get(refactoring);
		assertNotNull(text);
	}
	
	/**
	 * 建立CSPreference.xml檔案
	 */
	private void CreateDummyHandlerXML() {
		//取的XML的root
		Element root = JDomUtil.createXMLContent();

		//建立Dummy Handler的Tag
		Element dummyHandler = new Element(JDomUtil.DummyHandlerTag);
		Element rule = new Element("rule");
		//假如e.printStackTrace有被勾選起來
		rule.setAttribute(JDomUtil.e_printstacktrace,"Y");

		//假如system.out.println有被勾選起來
		rule.setAttribute(JDomUtil.systemout_print,"Y");
		
		rule.setAttribute(JDomUtil.apache_log4j,"Y");
		rule.setAttribute(JDomUtil.java_Logger,"Y");

		//把使用者自訂的Rule存入XML
		Element libRule = new Element("librule");
		
		//將新建的tag加進去
		dummyHandler.addContent(rule);
		dummyHandler.addContent(libRule);

		if (root.getChild(JDomUtil.DummyHandlerTag) != null)
			root.removeChild(JDomUtil.DummyHandlerTag);

		root.addContent(dummyHandler);

		//將檔案寫回
		String path = JDomUtil.getWorkspace() + File.separator + "CSPreference.xml";
		JDomUtil.OutputXMLFile(root.getDocument(), path);
	}
}
