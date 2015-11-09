package ntut.csie.robusta.codegen;


import static org.junit.Assert.*;

import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VariableDeclarationStatementFinderVisitorTest {
	String testProjectName;
	JavaProjectMaker javaProjectMaker;
	JavaFileToString javaFile2String;
	CompilationUnit compilationUnit;
	VariableDeclarationStatementFinderVisitor variableDeclarationStatementFinder;
	SmellSettings smellSettings;
	List<ASTNode> methodCollectList;
	
	@Before
	public void setUp() throws Exception {
		testProjectName = "VariableDeclarationStatementFinderVisitor";
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();

		javaFile2String = new JavaFileToString();
		javaFile2String.read(VariableDeclarationStatementSampleCode.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				VariableDeclarationStatementSampleCode.class.getPackage().getName(),
				VariableDeclarationStatementSampleCode.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + VariableDeclarationStatementSampleCode.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(VariableDeclarationStatementSampleCode.class, testProjectName));
		//Create AST to parse
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

	/**
	 * variable is declared in class, so it would be a FieldDeclaration which visitor can not find it.
	 */
	@Test
	public void testVariableDeclarationStatementFinder_Field() {
		// fieldString.toLowerCase()
		MethodInvocation mi = (MethodInvocation)NodeFinder.perform(compilationUnit, 301 - 1, 25);
		VariableDeclarationStatementFinderVisitor vdsfv = new VariableDeclarationStatementFinderVisitor(mi);
		compilationUnit.accept(vdsfv);
		assertNull(vdsfv.getFoundVariableDeclarationStatement());
	}
	
	@Test
	public void testVariableDeclarationStatementFinder_Foundable() {
		// localString.getBytes();
		MethodInvocation mi = (MethodInvocation)NodeFinder.perform(compilationUnit, 464 - 1, 22);
		VariableDeclarationStatementFinderVisitor vdsfv = new VariableDeclarationStatementFinderVisitor(mi);
		compilationUnit.accept(vdsfv);
		assertEquals("String localString=\"千山鳥飛絕\";\n", vdsfv.getFoundVariableDeclarationStatement().toString());
	}
	
	/**
	 * check whether visitor can detect two different method which they has the same program code inside.
	 */
	@Test
	public void testVariableDeclarationStatementFinder_DifferentMethodDeclaraionOwnsSameCode_1st() {
		MethodInvocation mi = (MethodInvocation)NodeFinder.perform(compilationUnit, 639 - 1, 22);
		VariableDeclarationStatementFinderVisitor vdsfv = new VariableDeclarationStatementFinderVisitor(mi);
		compilationUnit.accept(vdsfv);
		assertEquals((603 - 1), vdsfv.getFoundVariableDeclarationStatement().getStartPosition());
	}
	
	/**
	 * @see #testVariableDeclarationStatementFinder_DifferentMethodDeclaraionOwnsSameCode_1st()
	 * 從MD2的mi尋找他的VariableDeclarationStatement
	 */
	@Test
	public void testVariableDeclarationStatementFinder_DifferentMethodDeclaraionOwnsSameCode_2rd() {
		MethodInvocation mi = (MethodInvocation)NodeFinder.perform(compilationUnit, 783 - 1, 22);
		VariableDeclarationStatementFinderVisitor vdsfv = new VariableDeclarationStatementFinderVisitor(mi);
		compilationUnit.accept(vdsfv);
		assertEquals((747 - 1), vdsfv.getFoundVariableDeclarationStatement().getStartPosition());
	}
	
}
