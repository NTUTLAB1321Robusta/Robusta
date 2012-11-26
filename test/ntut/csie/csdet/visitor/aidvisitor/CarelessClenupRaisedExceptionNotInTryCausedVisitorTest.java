package ntut.csie.csdet.visitor.aidvisitor;


import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupCloseInFinallyRaisedExceptionNotInTryExample;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CarelessClenupRaisedExceptionNotInTryCausedVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	CarelessClenupRaisedExceptionNotInTryCausedVisitor visitor;
	SmellSettings smellSettings;
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "CarelessCleanupCloseInFinallyRaisedExceptionNotInTryExample";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);
		javaProjectMaker.setJREDefaultContainer();
		
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		javaFile2String.read(CarelessCleanupCloseInFinallyRaisedExceptionNotInTryExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				CarelessCleanupCloseInFinallyRaisedExceptionNotInTryExample.class.getPackage().getName(),
				CarelessCleanupCloseInFinallyRaisedExceptionNotInTryExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + CarelessCleanupCloseInFinallyRaisedExceptionNotInTryExample.class.getPackage().getName() + ";" 
				+ String.format("%n") + javaFile2String.getFileContent());
		javaFile2String.clear();
		
		Path ccExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(CarelessCleanupCloseInFinallyRaisedExceptionNotInTryExample.class, testProjectName));

		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(ccExamplePath)));
		parser.setResolveBindings(true);
		CreateSettings();
		// ���oAST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}

	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);		
	}

	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
		File settingFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(settingFile.exists()) {
			assertTrue(settingFile.delete());
		}
	}

	@Test
	public void testCloseResourceInFinallyRaisedExceptionNotInTry() throws Exception {
		MethodDeclaration md = (MethodDeclaration) NodeFinder.perform(compilationUnit, 210, 445);
		MethodInvocation methodInvocation = (MethodInvocation) NodeFinder.perform(compilationUnit, 634, 11);
		List<MethodInvocation> miList = new ArrayList<MethodInvocation>();
		miList.add(methodInvocation);
		visitor = new CarelessClenupRaisedExceptionNotInTryCausedVisitor(miList);
		md.accept(visitor);
		assertEquals(1, visitor.getCarelessCleanupNodes().size());
		assertEquals(methodInvocation ,visitor.getCarelessCleanupNodes().get(0));
	}
	
	@Test
	public void testCloseResourceInFinallyRaisedExceptionNotInTry_normalCase() throws Exception {
		MethodDeclaration md = (MethodDeclaration) NodeFinder.perform(compilationUnit, 661, 396);
		MethodInvocation methodInvocation = (MethodInvocation) NodeFinder.perform(compilationUnit, 1036, 11);
		List<MethodInvocation> miList = new ArrayList<MethodInvocation>();
		miList.add(methodInvocation);
		visitor = new CarelessClenupRaisedExceptionNotInTryCausedVisitor(miList);
		md.accept(visitor);
		assertEquals(0, visitor.getCarelessCleanupNodes().size());
	}
}
