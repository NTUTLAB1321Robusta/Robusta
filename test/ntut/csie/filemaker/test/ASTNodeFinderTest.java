package ntut.csie.filemaker.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.jdt.util.testSampleCode.NodeUtilsTestSample;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ASTNodeFinderTest {
	JavaFileToString javaFileToString;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	String projectName;
	
	public ASTNodeFinderTest() {
		projectName = "ASTNodeFinderExampleProject";
	}
	
	@Before
	public void setUp() throws Exception {
		javaFileToString = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		javaFileToString.read(NodeUtilsTestSample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(NodeUtilsTestSample.class.getPackage().getName(),
				NodeUtilsTestSample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + NodeUtilsTestSample.class.getPackage().getName() + ";" + String.format("%n")
						+ javaFileToString.getFileContent());
		javaFileToString.clear();
		

		Path ccExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(NodeUtilsTestSample.class, projectName));
		
		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin
				.getWorkspace().getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}
	
	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testGetNodeFromSpecifiedClass() throws Exception {
		/**
		 * 略述
		 * lineNumber = 1 → 該目標 .java 全部 code
		 * lineNumber method → 整個 method code
		 * lineNumber class → 整個 class code
		 * lineNumber 分號 → null (line number not match)
		 * lineNumber 註解 → null
		 * lineNumber 大括弧 → null (line number not match)
		 * lineNumber 空白行數 → null
		 */
		
		//取得該行內容
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> list = methodCollector.getMethodList();
		MethodDeclaration mDeclaration = list.get(1);
		ExpressionStatement statement = (ExpressionStatement) mDeclaration.getBody().statements().get(1);
		
		//輸入class指定行數
		int lineNumber = 21;
		
		//case#1:一般情況
		ASTNode astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertEquals(ASTNode.EXPRESSION_STATEMENT, astNode.getNodeType());
		assertEquals(lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
		assertEquals(astNode.toString(), statement.toString());
		
		//case#2:指向註解
		lineNumber = 10;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
		
		//case#3:指向method
		lineNumber = 18;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertEquals(ASTNode.METHOD_DECLARATION, astNode.getNodeType());
		assertEquals(lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
		assertEquals(astNode.toString(), mDeclaration.toString());
		
		//case#4:指向空白
		lineNumber = 48;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
		
		//case#5:超過該java行數
		lineNumber = 999999999;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
	}
	
	@Ignore
	public void testGetNodeFromSpecifiedClass_caseSemicolonParentheses() throws Exception {
		//取得該行內容
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
//		List<MethodDeclaration> list = methodCollector.getMethodList();
//		MethodDeclaration mDeclaration = list.get(1);
		
		//輸入class指定行數
		int lineNumber = 20;
		
		//(尚未解決)
		//類似的case有 ";" "{" "}"
		//case#6:指向分號get到node是正確的但是行數不match回傳null
		lineNumber = 46;
		ASTNode astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		// FIXME the bug need to be fix.
		assertEquals("FIXME the bug need to be fix.",lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
	}
}
