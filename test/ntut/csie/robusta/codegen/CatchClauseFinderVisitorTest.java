package ntut.csie.robusta.codegen;

import static org.junit.Assert.*;

import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitor;
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

public class CatchClauseFinderVisitorTest {
	String testProjectName;
	JavaProjectMaker javaProjectMaker;
	JavaFileToString javaFile2String;
	CompilationUnit compilationUnit;
	CatchClauseFinderVisitor dummyHandlerVisitor;
	SmellSettings smellSettings;
	ASTMethodCollector methodCollector;
	List<ASTNode> methodCollectList;
	
	public CatchClauseFinderVisitorTest() {
		testProjectName = "CatchClauseFinderVisitor";
	}
	
	@Before
	public void setUp() throws Exception {
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();

		javaFile2String = new JavaFileToString();
		javaFile2String.read(CatchClauseSampleCode.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				CatchClauseSampleCode.class.getPackage().getName(),
				CatchClauseSampleCode.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + CatchClauseSampleCode.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(CatchClauseSampleCode.class, testProjectName));
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testCatchClauseFinderVisitor_2CatchClause() {
		//to get correct position of statement, you should subtract the position from ASTVise with 1 
		int targetCatchClauseStartPosition = 459 - 1;
		CatchClauseFinderVisitor catchClauseFinder = new CatchClauseFinderVisitor(targetCatchClauseStartPosition);
		compilationUnit.accept(catchClauseFinder);
		assertEquals("catch (FileNotFoundException e) {\n  e.printStackTrace();\n}\n", catchClauseFinder.getFoundCatchClause().toString());
		
		targetCatchClauseStartPosition = 523 - 1;
		catchClauseFinder = new CatchClauseFinderVisitor(targetCatchClauseStartPosition);
		compilationUnit.accept(catchClauseFinder);
		assertEquals("catch (IOException e) {\n  e.printStackTrace();\n}\n", catchClauseFinder.getFoundCatchClause().toString());
	}
	
	@Test
	public void testCatchClauseFinderVisitor_1CatchClause() {
		int targetCatchClauseStartPosition = 703 - 1;
		CatchClauseFinderVisitor catchClauseFinder = new CatchClauseFinderVisitor(targetCatchClauseStartPosition);
		compilationUnit.accept(catchClauseFinder);
		assertEquals("catch (IOException e) {\n  System.out.println(e);\n}\n", catchClauseFinder.getFoundCatchClause().toString());
	}

}
