package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.OverwrittenLeadExceptionExample;
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

public class OverwrittenLeadExceptionVisitorTest {
	JavaProjectMaker javaProjectMaker;
	JavaFileToString javaFile2String;
	CompilationUnit compilationUnit;
	OverwrittenLeadExceptionVisitor overwrittenLeadExceptionVisitor;
	SmellSettings smellSettings;
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "OverwrittenLeadExceptionTest";
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);
		javaProjectMaker.setJREDefaultContainer();
		
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String = new JavaFileToString();
		javaFile2String.read(OverwrittenLeadExceptionExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				OverwrittenLeadExceptionExample.class.getPackage().getName(),
				OverwrittenLeadExceptionExample.class.getSimpleName() +  JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + OverwrittenLeadExceptionExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(OverwrittenLeadExceptionExample.class, testProjectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		CreateSettings(true);
	}
	
	@After
	public void tearDown() throws Exception {
		File smellSettingsFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(smellSettingsFile.exists())
			smellSettingsFile.delete();
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void visitWithoutSettings() throws Exception {
		CreateSettings(false);
		overwrittenLeadExceptionVisitor = new OverwrittenLeadExceptionVisitor(compilationUnit);
		compilationUnit.accept(overwrittenLeadExceptionVisitor);
		assertEquals(0, overwrittenLeadExceptionVisitor.getOverwrittenList().size());
	}
	
	@Test
	public void visitWithSettings() throws Exception {
		overwrittenLeadExceptionVisitor = new OverwrittenLeadExceptionVisitor(compilationUnit);
		compilationUnit.accept(overwrittenLeadExceptionVisitor);
		assertEquals(colloectBadSmellListContent(overwrittenLeadExceptionVisitor.getOverwrittenList()), 28, overwrittenLeadExceptionVisitor.getOverwrittenList().size());
	}

	/**
	 * 紀錄所有badSmell內容以及行號
	 * @param badSmellList
	 * @return
	 */
	private String colloectBadSmellListContent(List<MarkerInfo> badSmellList) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		for (int i = 0; i < badSmellList.size(); i++) {
			MarkerInfo m = badSmellList.get(i);
			sb.append(m.getLineNumber()).append("\t").append(m.getStatement()).append("\n");
		}
		return sb.toString();
	}
	
	private void CreateSettings(boolean isDetecting) {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_OVERWRITTENLEADEXCEPTION, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}