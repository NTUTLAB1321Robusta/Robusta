package ntut.csie.robusta.codegen;


import static org.junit.Assert.*;
import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.sampleCode4VisitorTest.StatementBeFoundSampleCode;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitor;
import ntut.csie.robusta.codegen.ExpressionStatementStringFinderVisitor;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExpressionStatementStringFinderVisitorTest {
	String testProjectName;
	JavaProjectMaker javaProjectMaker;
	JavaFileToString javaFile2String;
	CompilationUnit compilationUnit;
	CatchClauseFinderVisitor dummyHandlerVisitor;
	SmellSettings smellSettings;
	ASTMethodCollector methodCollector;
	List<ASTNode> methodCollectList;
	String[] dummyHandlerPatternsInXML;

	public ExpressionStatementStringFinderVisitorTest() {
		testProjectName = this.getClass().getSimpleName();
	}
	
	@Before
	public void setUp() throws Exception {
		// 準備測試檔案樣本內容
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();

		// 建立新的檔案DummyAndIgnoreExample
		javaFile2String = new JavaFileToString();
		javaFile2String.read(StatementBeFoundSampleCode.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				StatementBeFoundSampleCode.class.getPackage().getName(),
				StatementBeFoundSampleCode.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + StatementBeFoundSampleCode.class.getPackage().getName() + ";%n"
				+ javaFile2String.getFileContent());
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(StatementBeFoundSampleCode.class, testProjectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public final void testFindExpressionStatement() throws Exception {
		String targetStatement = "e.printStackTrace();";
		ExpressionStatementStringFinderVisitor statementFinderVisitor = new ExpressionStatementStringFinderVisitor(targetStatement);
		compilationUnit.accept(statementFinderVisitor);
		assertEquals(574, statementFinderVisitor.getFoundExpressionStatement().getStartPosition());
		
		targetStatement = "System.out.println(e);";
		statementFinderVisitor = new ExpressionStatementStringFinderVisitor(targetStatement);
		compilationUnit.accept(statementFinderVisitor);
		assertEquals(683, statementFinderVisitor.getFoundExpressionStatement().getStartPosition());
		
		// line 22 & line 32的程式碼內容一樣，但是程式先找到符合的就會停下來
		targetStatement = "e.printStackTrace();";
		statementFinderVisitor = new ExpressionStatementStringFinderVisitor(targetStatement);
		compilationUnit.accept(statementFinderVisitor);
		assertFalse("此方法只能找到最先出現的ExpressionStatement", (statementFinderVisitor.getFoundExpressionStatement().getStartPosition() == (867 - 1)));
	}
}
