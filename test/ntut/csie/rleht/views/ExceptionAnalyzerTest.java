package ntut.csie.rleht.views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.SuppressWarningExampleForAnalyzer;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ExceptionAnalyzerTest {
	JavaFileToString javaFileToString;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	SmellSettings smellSettings;
	ExceptionAnalyzer exceptionAnalyzer;
	
	@Before
	public void setUp() throws Exception {
		// 讀取測試檔案樣本內容
		javaFileToString = new JavaFileToString();
		javaFileToString.read(SuppressWarningExampleForAnalyzer.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker = new JavaProjectMaker("ExceptionAnalyerTest");
		javaProjectMaker.setJREDefaultContainer();
		
		// 新增欲載入的 library
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		
		// 根據測試檔案樣本內容建立新的檔案
		javaProjectMaker.createJavaFile(
				SuppressWarningExampleForAnalyzer.class.getPackage().getName(),
				SuppressWarningExampleForAnalyzer.class.getSimpleName()
				+ JavaProjectMaker.JAVA_FILE_EXTENSION, "package "
				+ SuppressWarningExampleForAnalyzer.class.getPackage().getName()
				+ ";\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		javaFileToString.read(UnprotectedMainProgramWithoutTryExample.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutTryExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutTryExample.class.getSimpleName() + JavaProjectMaker.JAVA_FILE_EXTENSION
				, "package " + UnprotectedMainProgramWithoutTryExample.class.getPackage().getName() + ";\n"
				+ javaFileToString.getFileContent());
		
		// 建立 XML
		CreateSettings();
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(SuppressWarningExampleForAnalyzer.class, javaProjectMaker.getProjectName()));
		
		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		// 設定要被建立 AST 的檔案
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		
		// 取得 AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}
	
	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// 如果 XML 檔案存在，則刪除之
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// 刪除專案
		javaProjectMaker.deleteProject();
	}
	
//	@Test
//	public void testExceptionAnalyzerWithIntArgument() {
//		ASTMethodCollector collector = new ASTMethodCollector();
//		compilationUnit.accept(collector);
//		
//		// 儲存專區
//		List<MethodDeclaration> methodList = collector.getMethodList();
//		List<MarkerInfo> totalNTList = new ArrayList<MarkerInfo>();
//		List<RLMessage> totalMethodRLList = new ArrayList<RLMessage>();
//		List<SSMessage> totalSSList = new ArrayList<SSMessage>();
//		
//		for (int i = 0; i < methodList.size(); i++) {
//			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit,  methodList.get(i).getStartPosition(), 0);
//			compilationUnit.accept(exceptionAnalyzer);
//			totalNTList.addAll(exceptionAnalyzer.getNestedTryList());
//			totalMethodRLList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
//			totalSSList.addAll(exceptionAnalyzer.getSuppressSemllAnnotationList());
//		}
//		
//		assertEquals(8, totalNTList.size());
//		for (int i = 0; i < totalNTList.size(); i++) {
//			assertTrue(totalNTList.get(i).getCodeSmellType().toString().equals("Nested_Try_Block"));
//		}
//		assertEquals(78, totalNTList.get(0).getLineNumber());
//		assertEquals(98, totalNTList.get(1).getLineNumber());
//		assertEquals(254, totalNTList.get(2).getLineNumber());
//		assertEquals(258, totalNTList.get(3).getLineNumber());
//		assertEquals(308, totalNTList.get(4).getLineNumber());
//		assertEquals(324, totalNTList.get(5).getLineNumber());
//		assertEquals(443, totalNTList.get(6).getLineNumber());
//		assertEquals(447, totalNTList.get(7).getLineNumber());
//		
//		assertEquals(6, totalMethodRLList.size());
//
//		assertEquals(totalMethodRLList.get(0).getRLData().getExceptionType().toString(),"java.lang.RuntimeException");
//		assertEquals(totalMethodRLList.get(1).getRLData().getExceptionType().toString(),"java.lang.RuntimeException");
//		assertEquals(totalMethodRLList.get(2).getRLData().getExceptionType().toString(),"java.lang.RuntimeException");
//		assertEquals(totalMethodRLList.get(3).getRLData().getExceptionType().toString(),"java.io.IOException");
//		assertEquals(totalMethodRLList.get(4).getRLData().getExceptionType().toString(),"java.io.IOException");
//		assertEquals(totalMethodRLList.get(5).getRLData().getExceptionType().toString(),"java.io.IOException");
//		
//		assertEquals(133,totalMethodRLList.get(0).getLineNumber());
//		assertEquals(152,totalMethodRLList.get(1).getLineNumber());
//		assertEquals(172,totalMethodRLList.get(2).getLineNumber());
//		assertEquals(204,totalMethodRLList.get(3).getLineNumber());
//		assertEquals(218,totalMethodRLList.get(4).getLineNumber());
//		assertEquals(231,totalMethodRLList.get(5).getLineNumber());
//
//		assertEquals("suppress warning 的資訊在 nested 底下不會被記錄到。應該是26個，但是目前功能只能檢查到19個",19, totalSSList.size());
//	}

	@Ignore
	public void testExceptionAnalyzerWithBooleanArgument() {
		fail("第二種overloading的ExceptionAnalyzer未被測試");
	}
	
	@Test
	public void testVisitNode() throws Exception {
		ASTMethodCollector collector = new ASTMethodCollector();
		compilationUnit.accept(collector);
		
		List<MethodDeclaration> methodList = collector.getMethodList();
		
		Method visitNodemMethod = ExceptionAnalyzer.class.getDeclaredMethod("visitNode", ASTNode.class);
		visitNodemMethod.setAccessible(true);

		// #1 compilationUnit
		exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, compilationUnit.getStartPosition(), 0);
		assertTrue((Boolean)visitNodemMethod.invoke(exceptionAnalyzer, compilationUnit));
		
		// #2 METHOD_DECLARATION
		for (int i = 0; i < methodList.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodList.get(i).getStartPosition(), 0);
			assertTrue((Boolean)visitNodemMethod.invoke(exceptionAnalyzer, methodList.get(i)));
		}
		
		// #3 TRY_STATEMENT
		for (int i = 0; i < methodList.size(); i++) {
			MethodDeclaration md = (MethodDeclaration) methodList.get(i);
			for (int j = 0; j < md.getBody().statements().size(); j++) {
				ASTNode node = (ASTNode)md.getBody().statements().get(j);
				if (node.getNodeType() == ASTNode.TRY_STATEMENT) {
					exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodList.get(i).getStartPosition(), 0);
					assertTrue((Boolean)visitNodemMethod.invoke(exceptionAnalyzer, node));
				}
			}
		}
		// #4 THROW_STATEMENT
		for (int i = 0; i < methodList.size(); i++) {
			MethodDeclaration md = methodList.get(i);
			for (int j = 0; j < md.getBody().statements().size(); j++) {
				ASTNode node = (ASTNode)md.getBody().statements().get(j);
				if (node.getNodeType() == ASTNode.TRY_STATEMENT) {
					TryStatement tStatement = (TryStatement)node;
					for (int k = 0; k < tStatement.catchClauses().size(); k++) {
						CatchClause clause = (CatchClause)tStatement.catchClauses().get(k);
						for (int k2 = 0; k2 < clause.getBody().statements().size(); k2++) {
							if (((ASTNode)clause.getBody().statements().get(k2)).getNodeType()== ASTNode.THROW_STATEMENT) {
								ThrowStatement throwStatement = (ThrowStatement) clause.getBody().statements().get(k2);
								exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, throwStatement.getStartPosition(), 0);
								assertTrue((Boolean)visitNodemMethod.invoke(exceptionAnalyzer, throwStatement));
							}
						}
					}
				}
			}
		}
	}
	
//	@Test
//	public void testProcessTryStatement() throws Exception {
//		Method methodProcessTryStatement = ExceptionAnalyzer.class.getDeclaredMethod("processTryStatement", ASTNode.class);
//		methodProcessTryStatement.setAccessible(true);
//		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
//		compilationUnit.accept(astMethodCollector);
//		
//		List<MethodDeclaration> methodList = astMethodCollector.getMethodList();
//		List<MarkerInfo> totalNTList = new ArrayList<MarkerInfo>();
//		List<RLMessage> totalMethodRLList = new ArrayList<RLMessage>();
//		List<SSMessage> totalSSList = new ArrayList<SSMessage>();
//		
//		for (int i = 0; i < methodList.size(); i++) {
//			MethodDeclaration md = methodList.get(i);
//			for (int j = 0; j < md.getBody().statements().size(); j++) {
//				ASTNode node = (ASTNode)md.getBody().statements().get(j);
//				if (node.getNodeType() == ASTNode.TRY_STATEMENT) {
//					exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodList.get(i).getStartPosition(), 0);
//					methodProcessTryStatement.invoke(exceptionAnalyzer, node);
//					totalNTList.addAll(exceptionAnalyzer.getNestedTryList());
//					totalMethodRLList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
//					totalSSList.addAll(exceptionAnalyzer.getSuppressSemllAnnotationList());
//				}
//			}
//		}
//		
//		/*
//		 * 發生 nested try 所在的 line number
//		 */
//		int[] lineNumber = { 78, 98, 254, 258, 308, 324, 443, 447 };
//		
//		for (int i = 0; i < totalNTList.size(); i++) {
//			assertEquals(lineNumber[i], totalNTList.get(i).getLineNumber());
//		}
//		
//		/*
//		 * 不再 method 上
//		 */
//		assertEquals(0, totalMethodRLList.size());
//		
//		/*
//		 * nested try
//		 */
//		assertEquals(8, totalNTList.size());
//		
//		/*
//		 * 在巢狀 try-catch 要在 catch 上 suppress bad smell 時
//		 * 反觀在 method 上 suppress bad smell 時可以正確的被 suppress
//		 */
//		assertEquals("suppress warning 的資訊在 nested 底下不會被記錄到。預計要抓到15個，但是目前功能只能抓到9個", 9, totalSSList.size());
//	}
	
	@Test
	public void testFindExceptionTypes() throws Exception {
		Method methodFindExceptionTypes = ExceptionAnalyzer.class.getDeclaredMethod("findExceptionTypes", ASTNode.class, ITypeBinding[].class);
		methodFindExceptionTypes.setAccessible(true);
		
		// 資料產生
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<MethodDeclaration> methodlist = astMethodCollector.getMethodList();
		List<RLMessage> totalRLList = new ArrayList<RLMessage>();
		MethodDeclaration mDeclaration = methodlist.get(7);
		TryStatement tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		ExpressionStatement statement = (ExpressionStatement) tryStatement.getBody().statements().get(0);
		ClassInstanceCreation cic = (ClassInstanceCreation)statement.getExpression();
		
		// Class Instance Creation
		// 初始狀態
		exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(7).getStartPosition(), 0);
		totalRLList.addAll(exceptionAnalyzer.getExceptionList());
		assertEquals(0, totalRLList.size());
		methodFindExceptionTypes.invoke(exceptionAnalyzer, (ASTNode) cic, cic.resolveConstructorBinding().getExceptionTypes());
		totalRLList.addAll(exceptionAnalyzer.getExceptionList());
		assertEquals(1, totalRLList.size());
		
		// 清除資料
		exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(6).getStartPosition(), 0);
		totalRLList = new ArrayList<RLMessage>();
		assertEquals(0, totalRLList.size());
		mDeclaration = methodlist.get(6);
		tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		statement = (ExpressionStatement) tryStatement.getBody().statements().get(0);
		Assignment assignment = (Assignment) statement.getExpression();
		cic = (ClassInstanceCreation) assignment.getRightHandSide();
		methodFindExceptionTypes.invoke(exceptionAnalyzer, (ASTNode) cic, cic.resolveConstructorBinding().getExceptionTypes());
		
		// 疊加測試
		totalRLList.addAll(exceptionAnalyzer.getExceptionList());
		totalRLList.addAll(exceptionAnalyzer.getExceptionList());
		assertEquals(2, totalRLList.size());
	}
	
	@Test
	public void testAddRL() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		
		List<MethodDeclaration> methodlist = astMethodCollector.getMethodList();
		List<RLMessage> totalRLList = new ArrayList<RLMessage>();
		
		Method methodAddRLForInt = ExceptionAnalyzer.class.getDeclaredMethod("addRL", RLMessage.class, int.class);
		methodAddRLForInt.setAccessible(true);
		
		Method methodAddRLForString = ExceptionAnalyzer.class.getDeclaredMethod("addRL", RLMessage.class, String.class);
		methodAddRLForString.setAccessible(true);
		
		Method methodGetMethodAnnotation = ExceptionAnalyzer.class.getDeclaredMethod("getMethodAnnotation", ASTNode.class);
		methodGetMethodAnnotation.setAccessible(true);
		
		assertEquals(0, totalRLList.size());
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodAnnotation.invoke(exceptionAnalyzer, methodlist.get(i));
			totalRLList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
		}
		// 抓到 6 個 Tag 註記的 method overloading for addRL(RLMessage rlmsg, int currentCatch)
		assertEquals(6, totalRLList.size());
		for (int i = 0; i < totalRLList.size(); i++) {
			methodAddRLForInt.invoke(exceptionAnalyzer, totalRLList.get(i), i);
		}
		// 將 6 個 Tag 註記的 method 利用 addRL 這個 method 是否成功加入 
		assertEquals(6, exceptionAnalyzer.getExceptionList().size());

		totalRLList =  new ArrayList<RLMessage>();
		
		assertEquals(0, totalRLList.size());
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodAnnotation.invoke(exceptionAnalyzer, methodlist.get(i));
			totalRLList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
		}
		// 抓到 6 個 Tag 註記的 method overloading for addRL(RLMessage rlmsg, String key) 
		assertEquals(6, totalRLList.size());
		for (int i = 0; i < totalRLList.size(); i++) {
			methodAddRLForString.invoke(exceptionAnalyzer, totalRLList.get(i), "父母親的id哀豬叉踹." + i);
		}
		assertEquals(6, exceptionAnalyzer.getExceptionList().size());
	}

	@Test
	public void testGetMethodThrowsList() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<MethodDeclaration> methodlist = astMethodCollector.getMethodList();
		List<RLMessage> totalList = new ArrayList<RLMessage>();
		Method methodGetMethodThrowsList = ExceptionAnalyzer.class.getDeclaredMethod("getMethodThrowsList", ASTNode.class);
		methodGetMethodThrowsList.setAccessible(true);
		
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodThrowsList.invoke(exceptionAnalyzer, methodlist.get(i));
			totalList.addAll(exceptionAnalyzer.getExceptionList());
		}
		
		assertEquals("java.io.IOException",totalList.get(0).getRLData().getExceptionType().toString());
		assertEquals("java.io.IOException",totalList.get(1).getRLData().getExceptionType().toString());
		assertEquals("java.io.IOException",totalList.get(2).getRLData().getExceptionType().toString());
		assertEquals("java.io.IOException",totalList.get(3).getRLData().getExceptionType().toString());
		assertEquals("java.io.IOException",totalList.get(4).getRLData().getExceptionType().toString());
		assertEquals("java.io.IOException",totalList.get(5).getRLData().getExceptionType().toString());
		assertEquals("java.net.SocketTimeoutException",totalList.get(6).getRLData().getExceptionType().toString());
		assertEquals("java.io.InterruptedIOException",totalList.get(7).getRLData().getExceptionType().toString());
		assertEquals("java.net.SocketTimeoutException",totalList.get(8).getRLData().getExceptionType().toString());
		assertEquals("java.io.InterruptedIOException",totalList.get(9).getRLData().getExceptionType().toString());
		assertEquals("java.io.InterruptedIOException",totalList.get(10).getRLData().getExceptionType().toString());
		assertEquals("java.lang.ArithmeticException",totalList.get(11).getRLData().getExceptionType().toString());
		assertEquals("java.lang.Exception",totalList.get(12).getRLData().getExceptionType().toString());
		
		assertEquals(19, totalList.size());
	}

	@Test
	public void testGetMethodAnnotationForRLAnnotation() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<MethodDeclaration> methodlist = astMethodCollector.getMethodList();
		List<RLMessage> totalList = new ArrayList<RLMessage>();
		Method methodGetMethodAnnotation = ExceptionAnalyzer.class.getDeclaredMethod("getMethodAnnotation", ASTNode.class);
		methodGetMethodAnnotation.setAccessible(true);
		
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodAnnotation.invoke(exceptionAnalyzer, methodlist.get(i));
			totalList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
		}
		
		assertTrue(methodlist.get(7).toString().equals(totalList.get(0).getStatement().toString()));
		assertTrue(methodlist.get(8).toString().equals(totalList.get(1).getStatement().toString()));
		assertTrue(methodlist.get(9).toString().equals(totalList.get(2).getStatement().toString()));
		
		assertEquals(133, totalList.get(0).getLineNumber());
		assertEquals(152, totalList.get(1).getLineNumber());
		assertEquals(172, totalList.get(2).getLineNumber());
		
		assertEquals(6, totalList.size());
	}

//	@Test
//	public void testGetMethodAnnotationForSuppressSemllAnnotation() throws Exception {
//		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
//		compilationUnit.accept(astMethodCollector);
//		List<MethodDeclaration> methodlist = astMethodCollector.getMethodList();
//		List<SSMessage> totalList = new ArrayList<SSMessage>();
//		Method methodGetMethodAnnotation = ExceptionAnalyzer.class.getDeclaredMethod("getMethodAnnotation", ASTNode.class);
//		methodGetMethodAnnotation.setAccessible(true);
//		
//		for (int i = 0; i < methodlist.size(); i++) {
//			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
//			methodGetMethodAnnotation.invoke(exceptionAnalyzer, methodlist.get(i));
//			totalList.addAll(exceptionAnalyzer.getSuppressSemllAnnotationList());
//		}
//		
//		assertEquals("[Unprotected_Main_Program]", totalList.get(0).getSmellList().toString());
//		assertEquals("[Dummy_Handler]", totalList.get(1).getSmellList().toString());
//		assertEquals("[Nested_Try_Block, Dummy_Handler]", totalList.get(2).getSmellList().toString());
//		assertEquals("[Nested_Try_Block, Dummy_Handler]", totalList.get(3).getSmellList().toString());
//		assertEquals("[Ignore_Checked_Exception]",totalList.get(4).getSmellList().toString());
//		assertEquals("[Careless_CleanUp]", totalList.get(5).getSmellList().toString());
//		assertEquals("[Careless_CleanUp]", totalList.get(6).getSmellList().toString());
//		assertEquals("[Careless_CleanUp]", totalList.get(7).getSmellList().toString());
//		
//		assertEquals(34, totalList.get(0).getLineNumber());
//		assertEquals(43, totalList.get(1).getLineNumber());
//		assertEquals(70, totalList.get(2).getLineNumber());
//		assertEquals(87, totalList.get(3).getLineNumber());
//		assertEquals(106, totalList.get(4).getLineNumber());
//		assertEquals(133, totalList.get(5).getLineNumber());
//		assertEquals(152, totalList.get(6).getLineNumber());
//		assertEquals(172, totalList.get(7).getLineNumber());
//		
//		assertEquals(10, totalList.size());
//	}

	private void CreateSettings() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemErrPrintln);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrint);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_SystemOutPrintln);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_JavaUtilLoggingLogger);
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER, SmellSettings.EXTRARULE_OrgApacheLog4j);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
