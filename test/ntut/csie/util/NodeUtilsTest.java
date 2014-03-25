package ntut.csie.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.ASTCatchCollect;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.util.NodeUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SimpleName;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
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
		javaFile2String.read(NodeUtilsTestSample.class,
				JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(NodeUtilsTestSample.class.getPackage()
				.getName(), NodeUtilsTestSample.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ NodeUtilsTestSample.class.getPackage().getName() + ";"
				+ String.format("%n") + javaFile2String.getFileContent());
		javaFile2String.clear();

		Path ccExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(
				NodeUtilsTestSample.class, projectName));
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
	public void testIsITypeBindingExtended() {
		// when ITypeBinding is null
		assertFalse(NodeUtils.isITypeBindingExtended(null, Object.class));

		compilationUnit.accept(miVisitor);
		assertEquals(15, miVisitor.countMethodInvocations());

		// when ITypeBinding is Object
		assertTrue(NodeUtils.isITypeBindingExtended(miVisitor
				.getMethodInvocation(0).resolveMethodBinding()
				.getDeclaringClass(), Object.class));

		// when ITypeBinding is an subclass of Object
		assertTrue(NodeUtils.isITypeBindingExtended(miVisitor
				.getMethodInvocation(1).resolveMethodBinding()
				.getDeclaringClass(), Object.class));

		// when ITypeBinding is Object and check Closeable interface
		assertFalse(NodeUtils.isITypeBindingExtended(miVisitor
				.getMethodInvocation(0).resolveMethodBinding()
				.getDeclaringClass(), Closeable.class));

		// when ITypeBinding is RuntimeException and check Exception
		assertFalse(NodeUtils.isITypeBindingExtended(miVisitor
				.getMethodInvocation(10).resolveMethodBinding()
				.getDeclaringClass(), Exception.class));
	}

	@Test
	public void testIsITypeBindingImplemented() {
		// null的情況
		assertFalse(NodeUtils.isITypeBindingImplemented(null, Closeable.class));

		compilationUnit.accept(miVisitor);
		assertEquals(15, miVisitor.countMethodInvocations());

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
		// public void readFile() throws Exception - fos.close();
		MethodInvocation node = (MethodInvocation) NodeFinder.perform(
				compilationUnit, 1507, 11);
		assertTrue(NodeUtils.isMethodInvocationInFinally(node));
	}

	@Test
	public void testGetDeclaredExceptions() {
		MethodInvocation methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						"outputStreamMethod", "os.close()").get(0);
		ITypeBinding iTypeBindings[] = NodeUtils
				.getDeclaredExceptions(methodInvocation);
		assertEquals(1, iTypeBindings.length);
		assertEquals("java.io.IOException", iTypeBindings[0].getQualifiedName());
	}

	@Test
	public void testGetDeclaredExceptionsWhenItIsEmpty() {
		MethodInvocation methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						"fileMethod", "f.toString()").get(0);
		ITypeBinding iTypeBindings[] = NodeUtils
				.getDeclaredExceptions(methodInvocation);
		assertEquals(0, iTypeBindings.length);
	}

	@Test
	public void testGetMethodInvocationBindingVariableSimpleName() {
		MethodInvocation methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						"outputStreamMethod", "os.close()").get(0);
		SimpleName simpleName = NodeUtils
				.getSimpleNameFromExpression(methodInvocation
						.getExpression());
		assertTrue("os".equals(simpleName.toString()));
	}

	@Test
	public void testGetClassFromCatchClauseWithJavaDefinedException() {
		ASTCatchCollect catchCollecter = new ASTCatchCollect();
		compilationUnit.accept(catchCollecter);
		List<CatchClause> catchClauses = catchCollecter.getMethodList();

		assertEquals(2, catchClauses.size());

		Class clazz = NodeUtils.getClassFromCatchClause(catchClauses.get(0));
		assertEquals(IOException.class, clazz);
	}

	/**
	 * We can not recognize user defined exception
	 * @author pig
	 */
	@Ignore
	public void testGetClassFromCatchClauseWithUserSelfDefinedException() {
		ASTCatchCollect catchCollecter = new ASTCatchCollect();
		compilationUnit.accept(catchCollecter);
		List<CatchClause> catchClauses = catchCollecter.getMethodList();

		assertEquals(2, catchClauses.size());

		try {
			Class clazz = NodeUtils.getClassFromCatchClause(catchClauses.get(1));
		} catch (RuntimeException e) {
			String message = "We can not recognize the exception type what defined by user";
			assertTrue(message, false);
		}
	}

	public class MethodInvocationVisitor extends ASTVisitor {
		private List<MethodInvocation> methodInvocationList;
		
		public MethodInvocationVisitor(){
			super();
			methodInvocationList = new ArrayList<MethodInvocation>();
		}
		
		public boolean visit(MethodInvocation node) {
			methodInvocationList.add(node);
			return false;
		}
		
		public MethodInvocation getMethodInvocation(int index) {
			return methodInvocationList.get(index);
		}
		
		public int countMethodInvocations() {
			return methodInvocationList.size();
		}
	}
}
