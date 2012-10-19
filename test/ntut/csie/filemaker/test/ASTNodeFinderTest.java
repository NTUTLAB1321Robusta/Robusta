package ntut.csie.filemaker.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.jdt.util.testSampleCode.NodeUtilsTestSample;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.robusta.util.PathUtils;

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
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		javaFileToString.read(NodeUtilsTestSample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(NodeUtilsTestSample.class.getPackage().getName(),
				NodeUtilsTestSample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + NodeUtilsTestSample.class.getPackage().getName() + ";\n"
						+ javaFileToString.getFileContent());
		javaFileToString.clear();
		

		Path ccExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(NodeUtilsTestSample.class, projectName));
		
		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin
				.getWorkspace().getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);
		// ���oAST
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
		 * ���z
		 * lineNumber = 1 �� �ӥؼ� .java ���� code
		 * lineNumber method �� ��� method code
		 * lineNumber class �� ��� class code
		 * lineNumber ���� �� null (line number not match)
		 * lineNumber ���� �� null
		 * lineNumber �j�A�� �� null (line number not match)
		 * lineNumber �ťզ�� �� null
		 */
		
		//���o�Ӧ椺�e
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		List<MethodDeclaration> list = methodCollector.getMethodList();
		MethodDeclaration mDeclaration = list.get(1);
		ExpressionStatement statement = (ExpressionStatement) mDeclaration.getBody().statements().get(1);
		
		//��Jclass���w���
		int lineNumber = 20;
		
		//case#1:�@�뱡�p
		ASTNode astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertEquals(ASTNode.EXPRESSION_STATEMENT, astNode.getNodeType());
		assertEquals(lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
		assertEquals(astNode.toString(), statement.toString());
		
		//case#2:���V����
		lineNumber = 9;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
		
		//case#3:���Vmethod
		lineNumber = 17;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertEquals(ASTNode.METHOD_DECLARATION, astNode.getNodeType());
		assertEquals(lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
		assertEquals(astNode.toString(), mDeclaration.toString());
		
		//case#4:���V�ť�
		lineNumber = 49;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
		
		//case#5:�W�L��java���
		lineNumber = 999999999;
		astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		assertNull(astNode);
	}
	
//	@Test
	public void testGetNodeFromSpecifiedClass_caseSemicolonParentheses() throws Exception {
		//���o�Ӧ椺�e
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
//		List<MethodDeclaration> list = methodCollector.getMethodList();
//		MethodDeclaration mDeclaration = list.get(1);
		
		//��Jclass���w���
		int lineNumber = 20;
		
		//(�|���ѨM)
		//������case�� ";" "{" "}"
		//case#6:���V����get��node�O���T�����O��Ƥ�match�^��null
		lineNumber = 46;
		ASTNode astNode = ASTNodeFinder.getNodeFromSpecifiedClass(NodeUtilsTestSample.class, projectName, lineNumber);
		// FIXME the bug need to be fix.
		assertEquals("FIXME the bug need to be fix.",lineNumber, compilationUnit.getLineNumber(astNode.getStartPosition()));
	}
}
