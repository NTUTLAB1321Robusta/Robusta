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

import ntut.csie.analyzer.ASTCatchCollect;
import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.CommonExample;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.dummy.DummyHandlerVisitor;
import ntut.csie.analyzer.nested.NestedTryStatementExample;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.util.PathUtils;

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
	private SmellSettings smellSettings;
	
	public RethrowExRefactoringTest() {
		testProjectName = "RethrowExRefactoringTestProject";
		dummyHandlerExamplePath = new Path(testProjectName + "/"
				+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(CommonExample.class.getName())
				+ JavaProjectMaker.JAVA_FILE_EXTENSION);
	}

	@Before
	public void setUp() throws Exception {
		
		javaFile2String = new JavaFileToString();
		javaFile2String.read(CommonExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		
		javapProjectMaker = new JavaProjectMaker(testProjectName);
		javapProjectMaker.setJREDefaultContainer();
		
		javapProjectMaker
				.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR
						+ "/log4j-1.2.15.jar");
		
		javapProjectMaker.createJavaFile(
				CommonExample.class.getPackage().getName(),
				CommonExample.class.getSimpleName()	+ JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + CommonExample.class.getPackage().getName()
				+ ";\n" + javaFile2String.getFileContent());
		// establish Nested try statement example file
		javaFile2String = new JavaFileToString();
		javaFile2String.read(NestedTryStatementExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javapProjectMaker.createJavaFile(
				NestedTryStatementExample.class.getPackage().getName(),
				NestedTryStatementExample.class.getSimpleName()	+ JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package "+ NestedTryStatementExample.class.getPackage().getName()
				+";\n" + javaFile2String.getFileContent());
		
		
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath)));
		parser.setResolveBindings(true);
		
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
		
		
		javapProjectMaker.deleteProject();
	}
	
	@Test
	public void testGetThrowStatementSourceLine() throws Exception {
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("javaFileWillBeRefactored");
		actRoot.setAccessible(true);
		actRoot.set(refactoring, compilationUnit);
		
		Field currentMethodNode = RethrowExRefactoring.class.getDeclaredField("methodNodeWillBeRefactored");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(refactoring, ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_systemOutPrint"));
		
		Method getThrowStatementSourceLine = RethrowExRefactoring.class.getDeclaredMethod("getLineNumberOfThrowExceptionStatement", int.class);
		getThrowStatementSourceLine.setAccessible(true);
		/** if high light boundary does not cover try-catch block, this method will return -1;  */
		assertEquals(-1, getThrowStatementSourceLine.invoke(refactoring, -1));
		/** if high light boundary covers catch statement but not covers throw statement, this method will return -1 */
		assertEquals(-1, getThrowStatementSourceLine.invoke(refactoring, 0));
		/** if high light boundary covers try-catch statement and throw statement, this method will return line number */
		currentMethodNode.set(refactoring, ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "false_throwAndSystemOut"));
		assertEquals(196-1, getThrowStatementSourceLine.invoke(refactoring, 0));
	}
	
	@Test
	public void testFindMethod() throws Exception {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath));

		Field problem = RethrowExRefactoring.class.getDeclaredField("badSmellType");
		problem.setAccessible(true);
		problem.set(refactoring, RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK);
		
		refactoring.methodIdx = "1";
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		IJavaProject javaProject = JavaCore.create(project);
		javaProject.open(null);
		
		Method findMethod = RethrowExRefactoring.class.getDeclaredMethod("findMethod", IResource.class);
		findMethod.setAccessible(true);
		/** the type of input resource is not IFile */
		assertFalse((Boolean)findMethod.invoke(refactoring, javaProject.getResource()));
		/** the type of input resource is IFile and problem is empty catch block exception*/
		assertTrue((Boolean)findMethod.invoke(refactoring, javaElement.getResource()));
		/** the type of input resource is IFile and problem is dummy handler*/
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
		
		Field problem = RethrowExRefactoring.class.getDeclaredField("badSmellType");
		problem.setAccessible(true);
		problem.set(refactoring, RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK);
		
		refactoring.methodIdx = "16";
		refactoring.catchIdx = 0;
		
		Method selectSourceLine = RethrowExRefactoring.class.getDeclaredMethod("selectSourceLine");
		selectSourceLine.setAccessible(true);
		// FIXME - we don't know how to get focus of ITextEditor during unit testing, so this test would fail currently.
		assertTrue(" we don't know how to get focus of ITextEditor during unit testing", (Boolean)selectSourceLine.invoke(refactoring));
	}
	
//	@Test
	public void testChangeAnnotation() throws Exception {
		// FIXME - this test will use selectSourceLine, so we can not test temporarily.
		fail("not implement");
	}
	
	@Test
	public void testSetExceptionName() throws Exception {
		Field exceptionType = RethrowExRefactoring.class.getDeclaredField("exceptionTypeWillBeRethrown");
		exceptionType.setAccessible(true);
		assertNull(exceptionType.get(refactoring));
		
		RefactoringStatus result = (RefactoringStatus)refactoring.setExceptionType("");
		assertEquals(	"<FATALERROR\n" +
						"\t\n" +
						"FATALERROR: Please Choose an Exception Type\n" +
						"Context: <Unspecified context>\n" +
						"code: none\n" +
						"Data: null\n" +
						">", result.toString());
		
		result = (RefactoringStatus)refactoring.setExceptionType("Dummy_handler");
		assertEquals("Dummy_handler", exceptionType.get(refactoring));
		assertEquals("<OK\n>", result.toString());
	}
	
	@Test
	public void testAddImportRLDeclaration() throws Exception {
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("javaFileWillBeRefactored");
		actRoot.setAccessible(true);
		actRoot.set(refactoring, compilationUnit);
		
		List<?> imports = compilationUnit.imports();
		assertEquals(5, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(3).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(4).toString());
		
		/** when first time import library, RL and Robustness will be imported*/
		Method addImportRLDeclaration = RethrowExRefactoring.class.getDeclaredMethod("importRobuatnessLevelLibrary");
		addImportRLDeclaration.setAccessible(true);
		addImportRLDeclaration.invoke(refactoring);
		
		imports = compilationUnit.imports();
		assertEquals(7, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(3).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(4).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.Robustness;\n", imports.get(5).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.RTag;\n", imports.get(6).toString());
		
		/** when second time import library, RL and Robustness will not be imported due to they have been imported */
		addImportRLDeclaration.invoke(refactoring);
		
		imports = compilationUnit.imports();
		assertEquals(7, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(3).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(4).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.Robustness;\n", imports.get(5).toString());
		assertEquals("import ntut.csie.robusta.agile.exception.RTag;\n", imports.get(6).toString());
	}
	
	@Test
	public void testAddImportDeclaration() throws Exception {
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("javaFileWillBeRefactored");
		actRoot.setAccessible(true);
		actRoot.set(refactoring, compilationUnit);
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(testProjectName);
		IType exType = JavaCore.create(project).findType("java.io.IOException");
		refactoring.setUserSelectingExceptionType(exType);
		
		/** import the library which has already imported */
		
		Method addImportDeclaration = RethrowExRefactoring.class.getDeclaredMethod("importExceptionLibrary");
		addImportDeclaration.setAccessible(true);
		addImportDeclaration.invoke(refactoring);
		
		List<?> imports = compilationUnit.imports();
		assertEquals(5, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(3).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(4).toString());
		
		/** import the library which has not already imported */
		exType = JavaCore.create(project).findType("java.io.IOError");
		refactoring.setUserSelectingExceptionType(exType);
		addImportDeclaration.invoke(refactoring);
		
		imports = compilationUnit.imports();
		assertEquals(6, imports.size());
		assertEquals("import java.io.FileInputStream;\n", imports.get(0).toString());
		assertEquals("import java.io.FileNotFoundException;\n", imports.get(1).toString());
		assertEquals("import java.io.IOException;\n", imports.get(2).toString());
		assertEquals("import java.util.logging.Level;\n", imports.get(3).toString());
		assertEquals("import org.apache.log4j.Logger;\n", imports.get(4).toString());
		assertEquals("import java.io.IOError;\n", imports.get(5).toString());
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
		
		Field problem = RethrowExRefactoring.class.getDeclaredField("badSmellType");
		problem.setAccessible(true);
		problem.set(refactoring, RLMarkerAttribute.CS_DUMMY_HANDLER);
		
		Method findMethod = RethrowExRefactoring.class.getDeclaredMethod("findMethod", IResource.class);
		findMethod.setAccessible(true);
		findMethod.invoke(refactoring, javaElement.getResource());
		
		assertEquals("<OK\n>", refactoring.setExceptionType("RuntimeException").toString());
		
		Field currentMethodNode = RethrowExRefactoring.class.getDeclaredField("methodNodeWillBeRefactored");
		currentMethodNode.setAccessible(true);
		ASTNode node = (ASTNode)currentMethodNode.get(refactoring);
		
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("javaFileWillBeRefactored");
		actRoot.setAccessible(true);
		CompilationUnit root = (CompilationUnit)actRoot.get(refactoring);
		// check precondition
		assertEquals(5, root.imports().size());
		/** first time import */
		Method addAnnotationRoot = RethrowExRefactoring.class.getDeclaredMethod("addAnnotationRoot", AST.class);
		addAnnotationRoot.setAccessible(true);
		addAnnotationRoot.invoke(refactoring, node.getAST());
		assertEquals(7, root.imports().size());
		
		/** second time import*/
		ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(root, node.getStartPosition(), 0);
		node.accept(exVisitor);
		List<?> rlList = exVisitor.getExceptionList();
		
		Field currentMethodRLList = RethrowExRefactoring.class.getDeclaredField("methodRobustnessLevelList");
		currentMethodRLList.setAccessible(true);
		currentMethodRLList.set(refactoring, rlList);
		
		addAnnotationRoot.invoke(refactoring, node.getAST());
		assertEquals(7, root.imports().size());
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
			//if there are some statement left after invoking deleteStatement. these statement should be some special logic statement.
			if(statements.size() > 0) {
				for(int j = 0; j < statements.size(); j++) {
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
		
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot().getFile(dummyHandlerExamplePath));
		
		refactoring.methodIdx = "6";
		
		Field problem = RethrowExRefactoring.class.getDeclaredField("badSmellType");
		problem.setAccessible(true);
		problem.set(refactoring, RLMarkerAttribute.CS_DUMMY_HANDLER);
		
		Method findMethod = RethrowExRefactoring.class.getDeclaredMethod("findMethod", IResource.class);
		findMethod.setAccessible(true);
		findMethod.invoke(refactoring, javaElement.getResource());
		
		Field exceptionType = RethrowExRefactoring.class.getDeclaredField("exceptionTypeWillBeRethrown");
		exceptionType.setAccessible(true);
		exceptionType.set(refactoring, "RuntimeException");
		
		Field currentMethodNodeField = RethrowExRefactoring.class.getDeclaredField("methodNodeWillBeRefactored");
		currentMethodNodeField.setAccessible(true);
		ASTNode currentMethodNode = (ASTNode)currentMethodNodeField.get(refactoring);
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
		Method addThrowStatement = RethrowExRefactoring.class.getDeclaredMethod("addThrowExceptionStatement", CatchClause.class, AST.class);
		addThrowStatement.setAccessible(true);
		// get currentMethodNode's catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		addThrowStatement.invoke(refactoring, catchCollector.getMethodList().get(0), currentMethodNode.getAST());
		// verify whether has added throw statement and has removed System.err.println(e)
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
		
		Field currentMethodNode = RethrowExRefactoring.class.getDeclaredField("methodNodeWillBeRefactored");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(refactoring, node);
		
		List<?> exceptionList = ((MethodDeclaration)node).thrownExceptions();
		
		//if a method will throw a specified exception type, it does not need to add the exception type into method's information. 
		refactoring.setExceptionType("IOException");
		// check precondition
		assertEquals(1, exceptionList.size());
		assertEquals("IOException", exceptionList.get(0).toString());
		
		checkMethodThrow.invoke(refactoring, node.getAST());
		
		exceptionList = ((MethodDeclaration)node).thrownExceptions();
		assertEquals(1, exceptionList.size());
		assertEquals("IOException", exceptionList.get(0).toString());
		
		//if a method will not throw a specified exception type, it need to add the exception type into method's information. 
		refactoring.setExceptionType("RuntimeException");
		// check precondition
		assertEquals(1, exceptionList.size());
		assertEquals("IOException", exceptionList.get(0).toString());
		
		checkMethodThrow.invoke(refactoring, node.getAST());
		
		exceptionList = ((MethodDeclaration)node).thrownExceptions();
		assertEquals(2, exceptionList.size());
		assertEquals("IOException", exceptionList.get(0).toString());
		assertEquals("RuntimeException", exceptionList.get(1).toString());
	}
	
//	@Test
	public void testRethrowException() throws Exception {
		CreateSettings();
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
		
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("methodNodeWillBeRefactored");
		actRoot.setAccessible(true);
		actRoot.set(refactoring, compilationUnit);
		
		refactoring.setExceptionType("RuntimeException");
		
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
		currentExList.set(refactoring, visitor.getDummyHandlerList());
		
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
		CreateSettings();
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
		refactoring.setUserSelectingExceptionType(exType);
		refactoring.setExceptionType("IOError");
		
		/** check precondition */
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<?> methodList = methodCollector.getMethodList();
		ASTNode node = (ASTNode)methodList.get(1);
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
		// check library name and amount which has been imported
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
		// check library name and amount which has been imported
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("methodNodeWillBeRefactored");
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
		CreateSettings();
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
		refactoring.setUserSelectingExceptionType(exType);
		refactoring.setExceptionType("IOError");
		
		Method collectChange = RethrowExRefactoring.class.getDeclaredMethod("collectChange", IResource.class);
		collectChange.setAccessible(true);
		collectChange.invoke(refactoring, tempMarker.getResource());
		
		Change result = refactoring.createChange(null);
		assertEquals("Rethrow Unchecked Exception", result.getName());
		assertEquals(	"DHExample.java [in ntut.csie.exceptionBadSmells [in src [in DummyHandlerTest]]]\n" +
						"  package ntut.csie.exceptionBadSmells\n" +
						"  import java.io.FileInputStream\n" +
						"  import java.io.FileNotFoundException\n" +
						"  import java.io.IOException\n" +
						"  import java.util.logging.Level\n" +
						"  import org.apache.log4j.Logger\n" +
						"  class DHExample\n" +
						"    Logger log4j\n" +
						"    java.util.logging.Logger javaLogger\n" +
						"    DHExample()\n" +
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
		CreateSettings();
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
		refactoring.setUserSelectingExceptionType(exType);
		refactoring.setExceptionType("IOError");
		
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
		
		Field actRoot = RethrowExRefactoring.class.getDeclaredField("javaFileWillBeRefactored");
		actRoot.setAccessible(true);
		actRoot.set(refactoring, compilationUnit);
		
		// check precondition
		Field textFileChange = RethrowExRefactoring.class.getDeclaredField("textFileChange");
		textFileChange.setAccessible(true);
		TextFileChange text = (TextFileChange)textFileChange.get(refactoring); 
		assertNull(text);

		Method applyChange = RethrowExRefactoring.class.getDeclaredMethod("applyChange");
		applyChange.setAccessible(true);
		applyChange.invoke(refactoring);

		text = (TextFileChange)textFileChange.get(refactoring);
		assertNotNull(text);
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
