package ntut.csie.robusta.codegen;


import static org.junit.Assert.*;

import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitor;
import ntut.csie.robusta.codegen.ExpressionStatementStringFinderVisitor;
import ntut.csie.util.PathUtils;

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
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();

		javaFile2String = new JavaFileToString();
		javaFile2String.read(StatementBeFoundSampleCode.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				StatementBeFoundSampleCode.class.getPackage().getName(),
				StatementBeFoundSampleCode.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + StatementBeFoundSampleCode.class.getPackage().getName() + ";%n"
				+ javaFile2String.getFileContent());
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(StatementBeFoundSampleCode.class, testProjectName));
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
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
		assertEquals(502, statementFinderVisitor.getFoundExpressionStatement().getStartPosition());
		
		targetStatement = "System.out.println(e);";
		statementFinderVisitor = new ExpressionStatementStringFinderVisitor(targetStatement);
		compilationUnit.accept(statementFinderVisitor);
		assertEquals(556, statementFinderVisitor.getFoundExpressionStatement().getStartPosition());
		
		targetStatement = "e.printStackTrace();";
		statementFinderVisitor = new ExpressionStatementStringFinderVisitor(targetStatement);
		compilationUnit.accept(statementFinderVisitor);
		assertFalse("this method only can find out the first ExpressionStatement in code", (statementFinderVisitor.getFoundExpressionStatement().getStartPosition() == (867 - 1)));
	}
}
