package ntut.csie.csdet.visitor;

import static org.junit.Assert.*;

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
		String projectName = "NestedTryStatementExampleProject";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.LIB_JAR_FOLDERNAME, JavaProjectMaker.BIN_CLASS_FOLDERNAME);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/lib/RL.jar");
		javaProjectMaker.setJREDefaultContainer();
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		javaFile2String.read(NestedTryStatementExample.class, "test");
		javaProjectMaker.createJavaFile(
				NestedTryStatementExample.class.getPackage().getName(),
				NestedTryStatementExample.class.getSimpleName() + ".java",
				"package " + NestedTryStatementExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		createSettings();
		
		Path nestedTryExamplePath = new Path(projectName
				+ "/src/"
				+ PathUtils.dot2slash(NestedTryStatementExample.class
						.getPackage().getName()) + "/"
				+ NestedTryStatementExample.class.getSimpleName().toString() + ".java");
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// �]�w�n�Q�إ�AST���ɮ�
		parser.setSource(
				JavaCore.createCompilationUnitFrom(
						ResourcesPlugin.getWorkspace().
						getRoot().getFile(nestedTryExamplePath)));
		parser.setResolveBindings(true);
		// �إ�XML
		createSettings();
		// ���oAST
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
						24, nestedTryStatementCount); 
	}
	
	/**
	 * �����Ҧ�badSmell���e�H�Φ渹
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

	private void createSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYBLOCK, SmellSettings.ATTRIBUTE_ISDETECTING, String.valueOf(true));
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
