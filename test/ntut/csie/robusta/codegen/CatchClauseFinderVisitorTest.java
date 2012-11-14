package ntut.csie.robusta.codegen;

import static org.junit.Assert.*;

import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.sampleCode4VisitorTest.CatchClauseSampleCode;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitor;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CatchClauseFinderVisitorTest {
	String testProjectName;
	JavaProjectMaker javaProjectMaker;
	JavaFileToString javaFile2String;
	CompilationUnit compilationUnit;
	CatchClauseFinderVisitor dummyHandlerVisitor;
	SmellSettings smellSettings;
	ASTMethodCollector methodCollector;
	List<ASTNode> methodCollectList;
	
	public CatchClauseFinderVisitorTest() {
		testProjectName = "CatchClauseFinderVisitor";
	}
	
	@Before
	public void setUp() throws Exception {
		// �ǳƴ����ɮ׼˥����e
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();

		// �إ߷s���ɮ�DummyAndIgnoreExample
		javaFile2String = new JavaFileToString();
		javaFile2String.read(CatchClauseSampleCode.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				CatchClauseSampleCode.class.getPackage().getName(),
				CatchClauseSampleCode.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + CatchClauseSampleCode.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(CatchClauseSampleCode.class, testProjectName));
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

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testCatchClauseFinderVisitor_2CatchClause() {
		// �����D������A���ծɭԡAASTView�W���ݨ쪺�Ʀr�٭n��@�A���פ~�|���T�C��ڥΪ��ɭԨS�t�C
		int targetCatchClauseStartPosition = 476 - 1;
		CatchClauseFinderVisitor catchClauseFinder = new CatchClauseFinderVisitor(targetCatchClauseStartPosition);
		compilationUnit.accept(catchClauseFinder);
		assertEquals("catch (FileNotFoundException e) {\n  e.printStackTrace();\n}\n", catchClauseFinder.getFoundCatchClause().toString());
		
		targetCatchClauseStartPosition = 577 - 1;
		catchClauseFinder = new CatchClauseFinderVisitor(targetCatchClauseStartPosition);
		compilationUnit.accept(catchClauseFinder);
		assertEquals("catch (IOException e) {\n  e.printStackTrace();\n}\n", catchClauseFinder.getFoundCatchClause().toString());
	}
	
	@Test
	public void testCatchClauseFinderVisitor_1CatchClause() {
		int targetCatchClauseStartPosition = 794 - 1;
		CatchClauseFinderVisitor catchClauseFinder = new CatchClauseFinderVisitor(targetCatchClauseStartPosition);
		compilationUnit.accept(catchClauseFinder);
		assertEquals("catch (IOException e) {\n  System.out.println(e);\n}\n", catchClauseFinder.getFoundCatchClause().toString());
	}

}
