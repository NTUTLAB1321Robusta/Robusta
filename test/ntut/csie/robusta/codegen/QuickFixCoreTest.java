package ntut.csie.robusta.codegen;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.filemaker.exceptionBadSmells.UserDefineDummyHandlerFish;
import ntut.csie.robusta.codegen.QuickFixCore;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class QuickFixCoreTest {
	String projectNameString;
	String packageNameString;
	String classSimpleNameString;
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	SmellSettings smellSettings;
	Path path;

	public QuickFixCoreTest() {
		projectNameString = "QuickFixCoreTest";
		packageNameString = DummyAndIgnoreExample.class.getPackage().getName();
		classSimpleNameString = DummyAndIgnoreExample.class.getSimpleName();
	}
	
	@Before
	public void setUp() throws Exception {
		// �ǳƴ����ɮ׼˥����e
		javaProjectMaker = new JavaProjectMaker(projectNameString);
		javaProjectMaker.setJREDefaultContainer();
		
		// �s�W�����J��library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		
		// �Yexample code����robustness notation�h������i�H���sĶ�q�L
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.FOLDERNAME_LIB_JAR + JavaProjectMaker.FOLDERNAME_LIB_JAR);

		// �إ߷s���ɮ�DummyAndIgnoreExample
		javaFile2String = new JavaFileToString();
		javaFile2String.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				DummyAndIgnoreExample.class.getPackage().getName(),
				DummyAndIgnoreExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + DummyAndIgnoreExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		// �~��إߴ��եΪ�UserDefineDummyHandlerFish
		javaFile2String.clear();
		javaFile2String.read(UserDefineDummyHandlerFish.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefineDummyHandlerFish.class.getPackage().getName(),
				UserDefineDummyHandlerFish.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefineDummyHandlerFish.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		path = new Path(PathUtils.getPathOfClassUnderSrcFolder(DummyAndIgnoreExample.class, projectNameString));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
	}

	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// �p�Gxml�ɮצs�b�A�h�R����
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// �R���M��
		javaProjectMaker.deleteProject();
	}

	@Test
	public void testGenerateRobustnessLevelAnnotation() {
		fail("Not yet implemented");
	}

	@Test
	public void testGenerateThrowExceptionOnMethodDeclaration() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveNodeInCatchClause() {
		QuickFixCore quickFixCore = new QuickFixCore();
		quickFixCore.setJavaFileModifiable(ResourcesPlugin.getWorkspace().getRoot().findMember(path));
		TryStatement tryStatement = ASTNodeFinder.getTryStatementNodeListByMethodDeclarationName(quickFixCore.getCompilationUnit(), "true_printStackTrace_public").get(0);
		CatchClause catchClause = (CatchClause) tryStatement.catchClauses().get(0);
		quickFixCore.removeNodeInCatchClause(catchClause, "e.printStackTrace();");
		quickFixCore.applyChange();
	}

	@Test
	public void testAddThrowRefinedExceptionInCatchClause() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddThrowExceptionInCatchClause() {
		fail("Not yet implemented");
	}

	@Test
	public void testSetJavaFileModifiable() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCompilationUnit() {
		fail("Not yet implemented");
	}

	@Test
	public void testGenerateBigOuterTryStatement() {
		fail("Not yet implemented");
	}
	
	@Ignore
	public void testGetChange() throws Exception {
		QuickFixCore quickfixCore = new QuickFixCore();
//		quickfixCore.setJavaFileModifiable(resource);
		IFile file = (IFile) PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow().getActivePage().getActiveEditor()
				.getEditorInput().getAdapter(IFile.class);
		
		assertNull(file);
		
		Method getChange = QuickFixCore.class.getDeclaredMethod("getChange", CompilationUnit.class, ASTRewrite.class);
		getChange.setAccessible(true);
//		Change change = (Change)getChange.invoke(quickfixCore, quickfixCore.getCompilationUnit(), null);
	}

	@Ignore
	public void testPerformChange() throws Exception {
		fail("�ثe�����D�p��bUnit Test�����EditorPart�A�ҥH����@");
	}
	
	@Ignore
	public void testApplyChange() throws Exception {
		fail("�ثe�����D�p��bUnit Test�����EditorPart�A�ҥH����@");
	}
	
}
