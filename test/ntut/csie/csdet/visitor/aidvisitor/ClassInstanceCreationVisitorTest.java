package ntut.csie.csdet.visitor.aidvisitor;


import static org.junit.Assert.*;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.sampleCode4VisitorTest.ClassInstanceCreationSampleCode;
import ntut.csie.robusta.util.PathUtils;

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

public class ClassInstanceCreationVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	ClassInstanceCreationVisitor classInstanceCreationVisitor;
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "ClassInstanceCreationSampleCodeProject";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String.read(ClassInstanceCreationSampleCode.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				ClassInstanceCreationSampleCode.class.getPackage().getName(),
				ClassInstanceCreationSampleCode.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + ClassInstanceCreationSampleCode.class.getPackage().getName() + ";" + String.format("%n")
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		Path ccExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(ClassInstanceCreationSampleCode.class, testProjectName));
		
		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);

		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}

	@Test
	public final void testOneLineCreation() {
		ASTNode methodDelcaration = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "createInstanceOneLine");
		// 從fos.flush();找出建立instance的地方
		MethodInvocation fosFlush = (MethodInvocation)NodeFinder.perform(compilationUnit, 338, 11);
		ASTNode fosCreation = NodeFinder.perform(compilationUnit, 277, 38);
		classInstanceCreationVisitor = new ClassInstanceCreationVisitor(fosFlush);
		methodDelcaration.accept(classInstanceCreationVisitor);
		assertEquals(fosCreation, classInstanceCreationVisitor.getClassInstanceCreation());
	}
	
	@Test
	public final void testTwoLineCreation() {
		ASTNode methodDelcaration = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "createInstanceTwoLine");
		// 從fos.flush();找出建立instance的地方
		MethodInvocation fosFlush = (MethodInvocation)NodeFinder.perform(compilationUnit, 535, 11);
		ASTNode fosCreation = NodeFinder.perform(compilationUnit, 474, 38);
		classInstanceCreationVisitor = new ClassInstanceCreationVisitor(fosFlush);
		methodDelcaration.accept(classInstanceCreationVisitor);
		assertEquals(fosCreation, classInstanceCreationVisitor.getClassInstanceCreation());
	}
	
	@Test
	public final void testTwoLineAndNewNew() {
		ASTNode methodDelcaration = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "createInstanceTwoLineAndNewNew");
		// 從fos.flush();找出建立instance的地方
		MethodInvocation fosFlush = (MethodInvocation)NodeFinder.perform(compilationUnit, 796, 11);
		ASTNode fosCreation = NodeFinder.perform(compilationUnit, 698, 53);
		classInstanceCreationVisitor = new ClassInstanceCreationVisitor(fosFlush);
		methodDelcaration.accept(classInstanceCreationVisitor);
		assertEquals(fosCreation, classInstanceCreationVisitor.getClassInstanceCreation());
	}
}
