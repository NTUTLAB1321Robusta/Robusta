package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;

import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SpareHandlerVisitorTest {
	JavaFileToString javaaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "SpareHandlerTest";
		// Ū�������ɮ׼˥����e
		javaaFile2String = new JavaFileToString();
		javaaFile2String.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();
		// �s�W�����J��library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		javaProjectMaker.createJavaFile(DummyAndIgnoreExample.class.getPackage().getName(),
				DummyAndIgnoreExample.class.getSimpleName(),
				"package " + DummyAndIgnoreExample.class.getPackage().getName()	+ ";\n"
				+ javaaFile2String.getFileContent());
		
		Path dummyAndIgnoreExamplePath = new Path(testProjectName
				+ "/" + JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(DummyAndIgnoreExample.class.getName()
						.toString()) + JavaProjectMaker.JAVA_FILE_EXTENSION);
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(dummyAndIgnoreExamplePath)));
		parser.setResolveBindings(true);
		// ���oAST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		javaProjectMaker.deleteProject();
	}
	
	
	@Test
	public void testVisitNode_visitTryNode() {
		// �qSample code�̭����X�@��TryStatement
		SimpleTryStatementFinder simpleTryFinder = new SimpleTryStatementFinder("true_DummyHandlerCatchNestedTry");
		compilationUnit.accept(simpleTryFinder);
		
		// �ϥΪ̿�ܪ�TryStatement�PvisitNode�ǤJ��TryStatement�O�P�@��
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(simpleTryFinder.getTryStatement());
		spareHandlerVisitor.visit(simpleTryFinder.getTryStatement());
		assertTrue(spareHandlerVisitor.getResult());
	}
	
	@Test
	public void testVisitNode_visitATryNodeAndSelectAnotherTryNode() {
		// �qSample code�̭����X�Ĥ@��TryStatement
		SimpleTryStatementFinder simpleTryFinderUserSelect = new SimpleTryStatementFinder("true_DummyHandlerCatchNestedTry");
		compilationUnit.accept(simpleTryFinderUserSelect);
		
		// �qSample code�̭����X�ĤG��TryStatement
		SimpleTryStatementFinder simpleTryFinderInputVisitNode = new SimpleTryStatementFinder("true_DummyHandlerTryNestedTry");
		compilationUnit.accept(simpleTryFinderInputVisitNode);
		
		// �ϥΪ̿�ܪ�TryStatement�PvisitNode�ǤJ��TryStatement�O���P�@��
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(simpleTryFinderUserSelect.getTryStatement());
		spareHandlerVisitor.visit(simpleTryFinderInputVisitNode.getTryStatement());
		assertFalse(spareHandlerVisitor.getResult());
	}	
	
	@Test
	public void testVisitNode_userNotSelectNode() {
		// �qSample code�̭����X�@��TryStatement�`�I
		SimpleTryStatementFinder simpleTryFinder = new SimpleTryStatementFinder("true_DummyHandlerCatchNestedTry");
		compilationUnit.accept(simpleTryFinder);
		
		// �ϥΪ̨S����ܸ`�I
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(null);
		spareHandlerVisitor.visit(simpleTryFinder.getTryStatement());
		assertFalse(spareHandlerVisitor.getResult());			
	}
	
	@Test
	public void testProcessTryStatement_CatchNestedTry() throws Exception {
		// �ھګ��w�� Method Name ��X�ڭ̭n��TryNode
		SimpleTryStatementFinder simpleTryFinder = new SimpleTryStatementFinder("true_DummyHandlerCatchNestedTry");
		compilationUnit.accept(simpleTryFinder);
		
		// �}��processTryStatement���s���v��
		Method processTryStatement = SpareHandlerVisitor.class.getDeclaredMethod("processTryStatement", ASTNode.class);
		processTryStatement.setAccessible(true);
		
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(simpleTryFinder.getTryStatement());
		processTryStatement.invoke(spareHandlerVisitor, simpleTryFinder.getTryStatement());
		
		assertTrue(spareHandlerVisitor.getResult());
	}
	
	@Test
	public void testProcessTryStatement_TryNestedTry() throws Exception {
		// �ھګ��w�� Method Name ��X�ڭ̭n��TryNode
		SimpleTryStatementFinder simpleTryFinder = new SimpleTryStatementFinder("true_DummyHandlerTryNestedTry");
		compilationUnit.accept(simpleTryFinder);
		
		// �}��processTryStatement���s���v��
		Method processTryStatement = SpareHandlerVisitor.class.getDeclaredMethod("processTryStatement", ASTNode.class);
		processTryStatement.setAccessible(true);
		
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(simpleTryFinder.getTryStatement());
		processTryStatement.invoke(spareHandlerVisitor, simpleTryFinder.getTryStatement());
		
		assertTrue(spareHandlerVisitor.getResult());
	}
	
	@Test
	public void testProcessTryStatement_TryWithoutNested() throws Exception {
		// �ھګ��w�� Method Name ��X�ڭ̭n��TryNode
		SimpleTryStatementFinder simpleTryFinder = new SimpleTryStatementFinder("false_rethrowRuntimeException");
		compilationUnit.accept(simpleTryFinder);
		
		// �}��processTryStatement���s���v��
		Method processTryStatement = SpareHandlerVisitor.class.getDeclaredMethod("processTryStatement", ASTNode.class);
		processTryStatement.setAccessible(true);
		
		SpareHandlerVisitor spareHandlerVisitor = new SpareHandlerVisitor(simpleTryFinder.getTryStatement());
		processTryStatement.invoke(spareHandlerVisitor, simpleTryFinder.getTryStatement());
		
		assertTrue(spareHandlerVisitor.getResult());
	}

	public class SimpleTryStatementFinder extends ASTVisitor{
		private TryStatement tryStatement;
		private String nameOfMethodWithSpecifiedTryStatement;
		public SimpleTryStatementFinder(String nameOfMethodWithSpecifiedTry) {
			super();
			tryStatement = null;
			nameOfMethodWithSpecifiedTryStatement = nameOfMethodWithSpecifiedTry;
		}
		
		public boolean visit(MethodDeclaration node) {
			return node.getName().toString().equals(nameOfMethodWithSpecifiedTryStatement);
		}
		
		public boolean visit(TryStatement node) {
			tryStatement = node;
			return false;
		}
		
		public TryStatement getTryStatement() {
			return tryStatement;
		}
	}
}
