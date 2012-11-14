package ntut.csie.robusta.codegen;

import static org.junit.Assert.*;

import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.sampleCode4VisitorTest.StatementBeFoundSampleCode;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitor;
import ntut.csie.robusta.codegen.StatementFinderVisitor;
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

public class StatementFinderVisitorTest {
	String testProjectName;
	JavaProjectMaker javaProjectMaker;
	JavaFileToString javaFile2String;
	CompilationUnit compilationUnit;
	CatchClauseFinderVisitor dummyHandlerVisitor;
	SmellSettings smellSettings;
	ASTMethodCollector methodCollector;
	List<ASTNode> methodCollectList;
	String[] dummyHandlerPatternsInXML;
	
	public StatementFinderVisitorTest() {
		testProjectName = "StatementFinderVisitor";
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
				"package " + StatementBeFoundSampleCode.class.getPackage().getName() + ";\n"
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
	public final void testFindExpressionStatementInCatchClause() {
		int targetStartPosition = 574 - 1;
		StatementFinderVisitor statementFinderVisitor = new StatementFinderVisitor(targetStartPosition);
		compilationUnit.accept(statementFinderVisitor);
		assertEquals("e.printStackTrace();\n", statementFinderVisitor.getFoundExpressionStatement().toString());
		
		targetStartPosition = 683 - 1;
		statementFinderVisitor = new StatementFinderVisitor(targetStartPosition);
		compilationUnit.accept(statementFinderVisitor);
		assertEquals("System.out.println(e);\n", statementFinderVisitor.getFoundExpressionStatement().toString());
		
		targetStartPosition = 867 - 1;
		statementFinderVisitor = new StatementFinderVisitor(targetStartPosition);
		compilationUnit.accept(statementFinderVisitor);
		assertEquals("e.printStackTrace();\n", statementFinderVisitor.getFoundExpressionStatement().toString());
	}

}
