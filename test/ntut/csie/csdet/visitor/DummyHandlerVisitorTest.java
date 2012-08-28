package ntut.csie.csdet.visitor;

import static org.junit.Assert.*;
import java.io.File;
import java.lang.reflect.Method;
import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.DummyAndIgnoreExample;
import ntut.csie.filemaker.exceptionBadSmells.UserDefineDummyHandlerFish;
import ntut.csie.rleht.builder.ASTMethodCollector;

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
//	String javaFileName;
	CompilationUnit compilationUnit;
	DummyHandlerVisitor dummyhandlerBSV;
	SmellSettings smellSettings;
	ASTMethodCollector methodCollector;
	List<ASTNode> methodCollectList;
	MethodDeclaration mDeclaration;
	String[] argForSettingXML;

	public DummyHandlerVisitorTest() {
		javaProjectName = "DummyHandlerTest";
//		javaFileName = "ntut.csie.filemaker.exceptionBadSmells";
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
		javaFile2String.read(DummyAndIgnoreExample.class, "test");
//		javaProjectMaker.createJavaFile(javaFileName, "DummyAndIgnoreExample.java", 
//				"package " + javaFileName + ";\n" + javaFile2String.getFileContent());
		javaProjectMaker.createJavaFile(
				DummyAndIgnoreExample.class.getPackage().getName(),
				DummyAndIgnoreExample.class.getSimpleName() + ".java",
				"package " + DummyAndIgnoreExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		// 繼續建立測試用的UserDefineDummyHandlerFish
		javaFile2String.clear();
		javaFile2String.read(UserDefineDummyHandlerFish.class, "test");
		javaProjectMaker.createJavaFile(
				UserDefineDummyHandlerFish.class.getPackage().getName(),
				UserDefineDummyHandlerFish.class.getSimpleName() + ".java",
				"package " + UserDefineDummyHandlerFish.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
//		javaProjectMaker.createJavaFile(javaFileName, "UserDefineDummyHandlerFish.java", 
//				"package " + javaFileName + ";\n" + javaFile2String.getFileContent());

		// 建立XML
		argForSettingXML = new String[] {
				SmellSettings.EXTRARULE_ePrintStackTrace, SmellSettings.EXTRARULE_SystemErrPrint, 
				SmellSettings.EXTRARULE_SystemErrPrintln, SmellSettings.EXTRARULE_SystemOutPrint, 
				SmellSettings.EXTRARULE_SystemOutPrintln, SmellSettings.EXTRARULE_JavaUtilLoggingLogger, 
				SmellSettings.EXTRARULE_OrgApacheLog4j};
		setNewSettingsWithExtraRules(argForSettingXML);
		
		Path path = new Path("DummyHandlerTest/src/ntut/csie/filemaker/exceptionBadSmells/DummyAndIgnoreExample.java");
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// 設定要被建立AST的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		// 取得AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
		dummyhandlerBSV = new DummyHandlerVisitor(compilationUnit);
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
		compilationUnit.accept(dummyhandlerBSV);
		if(dummyhandlerBSV.getDummyList() != null)
			dummy = dummyhandlerBSV.getDummyList().size();
		
		// 驗證總共抓到幾個bad smell
		assertEquals(16, dummy);
		assertEquals(2, dummyhandlerBSV.getFinallyCounter());
		assertEquals(25, dummyhandlerBSV.getCatchCounter());
		assertEquals(25, dummyhandlerBSV.getTryCounter());
	}
	
	@Test
	public void testVisitReturnValue() throws Exception {
		initializeMethodCollectList();
		
		// 驗證return true的非巢狀case
		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_printStackTrace_protected");
		TryStatement tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		assertTrue(dummyhandlerBSV.visit(tryStatement));

		// 驗證return false的巢狀final-try case
		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_DummyHandlerFinallyNestedTry");
		tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		tryStatement = (TryStatement) tryStatement.getFinally().statements().get(0);
		assertEquals(false, dummyhandlerBSV.visit(tryStatement));

		// 驗證return false的巢狀try-try case
		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_DummyHandlerTryNestedTry");
		tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		tryStatement = (TryStatement) tryStatement.getBody().statements().get(2);
		assertEquals(false, dummyhandlerBSV.visit(tryStatement));
		
		// 驗證return false的巢狀catch-try case
		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_DummyHandlerCatchNestedTry");
		tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		CatchClause catchStatement = (CatchClause) tryStatement.catchClauses().get(0);
		tryStatement = (TryStatement) catchStatement.getBody().statements().get(1);
		assertFalse(dummyhandlerBSV.visit(tryStatement));
	}

	@Test
	public void testVisitTryStatementfinallyCounter() {
		initializeMethodCollectList();
		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_DummyHandlerFinallyNestedTry");
		TryStatement tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		assertEquals(0, dummyhandlerBSV.getFinallyCounter());
		assertEquals(0, dummyhandlerBSV.getCatchCounter());
		assertEquals(0, dummyhandlerBSV.getTryCounter());
		assertTrue(dummyhandlerBSV.visit(tryStatement));
		assertEquals(1, dummyhandlerBSV.getFinallyCounter());
		assertEquals(1, dummyhandlerBSV.getCatchCounter());
		assertEquals(1, dummyhandlerBSV.getTryCounter());
	}
	
	@Test
	public void testVisitMethodInvocation() {
		initializeMethodCollectList();
		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_printStackTrace_public");
		ExpressionStatement eStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 0);
		MethodInvocation methodInvocation = (MethodInvocation) eStatement.getExpression();
		assertFalse(dummyhandlerBSV.visit(methodInvocation));
		assertEquals(1, dummyhandlerBSV.getDummyList().size());
	}
	
	@Test
	public void testDetectDummyHandler() {
		// 確認初始值
		initializeMethodCollectList();
		assertEquals(0, dummyhandlerBSV.getDummyList().size());
		
		//#1 正常的DummyHandler
		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_printStackTrace_public");
		ExpressionStatement eStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 0);
		dummyhandlerBSV.detectDummyHandler(eStatement);
		assertEquals(1, dummyhandlerBSV.getDummyList().size());
		
		//#2 有throw
		mDeclaration = getMethodDeclarationByName(methodCollectList, "false_throwAndPrint");
		eStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 0, 0, 0);
		dummyhandlerBSV.detectDummyHandler(eStatement);
		assertEquals(1, dummyhandlerBSV.getDummyList().size());
		
		//#3 測 Catch 外面
		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_printStackTrace_protected");
		TryStatement tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		eStatement = (ExpressionStatement)tryStatement.getBody().statements().get(1);
		dummyhandlerBSV.detectDummyHandler(eStatement);
		assertEquals(1, dummyhandlerBSV.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule () throws Exception {
		initializeMethodCollectList();
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", ExpressionStatement.class);
		method.setAccessible(true);
		ExpressionStatement expressionStatement;
		
		// 確認初始值
		setEmptySetting();
		regetDummyhandlerBSV();
		assertEquals(0, dummyhandlerBSV.getDummyList().size());
		
		// ePrintStackTrace case
		argForSettingXML = new String[] {SmellSettings.EXTRARULE_ePrintStackTrace};
		setNewSettingsWithExtraRules(argForSettingXML);
		//   分別測試：全部例子、符合例子、不符合例子 是否有抓出
		regetDummyhandlerBSV();
		assertEquals(7, dummyhandlerBSV.getDummyList().size());

		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_printStackTrace_protected");  // ePrintStackTrace
		expressionStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 0);
		method.invoke(dummyhandlerBSV, expressionStatement);
		assertEquals(8, dummyhandlerBSV.getDummyList().size());

		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_systemTrace");  // SystemPrint
		expressionStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 0);
		method.invoke(dummyhandlerBSV, expressionStatement);
		assertEquals(8, dummyhandlerBSV.getDummyList().size());

		// SystemPrint case
		argForSettingXML = new String[] {
				SmellSettings.EXTRARULE_SystemErrPrint, SmellSettings.EXTRARULE_SystemErrPrintln,
				SmellSettings.EXTRARULE_SystemOutPrint, SmellSettings.EXTRARULE_SystemOutPrint};
		setNewSettingsWithExtraRules(argForSettingXML);
		//   分別測試：全部例子、符合例子、不符合例子 是否有抓出
		regetDummyhandlerBSV();
		assertEquals(6, dummyhandlerBSV.getDummyList().size());

		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_systemTrace");  // SystemPrint
		expressionStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 0);
		method.invoke(dummyhandlerBSV, expressionStatement);
		assertEquals(7, dummyhandlerBSV.getDummyList().size());

		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_printStackTrace_protected");  // ePrintStackTrace
		expressionStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 0);
		method.invoke(dummyhandlerBSV, expressionStatement);
		assertEquals(7, dummyhandlerBSV.getDummyList().size());
		
		// JavaUtilLoggingLogger case
		argForSettingXML = new String[] {SmellSettings.EXTRARULE_JavaUtilLoggingLogger};
		setNewSettingsWithExtraRules(argForSettingXML);
		//   分別測試：全部例子、符合例子、不符合例子 是否有抓出
		regetDummyhandlerBSV();
		assertEquals(2, dummyhandlerBSV.getDummyList().size());

		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_javaLogInfo");  // JavaUtilLoggingLogger
		expressionStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 0);
		method.invoke(dummyhandlerBSV, expressionStatement);
		assertEquals(3, dummyhandlerBSV.getDummyList().size());

		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_Log4J");  // OrgApacheLog4j
		expressionStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 0);
		method.invoke(dummyhandlerBSV, expressionStatement);
		assertEquals(3, dummyhandlerBSV.getDummyList().size());

		// OrgApacheLog4j case
		argForSettingXML = new String[] {SmellSettings.EXTRARULE_OrgApacheLog4j};
		setNewSettingsWithExtraRules(argForSettingXML);
		//   分別測試：全部例子、符合例子、不符合例子 是否有抓出
		regetDummyhandlerBSV();
		assertEquals(1, dummyhandlerBSV.getDummyList().size());

		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_Log4J");  // OrgApacheLog4j
		expressionStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 0);
		method.invoke(dummyhandlerBSV, expressionStatement);
		assertEquals(2, dummyhandlerBSV.getDummyList().size());

		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_javaLogInfo");  // JavaUtilLoggingLogger
		expressionStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 0);
		method.invoke(dummyhandlerBSV, expressionStatement);
		assertEquals(2, dummyhandlerBSV.getDummyList().size());
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
		
		initializeMethodCollectList();
		String testClassPattern = UserDefineDummyHandlerFish.class.getName() + ".*";
		
		// 確認初始值
		setEmptySetting();
		regetDummyhandlerBSV();
		assertEquals(0, dummyhandlerBSV.getDummyList().size());

		// 確認全部的Example中恰有偵測到兩次呼叫
		smellSettings.addDummyHandlerPattern(testClassPattern, true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		regetDummyhandlerBSV();
		assertEquals(2, dummyhandlerBSV.getDummyList().size());
		
		// 確認當使用者未勾選時不會偵測到
		setEmptySetting();
		smellSettings.addDummyHandlerPattern(testClassPattern, false);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		regetDummyhandlerBSV();
		assertEquals(0, dummyhandlerBSV.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForUserPatternType2() {
		initializeMethodCollectList();
		ExpressionStatement expressionStatement;
		
		// 使用者輸入為 *.toString
		//   測試全部的 Example Code
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		regetDummyhandlerBSV();
		assertEquals(2, dummyhandlerBSV.getDummyList().size());
		//   輸入一個 true case
		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_systemOutPrintlnWithoutE");
		expressionStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 1);
		dummyhandlerBSV.detectDummyHandler(expressionStatement);
		assertEquals(3, dummyhandlerBSV.getDummyList().size());
		//   輸入一個 false case - method declaration 是 *.toCharArray
		expressionStatement = getExpressionStatementFromMethodDeclaration(mDeclaration, 1, 0, 2);
		dummyhandlerBSV.detectDummyHandler(expressionStatement);
		assertEquals(3, dummyhandlerBSV.getDummyList().size());

		// 使用者輸入為 *.toString, 但未勾選時
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString", false);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		regetDummyhandlerBSV();
		assertEquals(0, dummyhandlerBSV.getDummyList().size());
		// 使用者輸入改為 *.toString(), 不會被視同是 *.toString
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString()", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		regetDummyhandlerBSV();
		assertEquals(0, dummyhandlerBSV.getDummyList().size());
	}

	/**
	 * 測試當使用者自訂的type3是否能抓到
	 * 同時測試addDummyHandlerSmellInfo()中，遇到含有"<>"library會自動轉成"<"之前的子字串
	 */
	@Test
	public void testAddDummyHandlerSmellInfoForUserPatternType3WithBracket() {
		initializeMethodCollectList();
		String testClassPattern = "java.util.ArrayList.add";
		setEmptySetting();
		smellSettings.addDummyHandlerPattern(testClassPattern, true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		regetDummyhandlerBSV();
		assertEquals(1, dummyhandlerBSV.getDummyList().size());
	}

	@Test
	public void testIsThrowStatementInCatchClause() {
		initializeMethodCollectList();
		TryStatement tryStatement;
		CatchClause catchStatement;

		// 測試 符合的例子 是否會抓出
		mDeclaration = getMethodDeclarationByName(methodCollectList, "false_throwAndPrint");
		tryStatement = (TryStatement) mDeclaration.getBody().statements().get(0);
		catchStatement = (CatchClause) tryStatement.catchClauses().get(0);
		assertTrue(dummyhandlerBSV.isThrowStatementInCatchClause(catchStatement));
		
		// 測試 不符合例子 是否會抓出
		mDeclaration = getMethodDeclarationByName(methodCollectList, "true_DummyHandlerTryNestedTry");
		tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		catchStatement = (CatchClause) tryStatement.catchClauses().get(0);
		assertFalse(dummyhandlerBSV.isThrowStatementInCatchClause(catchStatement));
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
	private void regetDummyhandlerBSV() {
		dummyhandlerBSV = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyhandlerBSV);
	}
	
	private MethodDeclaration getMethodDeclarationByName(
			List<ASTNode> methodDeclarationList, String methodName) {
		for (int i = 0; i < methodDeclarationList.size(); i++) {
			mDeclaration = (MethodDeclaration) methodDeclarationList.get(i);
			if (methodName.equals(mDeclaration.getName().toString())) {
				return mDeclaration;
			}
		}
		return null;
	}
	
	private ExpressionStatement getExpressionStatementFromMethodDeclaration(
			MethodDeclaration mDeclaration, int statementsNumberOnMethodDeclaration,
			int catchClauseNumber, int statementsNumberOnCatchClause) {
		TryStatement tryStatement = (TryStatement) mDeclaration.getBody().statements().get(statementsNumberOnMethodDeclaration);
		CatchClause catchClause = (CatchClause) tryStatement.catchClauses().get(catchClauseNumber);
		return (ExpressionStatement) catchClause.getBody().statements().get(statementsNumberOnCatchClause);
	}

	// 產生一個收集所有Method的Collector串成List
	private void initializeMethodCollectList() {
		methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		methodCollectList = methodCollector.getMethodList();
	}
}
