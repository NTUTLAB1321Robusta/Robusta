package ntut.csie.csdet.visitor;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
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

public class IgnoreExceptionVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	IgnoreExceptionVisitor ignoreExceptionVisitor;
	
	public IgnoreExceptionVisitorTest() {
	}
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "IgnoredExceptionTest";
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();
		
		// �s�W�����J��library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");

		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		javaFile2String = new JavaFileToString();
		javaFile2String.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				DummyAndIgnoreExample.class.getPackage().getName(),
				DummyAndIgnoreExample.class.getSimpleName() +  JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + DummyAndIgnoreExample.class.getPackage().getName() + ";\n"
						+ javaFile2String.getFileContent());
		
		Path path = new Path(testProjectName + "/"
				+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(DummyAndIgnoreExample.class.getName())
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

	@After
	public void tearDown() throws Exception {
		File smellSettingsFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(smellSettingsFile.exists()) {
			smellSettingsFile.delete();
		}
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testVisitNode_withSettingFileAndIsDetectingFalse() {
		int ignoredSmellCount = 0;
		SmellSettings smellSetting = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSetting.setSmellTypeAttribute(SmellSettings.SMELL_IGNORECHECKEDEXCEPTION, SmellSettings.ATTRIBUTE_ISDETECTING, String.valueOf(false));
		smellSetting.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		assertTrue(new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH).exists());
		ignoreExceptionVisitor = new IgnoreExceptionVisitor(compilationUnit);
		compilationUnit.accept(ignoreExceptionVisitor);
		if(ignoreExceptionVisitor.getIgnoreList() != null)
			ignoredSmellCount = ignoreExceptionVisitor.getIgnoreList().size();
		
		// �����`�@���X��bad smell
		assertEquals(0, ignoredSmellCount);
	}
	
	@Test
	public void testVisitNode_withSettingFileAndIsDetectingTrue() {
		int ignoredSmellCount = 0;
		SmellSettings smellSetting = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSetting.setSmellTypeAttribute(SmellSettings.SMELL_IGNORECHECKEDEXCEPTION, SmellSettings.ATTRIBUTE_ISDETECTING, String.valueOf(true));
		smellSetting.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		assertTrue(new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH).exists());
		ignoreExceptionVisitor = new IgnoreExceptionVisitor(compilationUnit);
		compilationUnit.accept(ignoreExceptionVisitor);
		if(ignoreExceptionVisitor.getIgnoreList() != null)
			ignoredSmellCount = ignoreExceptionVisitor.getIgnoreList().size();
		
		// �����`�@���X��bad smell
		assertEquals(1, ignoredSmellCount);
	}

	@Test
	public void testVisitNode_withoutSettingFile() {
		int ignoredSmellCount = 0;
		ignoreExceptionVisitor = new IgnoreExceptionVisitor(compilationUnit);
		compilationUnit.accept(ignoreExceptionVisitor);
		if(ignoreExceptionVisitor.getIgnoreList() != null)
			ignoredSmellCount = ignoreExceptionVisitor.getIgnoreList().size();
		
		// �����`�@���X��bad smell
		assertEquals(0, ignoredSmellCount);
	}
}
