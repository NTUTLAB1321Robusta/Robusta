package ntut.csie.csdet.quickfix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTry;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTryAtLastStatement;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithTryAtMiddleStatement;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutCatchExceptionExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutStatementExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedmainProgramWithTryAtFirstStatement;
import ntut.csie.rleht.builder.ASTMethodCollector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UMQuickFixTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit unit1, unit2, unit3, unit4, unit5, unit6, unit7, unit8;
	SmellSettings smellSettings;
	UMQuickFix umFix;

	@Before
	public void setUp() throws Exception {
		String projectName = "UnprotectedMainProgramTest";
		javaFile2String = new JavaFileToString();
		javaProjectMaker = new JavaProjectMaker(projectName);
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.LIB_JAR_FOLDERNAME, JavaProjectMaker.BIN_CLASS_FOLDERNAME);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/lib/RL.jar");
		javaProjectMaker.setJREDefaultContainer();
		// 根據測試檔案樣本內容建立新的檔案
		// unit1
		javaFile2String.read(UnprotectedMainProgramExample.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramExample.class.getPackage().getName()
				, UnprotectedMainProgramExample.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit2
		javaFile2String.read(UnprotectedMainProgramWithoutCatchExceptionExample.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutCatchExceptionExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutCatchExceptionExample.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithoutCatchExceptionExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit3
		javaFile2String.read(UnprotectedMainProgramWithoutStatementExample.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutStatementExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutStatementExample.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithoutStatementExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit4
		javaFile2String.read(UnprotectedMainProgramWithoutTryExample.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutTryExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutTryExample.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithoutTryExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit5
		javaFile2String.read(UnprotectedmainProgramWithTryAtFirstStatement.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedmainProgramWithTryAtFirstStatement.class.getPackage().getName()
				, UnprotectedmainProgramWithTryAtFirstStatement.class.getSimpleName() + ".java"
				, "package " + UnprotectedmainProgramWithTryAtFirstStatement.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit6
		javaFile2String.read(UnprotectedMainProgramWithTryAtMiddleStatement.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTryAtMiddleStatement.class.getPackage().getName()
				, UnprotectedMainProgramWithTryAtMiddleStatement.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithTryAtMiddleStatement.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit7
		javaFile2String.read(UnprotectedMainProgramWithTryAtLastStatement.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTryAtLastStatement.class.getPackage().getName()
				, UnprotectedMainProgramWithTryAtLastStatement.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithTryAtLastStatement.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// unit8
		javaFile2String.read(UnprotectedMainProgramWithTry.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithTry.class.getPackage().getName()
				, UnprotectedMainProgramWithTry.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithTry.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		javaFile2String.clear();
		// 建立XML
		CreateSettings();
		/** unit1 */ 
		Path path1 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramExample.class.getSimpleName() + ".java");
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path1)));
		parser.setResolveBindings(true);
		// 取得AST
		unit1 = (CompilationUnit) parser.createAST(null); 
		unit1.recordModifications();
		/** unit2 */
		Path path2 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithoutCatchExceptionExample.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path2)));
		parser.setResolveBindings(true);
		// 取得AST
		unit2 = (CompilationUnit) parser.createAST(null); 
		unit2.recordModifications();
		/** unit3 */
		Path path3 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithoutStatementExample.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path3)));
		parser.setResolveBindings(true);
		// 取得AST
		unit3 = (CompilationUnit) parser.createAST(null); 
		unit3.recordModifications();
		/** unit4 */
		Path path4 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithoutTryExample.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path4)));
		parser.setResolveBindings(true);
		// 取得AST
		unit4 = (CompilationUnit) parser.createAST(null);
		unit4.recordModifications();
		/** unit5 */
		Path path5 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedmainProgramWithTryAtFirstStatement.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path5)));
		parser.setResolveBindings(true);
		// 取得AST
		unit5 = (CompilationUnit) parser.createAST(null);
		unit5.recordModifications();
		/** unit6 */
		Path path6 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithTryAtMiddleStatement.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path6)));
		parser.setResolveBindings(true);
		// 取得AST
		unit6 = (CompilationUnit) parser.createAST(null);
		unit6.recordModifications();
		/** unit7 */
		Path path7 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithTryAtLastStatement.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path7)));
		parser.setResolveBindings(true);
		// 取得AST
		unit7 = (CompilationUnit) parser.createAST(null);
		unit7.recordModifications();
		/** unit8 */
		Path path8 = new Path(projectName + "/src/ntut/csie/filemaker/exceptionBadSmells/UnprotectedMainProgram/" + UnprotectedMainProgramWithTry.class.getSimpleName() + ".java");
		//Create AST to parse
		parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path8)));
		parser.setResolveBindings(true);
		// 取得AST
		unit8 = (CompilationUnit) parser.createAST(null);
		unit8.recordModifications();
		
		umFix = new UMQuickFix("");
	}
	
	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		// 如果xml檔案存在，則刪除之
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// 刪除專案
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testAddCatchBody() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(umFix, unit5);
		
		Field rewrite = UMQuickFix.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		rewrite.set(umFix, ASTRewrite.create(unit5.getAST()));
		
		/* case 1 : there is a try statement and catch exception type, but it misplace */
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit5.accept(methodCollector);
		MethodDeclaration md = (MethodDeclaration)methodCollector.getMethodList().get(0);
		// check precondition
		assertEquals(	"public static void main(String[] args){\n" +
						"  try {\n" +
						"    int i=0;\n" +
						"    i=i++;\n" +
						"  }\n" +
					 	" catch (  Exception ex) {\n" +
					 	"  }\n" +
					 	"  UnprotectedMainProgramWithoutCatchExceptionExample test=new UnprotectedMainProgramWithoutCatchExceptionExample();\n" +
					 	"  test.toString();\n" +
						"}\n", md.toString());
		// test target
		Method addCatchBody = UMQuickFix.class.getDeclaredMethod("addCatchBody", TryStatement.class);
		addCatchBody.setAccessible(true);
		addCatchBody.invoke(umFix, (TryStatement)md.getBody().statements().get(0));
		// check postcondition
		// 修改的AST tree沒辦法回寫到檔上或是主要的AST中，所以沒辦法檢查最後修改完的程式碼是否如預期，
		// 只好去ASTRewrite中，找出我們修改的內容，來確定是否真的有修改
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[1].toString().contains("// TODO: handle exception"));
		assertEquals(9, unit5.getLineNumber(247));
		
		/* case 2 : there is a try statement, but it has no one catch clause to catch Exception type */
		methodCollector = new ASTMethodCollector();
		unit2.accept(methodCollector);
		md = (MethodDeclaration)methodCollector.getMethodList().get(0);
		actRoot.set(umFix, unit2);
		rewrite.set(umFix, ASTRewrite.create(unit2.getAST()));
		// check precondition
		assertEquals(	"public static void main(String[] args){\n" +
						"  try {\n" +
						"    UnprotectedMainProgramWithoutCatchExceptionExample test=new UnprotectedMainProgramWithoutCatchExceptionExample();\n" +
						"    test.toString();\n" +
						"  }\n" +
						" catch (  RuntimeException ex) {\n" +
						"  }\n" +
						"}\n", md.toString());
		//test target
		addCatchBody.invoke(umFix, (TryStatement)md.getBody().statements().get(0));
		// check postcondition
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[1].toString().contains("catch (Exception exxxxx)"));
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[2].toString().contains("// TODO: handle exception"));
	}
	
	@Test
	public void testMoveTryBlock() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(umFix, unit5);
		
		Field rewrite = UMQuickFix.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		rewrite.set(umFix, ASTRewrite.create(unit5.getAST()));
		
		int statementPos = 0;
		
		/* case 1 : try statement at first statement */
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit5.accept(methodCollector);
		MethodDeclaration md = (MethodDeclaration)methodCollector.getMethodList().get(statementPos);
		// check precondition
		ASTRewrite astRewrite = (ASTRewrite)rewrite.get(umFix);
		ListRewrite listRewrite = astRewrite.getListRewrite(md.getBody(), Block.STATEMENTS_PROPERTY);
		assertEquals(	"try {\n" +
						"  int i=0;\n" +
						"  i=i++;\n" +
						"}\n" +
						" catch (Exception ex) {\n" +
						"}\n", md.getBody().statements().get(statementPos).toString());
		// test target
		Method moveTryBlock = UMQuickFix.class.getDeclaredMethod("moveTryBlock", List.class, int.class, ListRewrite.class);
		moveTryBlock.setAccessible(true);
		moveTryBlock.invoke(umFix, md.getBody().statements(), statementPos, listRewrite);
		// check postcondition
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[3].toString().contains("// TODO: handle exception"));
		
		/* case 2 : try statement at last statement */
		actRoot.set(umFix, unit7);
		rewrite.set(umFix, ASTRewrite.create(unit7.getAST()));
		statementPos = 2;
		methodCollector = new ASTMethodCollector();
		unit7.accept(methodCollector);
		md = (MethodDeclaration)methodCollector.getMethodList().get(0);
		// check precondition
		astRewrite = (ASTRewrite)rewrite.get(umFix);
		listRewrite = astRewrite.getListRewrite(md.getBody(), Block.STATEMENTS_PROPERTY);
		assertEquals(	"try {\n" +
						"  int i=0;\n" +
						"  i=i++;\n" +
						"}\n" +
						" catch (RuntimeException ex) {\n" +
						"}\n", md.getBody().statements().get(statementPos).toString());
		// test target
		moveTryBlock.invoke(umFix, md.getBody().statements(), statementPos, listRewrite);
		// check postcondition
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[5].toString().contains("catch (Exception exxxxx)"));
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[6].toString().contains("// TODO: handle exception"));
		
		/* case 3 : try statement at last statement */
		actRoot.set(umFix, unit6);
		rewrite.set(umFix, ASTRewrite.create(unit6.getAST()));
		statementPos = 1;
		methodCollector = new ASTMethodCollector();
		unit6.accept(methodCollector);
		md = (MethodDeclaration)methodCollector.getMethodList().get(0);
		// check precondition
		astRewrite = (ASTRewrite)rewrite.get(umFix);
		listRewrite = astRewrite.getListRewrite(md.getBody(), Block.STATEMENTS_PROPERTY);
		assertEquals(	"try {\n" +
						"  int i=0;\n" +
						"  i=i++;\n" +
						"}\n" +
						" catch (RuntimeException ex) {\n" +
						"}\n", md.getBody().statements().get(statementPos).toString());
		// test target
		moveTryBlock.invoke(umFix, md.getBody().statements(), statementPos, listRewrite);
		// check postcondition
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[7].toString().contains("catch (Exception exxxxx)"));
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[8].toString().contains("// TODO: handle exception"));
	}
	
	@Test
	public void testAddNewTryBlock() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(umFix, unit4);
		
		Field rewrite = UMQuickFix.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		rewrite.set(umFix, ASTRewrite.create(unit4.getAST()));
		
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit4.accept(methodCollector);
		MethodDeclaration md = (MethodDeclaration)methodCollector.getMethodList().get(0);
		// check precondition
		assertEquals(	"public static void main(String[] args){\n" +
						"  UnprotectedMainProgramWithoutTryExample test=new UnprotectedMainProgramWithoutTryExample();\n" +
						"  test.toString();\n" +
						"}\n", md.toString());
		ASTRewrite astRewrite = (ASTRewrite)rewrite.get(umFix);
		ListRewrite listRewrite = astRewrite.getListRewrite(md.getBody(), Block.STATEMENTS_PROPERTY);
		// test target
		Method addNewTryBlock = UMQuickFix.class.getDeclaredMethod("addNewTryBlock", ListRewrite.class);
		addNewTryBlock.setAccessible(true);
		addNewTryBlock.invoke(umFix, listRewrite);
		// check postcondition
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[1].toString().contains("try {"));
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[3].toString().contains("catch (Exception ex)"));
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[4].toString().contains("// TODO: handle exception"));
	}
	
	@Test
	public void testAddBigOuterTry() throws Exception {
		Field actRoot = BaseQuickFix.class.getDeclaredField("actRoot");
		actRoot.setAccessible(true);
		actRoot.set(umFix, unit8);
		/* case 1 : there already has a try statement */
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit8.accept(methodCollector);
		MethodDeclaration md = (MethodDeclaration)methodCollector.getMethodList().get(0);
		// check precondition
		assertEquals(	"public static void main(String[] args){\n" +
						"  int i=0, j=0, k=0;\n" +
						"  try {\n" +
						"    i=i++;\n" +
						"  }\n" +
						" catch (  RuntimeException ex) {\n" +
						"  }\n" +
						"  UnprotectedMainProgramWithoutCatchExceptionExample test=new UnprotectedMainProgramWithoutCatchExceptionExample();\n" +
						"  try {\n" +
						"    j=j++;\n" +
						"  }\n" +
						" catch (  RuntimeException ex) {\n" +
						"  }\n" +
						"  test.toString();\n" +
						"  try {\n" +
						"    k=k++;\n" +
						"  }\n" +
						" catch (  RuntimeException ex) {\n" +
						"  }\n" +
						"}\n", md.toString());
		Field currentMethodNode = BaseQuickFix.class.getDeclaredField("currentMethodNode");
		currentMethodNode.setAccessible(true);
		currentMethodNode.set(umFix, md);
		// test target
		Method addBigOuterTry = UMQuickFix.class.getDeclaredMethod("addBigOuterTry");
		addBigOuterTry.setAccessible(true);
		addBigOuterTry.invoke(umFix);
		// check postcondition
		Field rewrite = UMQuickFix.class.getDeclaredField("rewrite");
		rewrite.setAccessible(true);
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[7].toString().contains("catch (Exception exxxxx)"));
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[8].toString().contains("// TODO: handle exception"));
		
		/* case 2 : there has no try statement */
		actRoot.set(umFix, unit4);
		methodCollector = new ASTMethodCollector();
		unit4.accept(methodCollector);
		md = (MethodDeclaration)methodCollector.getMethodList().get(0);
		// check precondition
		assertEquals(	"public static void main(String[] args){\n" +
						"  UnprotectedMainProgramWithoutTryExample test=new UnprotectedMainProgramWithoutTryExample();\n" +
						"  test.toString();\n" +
						"}\n", md.toString());
		currentMethodNode.set(umFix, md);
		// test target
		addBigOuterTry.invoke(umFix);
		// check postcondition
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[3].toString().contains("catch (Exception ex)"));
		assertTrue(((ASTRewrite)rewrite.get(umFix)).rewriteAST().getChildren()[4].toString().contains("// TODO: handle exception"));
	}
	
	@Test
	public void testRun() throws Exception {
		fail("因為applyChange也存在focus edit part問題，故尚未實作");
	}
	
	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.setSmellTypeAttribute(SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM, SmellSettings.ATTRIBUTE_ISDETECTING, "true");
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
