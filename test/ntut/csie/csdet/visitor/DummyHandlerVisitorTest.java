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
	JavaFileToString javaFile2String;
	CompilationUnit compilationUnit;
	DummyHandlerVisitor dummyHandlerVisitor;
	SmellSettings smellSettings;
	ASTMethodCollector methodCollector;
	List<ASTNode> methodCollectList;
	String[] dummyHandlerPatternsInXML;

	public DummyHandlerVisitorTest() {
	}
	
	@Before
	public void setUp() throws Exception {
		String testProjectName = "DummyHandlerTest";
		// �ǳƴ����ɮ׼˥����e
		javaProjectMaker = new JavaProjectMaker(testProjectName);
		javaProjectMaker.setJREDefaultContainer();
		
		// �s�W�����J��library
		javaProjectMaker.addJarFromProjectToBuildPath(JavaProjectMaker.FOLDERNAME_LIB_JAR + "/log4j-1.2.15.jar");
		
		// �إ߷s���ɮ�DummyAndIgnoreExample
		javaFile2String = new JavaFileToString();
		javaFile2String.read(DummyAndIgnoreExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				DummyAndIgnoreExample.class.getPackage().getName(),
				DummyAndIgnoreExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + DummyAndIgnoreExample.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		// �~��إߴ��եΪ�UserDefineDummyHandlerFish
		javaFile2String.clear();
		javaFile2String.read(UserDefineDummyHandlerFish.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UserDefineDummyHandlerFish.class.getPackage().getName(),
				UserDefineDummyHandlerFish.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION,
				"package " + UserDefineDummyHandlerFish.class.getPackage().getName() + ";\n"
				+ javaFile2String.getFileContent());
		
		// �إ�XML
		dummyHandlerPatternsInXML = new String[] {
				SmellSettings.EXTRARULE_ePrintStackTrace, SmellSettings.EXTRARULE_SystemErrPrint, 
				SmellSettings.EXTRARULE_SystemErrPrintln, SmellSettings.EXTRARULE_SystemOutPrint, 
				SmellSettings.EXTRARULE_SystemOutPrintln, SmellSettings.EXTRARULE_JavaUtilLoggingLogger, 
				SmellSettings.EXTRARULE_OrgApacheLog4j};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		
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
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
	}

	@After
	public void tearDown() throws Exception {
		deleteOldSettings();
		// �R���M��
		javaProjectMaker.deleteProject();
	}
	
	@Test
	public void testVisitMemberData() {
		int dummy = 0;
		compilationUnit.accept(dummyHandlerVisitor);
		if(dummyHandlerVisitor.getDummyList() != null)
			dummy = dummyHandlerVisitor.getDummyList().size();
		
		// �����`�@���X��bad smell
		assertEquals(16, dummy);
		assertEquals(2, dummyHandlerVisitor.getFinallyCounter());
		assertEquals(25, dummyHandlerVisitor.getCatchCounter());
		assertEquals(25, dummyHandlerVisitor.getTryCounter());
	}
	
	@Test
	public void testVisitReturnValue() throws Exception {
		MethodDeclaration md = null;
		TryStatement tryStatement = null;
		
		// ����return true���D�_��case
		md = ASTNodeFinder.getMethodDeclarationNodeByName(
				compilationUnit, "true_printStackTrace_protected");	
		tryStatement = (TryStatement) md.getBody().statements().get(1);
		assertTrue(dummyHandlerVisitor.visit(tryStatement)); 
		
		// ����return false���_��final-try case
		md = ASTNodeFinder.getMethodDeclarationNodeByName(
				compilationUnit, "true_DummyHandlerFinallyNestedTry");	
		tryStatement = (TryStatement) md.getBody().statements().get(1);
		tryStatement = (TryStatement) tryStatement.getFinally().statements().get(0);
		assertEquals(false, dummyHandlerVisitor.visit(tryStatement));

		// ����return false���_��try-try case
		md = ASTNodeFinder.getMethodDeclarationNodeByName(
				compilationUnit, "true_DummyHandlerTryNestedTry");	
		tryStatement = (TryStatement) md.getBody().statements().get(1);
		tryStatement = (TryStatement) tryStatement.getBody().statements().get(2);
		assertEquals(false, dummyHandlerVisitor.visit(tryStatement));
		
		// ����return false���_��catch-try case
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
		// �T�{��l��
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		//#1 ���`��DummyHandler
		MethodInvocation eStatement = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
				"true_printStackTrace_public", "e.printStackTrace()").get(0);
		dummyHandlerVisitor.detectDummyHandler(eStatement);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
		
		//#2 ��throw
		eStatement = ASTNodeFinder
			.getMethodInvocationByMethodNameAndCode(compilationUnit,
			"false_throwAndPrint", "e.printStackTrace()").get(0);
		dummyHandlerVisitor.detectDummyHandler(eStatement);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
		
		//#3 �� Catch �~��
		eStatement = ASTNodeFinder
		.getMethodInvocationByMethodNameAndCode(compilationUnit,
		"true_printStackTrace_protected", "fis.read()").get(0);
		dummyHandlerVisitor.detectDummyHandler(eStatement);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_PrintStackTrace() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", MethodInvocation.class);
		method.setAccessible(true);
		
		// �T�{��l��
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		// ePrintStackTrace case
		dummyHandlerPatternsInXML = new String[] {SmellSettings.EXTRARULE_ePrintStackTrace};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   ���O���աG�����Ҥl�B�ŦX�Ҥl�B���ŦX�Ҥl �O�_����X
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(7, dummyHandlerVisitor.getDummyList().size());

		MethodInvocation mi = null;
		mi = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(compilationUnit, "true_printStackTrace_protected", "e.printStackTrace()").get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(8, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_SystemOutPrint() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", MethodInvocation.class);
		method.setAccessible(true);
		
		// �T�{��l��
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		dummyHandlerPatternsInXML = new String[] {
				SmellSettings.EXTRARULE_SystemErrPrint, SmellSettings.EXTRARULE_SystemErrPrintln,
				SmellSettings.EXTRARULE_SystemOutPrint, SmellSettings.EXTRARULE_SystemOutPrint};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   ���O���աG�����Ҥl�B�ŦX�Ҥl�B���ŦX�Ҥl �O�_����X
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(6, dummyHandlerVisitor.getDummyList().size());

		MethodInvocation mi = null;
		mi = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						"true_systemTrace",
						"System.out.println(\"DummyHandlerExample.true_systemErrPrint()\")")
				.get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(7, dummyHandlerVisitor.getDummyList().size());

		
		mi = ASTNodeFinder
		.getMethodInvocationByMethodNameAndCode(compilationUnit,
				"true_printStackTrace_protected",
				"e.printStackTrace()").get(0);
		method.invoke(dummyHandlerVisitor, mi);
		
		assertEquals(7, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_JavaUtilLoggingLogger() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", MethodInvocation.class);
		method.setAccessible(true);
		
		// �T�{��l��
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		// JavaUtilLoggingLogger case
		dummyHandlerPatternsInXML = new String[] {SmellSettings.EXTRARULE_JavaUtilLoggingLogger};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   ���O���աG�����Ҥl�B�ŦX�Ҥl�B���ŦX�Ҥl �O�_����X
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());

		MethodInvocation mi = null;
		mi = ASTNodeFinder
		.getMethodInvocationByMethodNameAndCode(compilationUnit,
				"true_javaLogInfo",
				"javaLog.info(\"\")").get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(3, dummyHandlerVisitor.getDummyList().size());

		mi = ASTNodeFinder
		.getMethodInvocationByMethodNameAndCode(compilationUnit,
				"true_Log4J",
				"log4j.info(\"message\")").get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(3, dummyHandlerVisitor.getDummyList().size());
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(3, dummyHandlerVisitor.getDummyList().size());

	}
	
	@Test
	public void testAddDummyHandlerSmellInfoForExtraRule_OrgApacheLog4j() throws Exception {
		Method method = DummyHandlerVisitor.class.getDeclaredMethod("addDummyHandlerSmellInfo", MethodInvocation.class);
		method.setAccessible(true);
		
		// �T�{��l��
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		// OrgApacheLog4j case
		dummyHandlerPatternsInXML = new String[] {SmellSettings.EXTRARULE_OrgApacheLog4j};
		setNewSettingsWithExtraRules(dummyHandlerPatternsInXML);
		//   ���O���աG�����Ҥl�B�ŦX�Ҥl�B���ŦX�Ҥl �O�_����X
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());

		MethodInvocation mi = null;
		mi = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(
				compilationUnit, "true_Log4J", "log4j.info(\"message\")")
				.get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());
		
		mi = ASTNodeFinder
		.getMethodInvocationByMethodNameAndCode(compilationUnit,
				"true_javaLogInfo",
				"javaLog.info(\"\")").get(0);
		method.invoke(dummyHandlerVisitor, mi);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());
	}

	/**
	 * ���շ�ϥΪ̦ۭq��type1�O�_����Outer class
	 * �����է���
	 */
	@Test
	public void testAddDummyHandlerSmellInfoForUserPatternType1() {
//		java.util.* case1 class
//		*.toString case2 statement
//		java.lang.String.toString case3 statement
//		java.util.ArrayList<java.lang.Boolean> case4 statement
		
		String testClassPattern = UserDefineDummyHandlerFish.class.getName() + ".*";
		
		// �T�{��l��
		setEmptySetting();
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());

		// �T�{������Example���꦳������⦸�I�s
		smellSettings.addDummyHandlerPattern(testClassPattern, true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());
		
		// �T�{��ϥΪ̥��Ŀ�ɤ��|������
		setEmptySetting();
		smellSettings.addDummyHandlerPattern(testClassPattern, false);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoWithUserDefinedPatternAndDetecting() {
		// �ϥΪ̦۩w�q *.toString ��Pattern�A�åB�n�D�n����
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(2, dummyHandlerVisitor.getDummyList().size());

		MethodInvocation mi = null;

		// �ŦX *.toString ���{���X -> e.toString()
		mi = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(
				compilationUnit, "true_systemOutPrintlnWithoutE",
				"e.toString()").get(0);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		dummyHandlerVisitor.detectDummyHandler(mi);
		assertEquals(1, dummyHandlerVisitor.getDummyList().size());

		// ���ŦX *.toString ���{���X -> e.toString.toCharArray()
		mi = ASTNodeFinder.getMethodInvocationByMethodNameAndCode(
				compilationUnit, "true_systemOutPrintlnWithoutE",
				"e.toString().toCharArray()").get(0);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		dummyHandlerVisitor.detectDummyHandler(mi);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
	}
	
	@Test
	public void testAddDummyHandlerSmellInfoWithUserDefinedPatternButNotDetecting() { 
		// �ϥΪ̦��۩w�q *.toString ��Pattern�A���O���n����
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString", false);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
		
		// �ϥΪ̿�J�אּ *.toString(), ���|�Q���P�O *.toString
		setEmptySetting();
		smellSettings.addDummyHandlerPattern("*.toString()", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		dummyHandlerVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(dummyHandlerVisitor);
		assertEquals(0, dummyHandlerVisitor.getDummyList().size());
	}

	/**
	 * ���շ�ϥΪ̦ۭq��type3�O�_����
	 * �P�ɴ���addDummyHandlerSmellInfo()���A�J��t��"<>"library�|�۰��ন"<"���e���l�r��
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

		// ���� �ŦX���Ҥl �O�_�|��X
		MethodDeclaration md = null;
		md = ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit, "false_throwAndPrint");
		tryStatement = (TryStatement) md.getBody().statements().get(0);
		catchStatement = (CatchClause) tryStatement.catchClauses().get(0);
		assertTrue(dummyHandlerVisitor.isThrowStatementInCatchClause(catchStatement));
		
		// ���� ���ŦX�Ҥl �O�_�|��X
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
