package ntut.csie.robusta.codegen;


import static org.junit.Assert.*;

import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.sampleCode4VisitorTest.VariableDeclarationStatementSampleCode;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class VariableDeclarationStatementFinderVisitorTest {
	String testProjectName;
	JavaProjectMaker javaProjectMaker;
	JavaFileToString javaFile2String;
	CompilationUnit compilationUnit;
	VariableDeclarationStatementFinderVisitor variableDeclarationStatementFinder;
	SmellSettings smellSettings;
	List<ASTNode> methodCollectList;
	
	@Before
	public void setUp() throws Exception {
		testProjectName = "VariableDeclarationStatementFinderVisitor";
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();
		// 建立新的檔案DummyAndIgnoreExample
		javaFile2String = new JavaFileToString();
		javaFile2String.read(VariableDeclarationStatementSampleCode.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				VariableDeclarationStatementSampleCode.class.getPackage().getName(),
				VariableDeclarationStatementSampleCode.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + VariableDeclarationStatementSampleCode.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(VariableDeclarationStatementSampleCode.class, testProjectName));
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
		javaProjectMaker.deleteProject();
	}

	/**
	 * 變數是在class裡面被宣告的，所以會是一個FieldDeclaration，用這個Visitor就什麼都找不到。
	 */
	@Test
	public void testVariableDeclarationStatementFinder_Field() {
		// fieldString.toLowerCase()
		MethodInvocation mi = (MethodInvocation)NodeFinder.perform(compilationUnit, 318 - 1, 25);
		VariableDeclarationStatementFinderVisitor vdsfv = new VariableDeclarationStatementFinderVisitor(mi);
		compilationUnit.accept(vdsfv);
		assertNull(vdsfv.getFoundVariableDeclarationStatement());
	}
	
	@Test
	public void testVariableDeclarationStatementFinder_Foundable() {
		// localString.getBytes();
		MethodInvocation mi = (MethodInvocation)NodeFinder.perform(compilationUnit, 481 - 1, 22);
		VariableDeclarationStatementFinderVisitor vdsfv = new VariableDeclarationStatementFinderVisitor(mi);
		compilationUnit.accept(vdsfv);
		assertEquals("String localString=\"千山鳥飛絕\";\n", vdsfv.getFoundVariableDeclarationStatement().toString());
	}
	
	/**
	 * 檢查不同的MethodDeclaration擁有相同程式碼的宣告時，會不會出錯。
	 * 從MD1的mi尋找他的VariableDeclarationStatement
	 */
	@Test
	public void testVariableDeclarationStatementFinder_DifferentMethodDeclaraionOwnsSameCode_1st() {
		// sameName.toCharArray(); in declareSameNameInstanceInDifferentMethodDeclaration_MD1()
		MethodInvocation mi = (MethodInvocation)NodeFinder.perform(compilationUnit, 656 - 1, 22);
		VariableDeclarationStatementFinderVisitor vdsfv = new VariableDeclarationStatementFinderVisitor(mi);
		compilationUnit.accept(vdsfv);
		/*
		 * String sameName = "一粥一飯，當思來處不易"; in declareSameNameInstanceInDifferentMethodDeclaration_MD1()
		 * Start position is 620, 32
		 */
		assertEquals((620 - 1), vdsfv.getFoundVariableDeclarationStatement().getStartPosition());
	}
	
	/**
	 * @see #testVariableDeclarationStatementFinder_DifferentMethodDeclaraionOwnsSameCode_1st()
	 * 從MD2的mi尋找他的VariableDeclarationStatement
	 */
	@Test
	public void testVariableDeclarationStatementFinder_DifferentMethodDeclaraionOwnsSameCode_2rd() {
		// sameName.toCharArray(); in declareSameNameInstanceInDifferentMethodDeclaration_MD2()
		MethodInvocation mi = (MethodInvocation)NodeFinder.perform(compilationUnit, 800 - 1, 22);
		VariableDeclarationStatementFinderVisitor vdsfv = new VariableDeclarationStatementFinderVisitor(mi);
		compilationUnit.accept(vdsfv);
		/*
		 * String sameName = "一粥一飯，當思來處不易"; in declareSameNameInstanceInDifferentMethodDeclaration_MD2()
		 * Start position is 764, 32
		 */
		assertEquals((764 - 1), vdsfv.getFoundVariableDeclarationStatement().getStartPosition());
	}
	
}
