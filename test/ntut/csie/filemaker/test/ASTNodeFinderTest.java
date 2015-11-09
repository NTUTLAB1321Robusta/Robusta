package ntut.csie.filemaker.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.util.NodeUtilsTestSample;
import ntut.csie.util.PathUtils;

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

		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin
				.getWorkspace().getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);

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
		 * lineNumber = 1 →  hole program code in java file
		 * lineNumber of method →  hole method code
		 * lineNumber of class → hole class code
		 * lineNumber of ":" → null (line number not match)
		 * lineNumber of comment → null
		 * lineNumber of braces → null (line number not match)
		 * lineNumber of empty line → null
		 */
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> list = methodCollector.getMethodList();
		MethodDeclaration mDeclaration = list.get(1);
		ExpressionStatement statement = (ExpressionStatement) mDeclaration.getBody().statements().get(1);
		
		int lineNumber = 18;
		
		//case#1:normal state
		ASTNode astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertEquals(ASTNode.EXPRESSION_STATEMENT, astNode.getNodeType());
		assertEquals(lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
		assertEquals(astNode.toString(), statement.toString());
		
		//case#2:line number of comment 
		lineNumber = 10;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
		
		//case#3:line number of method
		lineNumber = 16;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertEquals(ASTNode.METHOD_DECLARATION, astNode.getNodeType());
		assertEquals(lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
		assertEquals(astNode.toString(), mDeclaration.toString());
		
		//case#4:line number of empty line
		lineNumber = 32;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
		
		//case#5:line number over java file 
		lineNumber = 999999999;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
	}
	
	@Ignore
	public void testGetNodeFromSpecifiedClass_caseSemicolonParentheses() throws Exception {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		
		int lineNumber = 20;
		
		//(unsolved issue)
		//similar case like input the line number of ";", "{" and "}"
		//case#6: use the line number of ";" can get the correct astnode.
		//        but line number got by using astnode is not match to the original line number 
		lineNumber = 37;
		ASTNode astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		// FIXME the bug need to be fix.
		assertEquals("FIXME the bug need to be fix.",lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
	}
}
