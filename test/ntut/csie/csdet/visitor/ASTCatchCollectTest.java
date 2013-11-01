package ntut.csie.csdet.visitor;

import java.io.File;

import junit.framework.TestCase;
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
/**
 * 測試去讀取一個MM.java的檔案中catch的數量
 * @author chewei
 */

public class ASTCatchCollectTest extends TestCase {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	SmellSettings smellSettings;
	ASTCatchCollect catchCollector;
	
	protected void setUp() throws Exception {
		String testProjectName = "ASTCatchCollectExampleProject";
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
		catchCollector = new ASTCatchCollect();
	}

	protected void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
		File settingFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(settingFile.exists()) {
			assertTrue(settingFile.delete());
		}
	}

	public void testASTCatchCollect() {
		compilationUnit.accept(catchCollector);
		assertEquals(65, catchCollector.getMethodList().size());
	}

	private void createSettings(boolean isDetecting) {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_NESTEDTRYSTATEMENT, SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
