package ntut.csie.jdt.util;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;

import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.jdt.util.simpleVisitor.MethodInvocationVisitor;
import ntut.csie.jdt.util.testSampleCode.NodeUtilsTestSample;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NodeUtilsTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	MethodInvocationVisitor miVisitor;
	String projectName;
	
	public NodeUtilsTest() {
		projectName = "NodeUtilsExampleProject";
	}

	@Before
	public void setUp() throws Exception {
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String.read(NodeUtilsTestSample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(NodeUtilsTestSample.class.getPackage().getName(),
				NodeUtilsTestSample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + NodeUtilsTestSample.class.getPackage().getName() + ";" + String.format("%n")
						+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		Path ccExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(NodeUtilsTestSample.class, projectName));
		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin
				.getWorkspace().getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		miVisitor = new MethodInvocationVisitor();
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testIsITypeBindingImplemented() {	
		// null的情況
		assertFalse(NodeUtils.isITypeBindingImplemented(null, Closeable.class));
		
		compilationUnit.accept(miVisitor);
		assertEquals(10, miVisitor.countMethodInvocations());
		
		// ITypeBinding為Object的情況
		assertFalse(NodeUtils.isITypeBindingImplemented(miVisitor
				.getMethodInvocation(0).resolveMethodBinding()
				.getDeclaringClass(), Closeable.class));
		
		// ITypeBinding的SuperClass也不是使用者指定之interface的情況
		assertFalse(NodeUtils.isITypeBindingImplemented(miVisitor
				.getMethodInvocation(1).resolveMethodBinding()
				.getDeclaringClass(), Closeable.class));
		
		assertFalse(NodeUtils.isITypeBindingImplemented(miVisitor
				.getMethodInvocation(4).resolveMethodBinding()
				.getDeclaringClass(), Closeable.class));
				
		// ITypeBinding為使用者指定之interface的情況
		assertTrue(
				miVisitor.getMethodInvocation(2).toString(),
				NodeUtils.isITypeBindingImplemented(miVisitor
						.getMethodInvocation(2).resolveMethodBinding()
						.getDeclaringClass(), Closeable.class));
		
		assertTrue(NodeUtils.isITypeBindingImplemented(miVisitor
				.getMethodInvocation(3).resolveMethodBinding()
				.getDeclaringClass(), Closeable.class));
		
		// ITypeBinding的superClass為使用者指定之interface的情況
		assertTrue(NodeUtils.isITypeBindingImplemented(miVisitor
				.getMethodInvocation(5).resolveMethodBinding()
				.getDeclaringClass(), Closeable.class));
	}

	@Test
	public void testIsMethodInvocationInFinally() {
		//public void readFile() throws Exception - fos.close();
		MethodInvocation node = (MethodInvocation)NodeFinder.perform(compilationUnit, 1526, 11);
		assertTrue(NodeUtils.isMethodInvocationInFinally(node));
	}
}
