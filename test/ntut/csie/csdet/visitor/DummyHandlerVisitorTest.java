package ntut.csie.csdet.visitor;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.filemaker.exceptionBadSmells.UserDefineDummyHandlerFish;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DummyHandlerVisitorTest {
	JavaProjectMaker javaProjectMaker;
	String javaProjectName;
	JavaFileToString javaFile2String;
	CompilationUnit compilationUnit;
	DummyHandlerVisitor dummyHandlerVisitor;
	SmellSettings smellSettings;
	ASTMethodCollector methodCollector;
	List<ASTNode> methodCollectList;
//	MethodDeclaration mDeclaration;
	String[] dummyHandlerPatternsInXML;

	public DummyHandlerVisitorTest() {
		javaProjectName = "DummyHandlerTest";
	}
	
	@Before
	public void setUp() throws Exception {
		// 準備測試檔案樣本內容
		javaProjectMaker = new JavaProjectMaker(javaProjectName);
		javaProjectMaker.setJREDefaultContainer();
		
		// 新增欲載入的library
		javaProjectMaker.addJarFromProjectToBuildPath("lib/log4j-1.2.15.jar");
		
		// 建立新的檔案DummyAndIgnoreExample
		javaFile2String = new JavaFileToString();
		javaFile2String.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				DummyAndIgnoreExample.class.getPackage().getName(),
				DummyAndIgnoreExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + DummyAndIgnoreExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		// 繼續建立測試用的UserDefineDummyHandlerFish
		javaFile2String.clear();
		javaFile2String.read(UserDefineDummyHandlerFish.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefineDummyHandlerFish.class.getPackage().getName(),
				UserDefineDummyHandlerFish.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefineDummyHandlerFish.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		// 建立XML
		dummyHandlerPatternsInXML = new String[] {
				SmellSettings.EXTRARULE_ePrintStackTrace, SmellSettings.EXTRARULE_SystemErrPrint, 
				SmellSettings.EXTRARULE_SystemErrPrintln, SmellSettings.EXTRARULE_SystemOutPrint, 
				SmellSettings.EXTRARULE_SystemOutPrintln, SmellSettings.EXTRARULE_JavaUtilLoggingLogger, 
				SmellSettings.EXTRARULE_OrgApacheLog4j};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		
		Path path = new Path(javaProjectName + "/"
				+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(DummyAndIgnoreExample.class.getName())
				+ JavaProjectMaker.JAVA_FILE_EXTENSION);
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
	}

	@After
	public void tearDown() throws Exception {
		deleteOldSettings();
		// 刪除專案
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testVisitMemberData() {
		int dummy = 0;
		compilationUnit.accept(dummyHandlerVisitor);
		if(dummyHandlerVisitor.getDummyList() != null)
			dummy = dummyHandlerVisitor.getDummyList().size();
		
		// 驗證總共抓到幾個bad smell
		assertEquals(16, dummy);
		assertEquals(2, dummyHandlerVisitor.getFinallyCounter());
		assertEquals(25, dummyHandlerVisitor.getCatchCounter());
		assertEquals(25, dummyHandlerVisitor.getTryCounter());
	}
	
	@Test
	public void testVisitReturnValue() throws Exception {
		MethodDeclaration md = null;
		TryStatement tryStatement = null;
		
		// 驗證return true的非巢狀case
		md = ASTNodeFinder.getMethodDeclarationNodeByName(
				compilationUnit, "true_printStackTrace_protected");	
		tryStatement = (TryStatement) md.getBody().statements().get(1);
		assertTrue(dummyHandlerVisitor.visit(tryStatement)); 
		
		// 驗證return false的巢狀final-try case
		md = ASTNodeFinder.getMethodDeclarationNodeByName(
				compilationUnit, "true_DummyHandlerFinallyNestedTry");	
		tryStatement = (TryStatement) md.getBody().statements().get(1);
		tryStatement = (TryStatement) tryStatement.getFinally().statements().get(0);
		assertEquals(false, dummyHandlerVisitor.visit(tryStatement));

		// 驗證return false的巢狀try-try case
		md = ASTNodeFinder.getMethodDeclarationNodeByName(
				compilationUnit, "true_DummyHandlerTryNestedTry");	
		tryStatement = (TryStatement) md.getBody().statements().get(1);
		tryStatement = (TryStatement) tryStatement.getBody().statements().get(2);
		assertEquals(false, dummyHandlerVisitor.visit(tryStatement));
		
		// 驗證return false的巢狀catch-try case
		md = ASTNodeFinder.getMethodDeclarationNodeByName(
				compilationUnit, "true_DummyHandlerCatchNestedTry");		
		tryStatement = (TryStatement) md.getBody().statements().get(1);
		CatchClause catchStatement = (CatchClause) tryStatement.catchClauses().get(0);
		tryStatement = (TryStatement) catchStatement.getBody().statements().get(1);
		assertFalse(dummyHandlerVisitor.visit(tryStatement));
	}

	@Test
	public void testVisitTryStatementfinallyCounter() {
		MethodDeclaration md = ASTNodeFinder.getMethodDeclarationNodeByName(
				compilationUnit, "true_DummyHandlerFinallyNestedTry");	
		TryStatement tryStatement = (TryStatement) md.getBody().statements().get(1);
		assertEquals(0, dummyHandlerVisitor.getFinallyCounter());
		assertEquals(0, dummyHandlerVisitor.getCatchCounter());
		assertEquals(0, dummyHandlerVisitor.getTryCounter());
		assertTrue(dummyHandlerVisitor.visit(tryStatement));
		assertEquals(1, dummyHandlerVisitor.getFinallyCounter());
		assertEquals(1, dummyHandlerVisitor.getCatchCounter());
		assertEquals(1, dummyHandlerVisitor.getTryCounter());
	}
	
	@Test
	public void testVisitMethodInvocation() {
		MethodDeclaration md = ASTNodeFinder.getMethodDeclarationNodeByName(
				compilationUnit, "true_printStackTrace_public");
		ExpressionStatement eStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 0);
		MethodInvocation methodInvocation = (MethodInvocation) eStatement.getExpression();
		assertFalse(dummyHandlerVisitor.visit(methodInvocation));
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testDetectDummyHandler() {
		// 確認初始值
		MethodDeclaration md = null;
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		//#1 正常的DummyHandler
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_printStackTrace_public");
		ExpressionStatement eStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 0);
		dummyHandlerVisitor.detectDummyHandler(eStatement);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
		
		//#2 有throw
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "false_throwAndPrint");
		eStatement = getExpressionStatementFromMethodDeclaration(md, 0, 0, 0);
		dummyHandlerVisitor.detectDummyHandler(eStatement);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
		
		//#3 測 Catch 外面
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_printStackTrace_protected");
		TryStatement tryStatement = (TryStatement) md.getBody().statements().get(1);
		eStatement = (ExpressionStatement)tryStatement.getBody().statements().get(1);
		dummyHandlerVisitor.detectDummyHandler(eStatement);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_PrintStackTrace() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", ExpressionStatement.class);
		method.setAccessible(true);
		ExpressionStatement expressionStatement;
		
		// 確認初始值
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		// ePrintStackTrace case
		dummyHandlerPatternsInXML = new String[] {SmellSettings.EXTRARULE_ePrintStackTrace};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   分別測試：全部例子、符合例子、不符合例子 是否有抓出
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(7, dummyHandlerVisitor.getDummyList().size());
		
		MethodDeclaration md = null;
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_printStackTrace_protected");  // ePrintStackTrace
		expressionStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 0);
		method.invoke(dummyHandlerVisitor, expressionStatement);
		assertEquals(8, dummyHandlerVisitor.getDummyList().size());

		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_systemTrace");  // SystemPrint
		expressionStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 0);
		method.invoke(dummyHandlerVisitor, expressionStatement);
		assertEquals(8, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_SystemOutPrint() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", ExpressionStatement.class);
		method.setAccessible(true);
		ExpressionStatement expressionStatement;
		
		// 確認初始值
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		dummyHandlerPatternsInXML = new String[] {
				SmellSettings.EXTRARULE_SystemErrPrint, SmellSettings.EXTRARULE_SystemErrPrintln,
				SmellSettings.EXTRARULE_SystemOutPrint, SmellSettings.EXTRARULE_SystemOutPrint};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   分別測試：全部例子、符合例子、不符合例子 是否有抓出
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(6, dummyHandlerVisitor.getDummyList().size());

		MethodDeclaration md;
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_systemTrace");  // SystemPrint
		expressionStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 0);
		method.invoke(dummyHandlerVisitor, expressionStatement);
		assertEquals(7, dummyHandlerVisitor.getDummyList().size());

		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_printStackTrace_protected");  // ePrintStackTrace
		expressionStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 0);
		method.invoke(dummyHandlerVisitor, expressionStatement);
		assertEquals(7, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_JavaUtilLoggingLogger() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", ExpressionStatement.class);
		method.setAccessible(true);
		ExpressionStatement expressionStatement;
		
		// 確認初始值
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		// JavaUtilLoggingLogger case
		dummyHandlerPatternsInXML = new String[] {SmellSettings.EXTRARULE_JavaUtilLoggingLogger};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   分別測試：全部例子、符合例子、不符合例子 是否有抓出
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());

		MethodDeclaration md;
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_javaLogInfo");  // JavaUtilLoggingLogger
		expressionStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 0);
		method.invoke(dummyHandlerVisitor, expressionStatement);
		assertEquals(3, dummyHandlerVisitor.getDummyList().size());

		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_Log4J");  // OrgApacheLog4j
		expressionStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 0);
		method.invoke(dummyHandlerVisitor, expressionStatement);
		assertEquals(3, dummyHandlerVisitor.getDummyList().size());

	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_OrgApacheLog4j() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", ExpressionStatement.class);
		method.setAccessible(true);
		ExpressionStatement expressionStatement;
		
		// 確認初始值
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		// OrgApacheLog4j case
		dummyHandlerPatternsInXML = new String[] {SmellSettings.EXTRARULE_OrgApacheLog4j};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   分別測試：全部例子、符合例子、不符合例子 是否有抓出
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());

		MethodDeclaration md;
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_Log4J");  // OrgApacheLog4j
		expressionStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 0);
		method.invoke(dummyHandlerVisitor, expressionStatement);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());

		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_javaLogInfo");  // JavaUtilLoggingLogger
		expressionStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 0);
		method.invoke(dummyHandlerVisitor, expressionStatement);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());
	}

	/**
	 * 測試當使用者自訂的type1是否能抓到Outer class
	 * 未測試完成
	 */
	@Test
	public void testAddDummyHandlerSmellInfoForUserPatternType1() {
//		java.util.* case1 class
//		*.toString case2 statement
//		java.lang.String.toString case3 statement
//		java.util.ArrayList<java.lang.Boolean> case4 statement
		
		String testClassPattern = UserDefineDummyHandlerFish.class.getName() + ".*";
		
		// 確認初始值
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());

		// 確認全部的Example中恰有偵測到兩次呼叫
		smellSettings.addDummyHandlerPattern(testClassPattern, true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());
		
		// 確認當使用者未勾選時不會偵測到
		setEmptySetting();
		smellSettings.addDummyHandlerPattern(testClassPattern, false);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForUserPatternType2() {
		ExpressionStatement expressionStatement;
		
		// 使用者輸入為 *.toString
		//   測試全部的 Example Code
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());

		MethodDeclaration md = null;
		//   輸入一個 true case
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_systemOutPrintlnWithoutE");
		expressionStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 1);
		dummyHandlerVisitor.detectDummyHandler(expressionStatement);
		assertEquals(3, dummyHandlerVisitor.getDummyList().size());
		//   輸入一個 false case - method declaration 是 *.toCharArray
		expressionStatement = getExpressionStatementFromMethodDeclaration(md, 1, 0, 2);
		dummyHandlerVisitor.detectDummyHandler(expressionStatement);
		assertEquals(3, dummyHandlerVisitor.getDummyList().size());

		// 使用者輸入為 *.toString, 但未勾選時
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString", false);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		// 使用者輸入改為 *.toString(), 不會被視同是 *.toString
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString()", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
	}

	/**
	 * 測試當使用者自訂的type3是否能抓到
	 * 同時測試addDummyHandlerSmellInfo()中，遇到含有"<>"library會自動轉成"<"之前的子字串
	 */
	@Test
	public void testAddDummyHandlerSmellInfoForUserPatternType3WithBracket() {
		String testClassPattern = "java.util.ArrayList.add";
		setEmptySetting();
		smellSettings.addDummyHandlerPattern(testClassPattern, true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
	}

	@Test
	public void testIsThrowStatementInCatchClause() {
		TryStatement tryStatement;
		CatchClause catchStatement;

		// 測試 符合的例子 是否會抓出
		MethodDeclaration md = null;
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "false_throwAndPrint");
		tryStatement = (TryStatement) md.getBody().statements().get(0);
		catchStatement = (CatchClause) tryStatement.catchClauses().get(0);
		assertTrue(dummyHandlerVisitor.isThrowStatementInCatchClause(catchStatement));
		
		// 測試 不符合例子 是否會抓出
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "true_DummyHandlerTryNestedTry");
		tryStatement = (TryStatement) md.getBody().statements().get(1);
		catchStatement = (CatchClause) tryStatement.catchClauses().get(0);
		assertFalse(dummyHandlerVisitor.isThrowStatementInCatchClause(catchStatement));
	}

	private void deleteOldSettings() {
		File settingFile = new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		if(settingFile.exists()) {
			assertTrue(settingFile.delete());
		}
	}
	
	private void setNewSettingsWithExtraRules(String[] argForXML) {
		deleteOldSettings();
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		for (int i=0; i<argForXML.length; i++) {
			smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, argForXML[i]);
		}
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

	private void setEmptySetting() {
		deleteOldSettings();
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	private ExpressionStatement getExpressionStatementFromMethodDeclaration(
			MethodDeclaration mDeclaration, int statementsNumberOnMethodDeclaration,
			int catchClauseNumber, int statementsNumberOnCatchClause) {
		TryStatement tryStatement = (TryStatement) mDeclaration.getBody().statements().get(statementsNumberOnMethodDeclaration);
		CatchClause catchClause = (CatchClause) tryStatement.catchClauses().get(catchClauseNumber);
		return (ExpressionStatement) catchClause.getBody().statements().get(statementsNumberOnCatchClause);
	}
}
