package ntut.csie.csdet.visitor;

import static org.junit.Assert.*;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NestedTryStatementVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	CarelessCleanupVisitor carelessCleanupVisitor;
	SmellSettings smellSettings;
	
	@Before
	public void setUp() throws Exception {
		String projectName = "NestedTryStatementExampleProject";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.LIB_JAR_FOLDERNAME, JavaProjectMaker.BIN_CLASS_FOLDERNAME);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/lib/RL.jar");
		javaProjectMaker.setJREDefaultContainer();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testNestedTryStatementVisitor() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetNestedTryStatementList() {
		fail("Not yet implemented");
	}

	@Test
	public void testVisitTryStatement() {
		fail("Not yet implemented");
	}

}
