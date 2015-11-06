package ntut.csie.robusta.codegen;

import static org.junit.Assert.*;

import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitor;
import ntut.csie.robusta.codegen.StatementFinderVisitor;
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
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();

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
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		compilationUnit = (CompilationUnit) parser.createAST(null); 
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}

	@Test
	public final void testFindExpressionStatementInCatchClause() {
		int targetStartPosition = 502 - 1;
		StatementFinderVisitor statementFinderVisitor = new StatementFinderVisitor(targetStartPosition);
		compilationUnit.accept(statementFinderVisitor);
		assertEquals("e.printStackTrace();\n", statementFinderVisitor.getFoundExpressionStatement().toString());
		
		targetStartPosition = 556 - 1;
		statementFinderVisitor = new StatementFinderVisitor(targetStartPosition);
		compilationUnit.accept(statementFinderVisitor);
		assertEquals("System.out.println(e);\n", statementFinderVisitor.getFoundExpressionStatement().toString());
		
		targetStartPosition = 686 - 1;
		statementFinderVisitor = new StatementFinderVisitor(targetStartPosition);
		compilationUnit.accept(statementFinderVisitor);
		assertEquals("e.printStackTrace();\n", statementFinderVisitor.getFoundExpressionStatement().toString());
	}

}
