package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.NestedTryStatementExample;
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

public class NestedTryStatementVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	NestedTryStatementVisitor nestedTryStatementVisitor;
	SmellSettings smellSettings;
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "NestedTryStatementExampleProject";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.FOLDERNAME_LIB_JAR, JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/" + JavaProjectMaker.RL_LIBRARY_PATH);
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String.read(NestedTryStatementExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				NestedTryStatementExample.class.getPackage().getName(),
				NestedTryStatementExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + NestedTryStatementExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		
		Path nestedTryExamplePath = new Path(PathUtils.getPathOfClassUnderSrcFolder(NestedTryStatementExample.class, testProjectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(nestedTryExamplePath)));
		parser.setResolveBindings(true);
		// 建立XML
		createSettings(true);
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		nestedTryStatementVisitor = new NestedTryStatementVisitor(compilationUnit);
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
	public void testNestedTryStatementVisitor() {
		int nestedTryStatementCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(nestedTryStatementVisitor);
		compilationUnit.accept(nestedTryStatementVisitor);
		if(nestedTryStatementVisitor.getNestedTryStatementList() != null) {
			nestedTryStatementCount = nestedTryStatementVisitor.getNestedTryStatementList().size();
		}
		assertEquals(
				colloectBadSmellListContent(nestedTryStatementVisitor.getNestedTryStatementList()),
						25, nestedTryStatementCount); 
	}
	
	@Test
	public void testNestedTryStatementVisitor_doNotDetect() {
		createSettings(false);
		nestedTryStatementVisitor = new NestedTryStatementVisitor(compilationUnit);

		int nestedTryStatementCount = 0;
		assertNotNull(compilationUnit);
		assertNotNull(nestedTryStatementVisitor);
		compilationUnit.accept(nestedTryStatementVisitor);
		if(nestedTryStatementVisitor.getNestedTryStatementList() != null) {
			nestedTryStatementCount = nestedTryStatementVisitor.getNestedTryStatementList().size();
		}
		assertEquals(
				colloectBadSmellListContent(nestedTryStatementVisitor.getNestedTryStatementList()),
						0, nestedTryStatementCount); 
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

	private void createSettings(boolean isDetecting) {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
