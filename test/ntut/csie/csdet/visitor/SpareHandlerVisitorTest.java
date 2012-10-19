package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;

import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SpareHandlerVisitorTest {
	JavaFileToString javaaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "SpareHandlerTest";
		// 讀取測試檔案樣本內容
		javaaFile2String = new JavaFileToString();
		javaaFile2String.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();
		// 新增欲載入的library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		// 根據測試檔案樣本內容建立新的檔案
		javaProjectMaker.createJavaFile(DummyAndIgnoreExample.class.getPackage().getName(),
				DummyAndIgnoreExample.class.getSimpleName(),
				"package " + DummyAndIgnoreExample.class.getPackage().getName()	+ ";\n"
				+ javaaFile2String.getFileContent());
		
		Path dummyAndIgnoreExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(DummyAndIgnoreExample.class, testProjectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(dummyAndIgnoreExamplePath)));
		parser.setResolveBindings(true);
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		javaProjectMaker.deleteProject();
	}
	
	
	@Test
	public void testVisitNode_visitTryNode() {
		// 從Sample code裡面取出一個TryStatement
		SimpleTryStatementFinder simpleTryFinder = new SimpleTryStatementFinder("true_DummyHandlerCatchNestedTry");
		compilationUnit.accept(simpleTryFinder);
		
		// 使用者選擇的TryStatement與visitNode傳入的TryStatement是同一個
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(simpleTryFinder.getTryStatement());
		spareHandlerVisitor.visit(simpleTryFinder.getTryStatement());
		assertTrue(spareHandlerVisitor.getResult());
	}
	
	@Test
	public void testVisitNode_visitATryNodeAndSelectAnotherTryNode() {
		// 從Sample code裡面取出第一個TryStatement
		SimpleTryStatementFinder simpleTryFinderUserSelect = new SimpleTryStatementFinder("true_DummyHandlerCatchNestedTry");
		compilationUnit.accept(simpleTryFinderUserSelect);
		
		// 從Sample code裡面取出第二個TryStatement
		SimpleTryStatementFinder simpleTryFinderInputVisitNode = new SimpleTryStatementFinder("true_DummyHandlerTryNestedTry");
		compilationUnit.accept(simpleTryFinderInputVisitNode);
		
		// 使用者選擇的TryStatement與visitNode傳入的TryStatement是不同一個
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(simpleTryFinderUserSelect.getTryStatement());
		spareHandlerVisitor.visit(simpleTryFinderInputVisitNode.getTryStatement());
		assertFalse(spareHandlerVisitor.getResult());
	}	
	
	@Test
	public void testVisitNode_userNotSelectNode() {
		// 從Sample code裡面取出一個TryStatement節點
		SimpleTryStatementFinder simpleTryFinder = new SimpleTryStatementFinder("true_DummyHandlerCatchNestedTry");
		compilationUnit.accept(simpleTryFinder);
		
		// 使用者沒有選擇節點
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(null);
		spareHandlerVisitor.visit(simpleTryFinder.getTryStatement());
		assertFalse(spareHandlerVisitor.getResult());			
	}
	
	@Test
	public void testProcessTryStatement_CatchNestedTry() throws Exception {
		// 根據指定的 Method Name 找出我們要的TryNode
		SimpleTryStatementFinder simpleTryFinder = new SimpleTryStatementFinder("true_DummyHandlerCatchNestedTry");
		compilationUnit.accept(simpleTryFinder);
		
		// 開放processTryStatement的存取權限
		Method processTryStatement = SpareHandlerVisitor.class.getDeclaredMethod("processTryStatement", ASTNode.class);
		processTryStatement.setAccessible(true);
		
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(simpleTryFinder.getTryStatement());
		processTryStatement.invoke(spareHandlerVisitor, simpleTryFinder.getTryStatement());
		
		assertTrue(spareHandlerVisitor.getResult());
	}
	
	@Test
	public void testProcessTryStatement_TryNestedTry() throws Exception {
		// 根據指定的 Method Name 找出我們要的TryNode
		SimpleTryStatementFinder simpleTryFinder = new SimpleTryStatementFinder("true_DummyHandlerTryNestedTry");
		compilationUnit.accept(simpleTryFinder);
		
		// 開放processTryStatement的存取權限
		Method processTryStatement = SpareHandlerVisitor.class.getDeclaredMethod("processTryStatement", ASTNode.class);
		processTryStatement.setAccessible(true);
		
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(simpleTryFinder.getTryStatement());
		processTryStatement.invoke(spareHandlerVisitor, simpleTryFinder.getTryStatement());
		
		assertTrue(spareHandlerVisitor.getResult());
	}
	
	@Test
	public void testProcessTryStatement_TryWithoutNested() throws Exception {
		// 根據指定的 Method Name 找出我們要的TryNode
		SimpleTryStatementFinder simpleTryFinder = new SimpleTryStatementFinder("false_rethrowRuntimeException");
		compilationUnit.accept(simpleTryFinder);
		
		// 開放processTryStatement的存取權限
		Method processTryStatement = SpareHandlerVisitor.class.getDeclaredMethod("processTryStatement", ASTNode.class);
		processTryStatement.setAccessible(true);
		
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(simpleTryFinder.getTryStatement());
		processTryStatement.invoke(spareHandlerVisitor, simpleTryFinder.getTryStatement());
		
		assertTrue(spareHandlerVisitor.getResult());
	}

	public class SimpleTryStatementFinder extends ASTVisitor{
		private TryStatement tryStatement;
		private String nameOfMethodWithSpecifiedTryStatement;
		public SimpleTryStatementFinder(String nameOfMethodWithSpecifiedTry) {
			super();
			tryStatement = null;
			nameOfMethodWithSpecifiedTryStatement = nameOfMethodWithSpecifiedTry;
		}
		
		public boolean visit(MethodDeclaration node) {
			return node.getName().toString().equals(nameOfMethodWithSpecifiedTryStatement);
		}
		
		public boolean visit(TryStatement node) {
			tryStatement = node;
			return false;
		}
		
		public TryStatement getTryStatement() {
			return tryStatement;
		}
	}
}
