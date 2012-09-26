/**
 * 
 */
package ntut.csie.csdet.visitor;

import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.NestedTryStatementExample;
import ntut.csie.robusta.util.PathUtils;

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
	JavaFileToString javaFile2String;
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
		
		// �s�W�����J��library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");

		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		javaFile2String = new JavaFileToString();
		javaFile2String.read(NestedTryStatementExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				NestedTryStatementExample.class.getPackage().getName(),
				NestedTryStatementExample.class.getSimpleName() +  JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + NestedTryStatementExample.class.getPackage().getName() + ";\n"
						+ javaFile2String.getFileContent());
		
		Path path = new Path(testProjectName + "/"
				+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(NestedTryStatementExample.class.getName())
				+ JavaProjectMaker.JAVA_FILE_EXTENSION);
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// ���oAST
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
		assertEquals(46, visitor.getTryCount());
		assertEquals(61, visitor.getCatchCount());
		assertEquals(12, visitor.getFinallyCount());
	}
}
