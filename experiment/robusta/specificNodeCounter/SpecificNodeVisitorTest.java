package robusta.specificNodeCounter;

import static org.junit.Assert.*;

import java.io.File;

import ntut.csie.csdet.visitor.IgnoreExceptionVisitor;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.IgnoredCheckedExceptionExample;
import ntut.csie.filemaker.exceptionBadSmells.UserDefineDummyHandlerFish;
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

public class SpecificNodeVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	SpecificNodeVisitor specificNodeVisitor;

	@Before
	public void setUp() throws Exception {
		String testProjectName = "SpecificNodeTest";
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();
		
		// 根據測試檔案樣本內容建立新的檔案
		javaFile2String = new JavaFileToString();
		javaFile2String.read(SpecificNodeExample.class, JavaProjectMaker.FOLDERNAME_EXPERIMENT);
		javaProjectMaker.createJavaFile(
				SpecificNodeExample.class.getPackage().getName(),
				SpecificNodeExample.class.getSimpleName() +  JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + SpecificNodeExample.class.getPackage().getName() + ";\n"
						+ javaFile2String.getFileContent());

		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(SpecificNodeExample.class, testProjectName));
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
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
	public void testVisitTryStatement() {
		specificNodeVisitor = new SpecificNodeVisitor(ASTNode.TRY_STATEMENT);
		compilationUnit.accept(specificNodeVisitor);
		assertEquals(3, specificNodeVisitor.getCount());
	}

	@Test
	public void testVisitTryStatement1() {
		specificNodeVisitor = new SpecificNodeVisitor(ASTNode.CATCH_CLAUSE);
		compilationUnit.accept(specificNodeVisitor);
		assertEquals(45, specificNodeVisitor.getCount());
	}
}
