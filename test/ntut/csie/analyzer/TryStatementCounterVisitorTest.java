/**
 * 
 */
package ntut.csie.analyzer;

import ntut.csie.analyzer.nested.NestedTryStatementExample;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Reverof
 *
 */
public class TryStatementCounterVisitorTest {
	JavaFileToString javaFileToString;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		String testProjectName = "TryStatementCounterTest";
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();
		
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");

		javaFileToString = new JavaFileToString();
		javaFileToString.read(NestedTryStatementExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				NestedTryStatementExample.class.getPackage().getName(),
				NestedTryStatementExample.class.getSimpleName() +  JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + NestedTryStatementExample.class.getPackage().getName() + ";\n"
						+ javaFileToString.getFileContent());
		
		Path path = new Path(testProjectName + "/"
				+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(NestedTryStatementExample.class.getName())
				+ JavaProjectMaker.JAVA_FILE_EXTENSION);
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}

	/**
	 * @throws java.lang.Exception
	 */
	
	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void visitTryCounter() {
		TryStatementCounterVisitor visitor = new TryStatementCounterVisitor();
		compilationUnit.accept(visitor);
		assertEquals(50, visitor.getTryCount());
		assertEquals(65, visitor.getCatchCount());
		assertEquals(13, visitor.getFinallyCount());
	}
}
