package ntut.csie.rleht.views;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.data.SSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.exceptionBadSmells.SuppressWarningExample;
import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.rleht.builder.ASTMethodCollector;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ExceptionAnalyzerTest {
	JavaFileToString javaFileToString;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;
	SmellSettings smellSettings;
	ExceptionAnalyzer exceptionAnalyzer;
	
	@Before
	public void setUp() throws Exception {
		// Ū�������ɮ׼˥����e
		javaFileToString = new JavaFileToString();
		javaFileToString.read(SuppressWarningExample.class, "test");
		javaProjectMaker = new JavaProjectMaker("ExceptionAnalyerTest");
		javaProjectMaker.setJREDefaultContainer();
		
		// �s�W�����J�� library
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(JavaProjectMaker.LIB_JAR_FOLDERNAME, JavaProjectMaker.BIN_CLASS_FOLDERNAME);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/lib/RL.jar");
		javaProjectMaker.addJarFromProjectToBuildPath("lib\\log4j-1.2.15.jar");
		
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
		javaProjectMaker.createJavaFile("ntut.csie.filemaker.exceptionBadSmells", "SuppressWarningExample.java", "package ntut.csie.filemaker.exceptionBadSmells;\n" + javaFileToString.getFileContent());
		javaFileToString.clear();
		javaFileToString.read(UnprotectedMainProgramWithoutTryExample.class, "test");
		javaProjectMaker.createJavaFile(
				UnprotectedMainProgramWithoutTryExample.class.getPackage().getName()
				, UnprotectedMainProgramWithoutTryExample.class.getSimpleName() + ".java"
				, "package " + UnprotectedMainProgramWithoutTryExample.class.getPackage().getName() + ";\n"
				+ javaFileToString.getFileContent());
		
		// �إ� XML
		CreateSettings();
		Path path = new Path("ExceptionAnalyerTest\\src\\ntut\\csie\\filemaker\\exceptionBadSmells\\SuppressWarningExample.java");
		
		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		// �]�w�n�Q�إ� AST ���ɮ�
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		
		// ���o AST
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}
	
	@After
	public void tearDown() throws Exception {
		File xmlFile = new File(JDomUtil.getWorkspace() + File.separator + "CSPreference.xml");
		// �p�G XML �ɮצs�b�A�h�R����
		if(xmlFile.exists())
			assertTrue(xmlFile.delete());
		// �R���M��
		javaProjectMaker.deleteProject();
	}
	
//	@Test
//	public void testVisitNode() throws Exception {
//		fail("Not yet implemented");
//	}
	
	@Test
	public void testProcessTryStatement() throws Exception {
		Method methodProcessTryStatement = ExceptionAnalyzer.class.getDeclaredMethod("processTryStatement", ASTNode.class);
		methodProcessTryStatement.setAccessible(true);
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		
		List<ASTNode> methodList = astMethodCollector.getMethodList();
		List<MarkerInfo> totalNTList = new ArrayList<MarkerInfo>();
		List<RLMessage> totalMethodRLList = new ArrayList<RLMessage>();
		List<SSMessage> totalSSList = new ArrayList<SSMessage>();
		
		for (int i = 0; i < methodList.size(); i++) {
			MethodDeclaration md = (MethodDeclaration) methodList.get(i);
			for (int j = 0; j < md.getBody().statements().size(); j++) {
				ASTNode node = (ASTNode)md.getBody().statements().get(j);
				if (node.getNodeType() == ASTNode.TRY_STATEMENT) {
					exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodList.get(i).getStartPosition(), 0);
					methodProcessTryStatement.invoke(exceptionAnalyzer, node);
					totalNTList.addAll(exceptionAnalyzer.getNestedTryList());
					totalMethodRLList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
					totalSSList.addAll(exceptionAnalyzer.getSuppressSemllAnnotationList());
				}
			}
		}
		
		/*
		 * �o�� nested try �Ҧb�� line number
		 */
		int[] lineNumber = { 75, 95, 267, 271 };
		
		for (int i = 0; i < totalNTList.size(); i++) {
			assertEquals(lineNumber[i], totalNTList.get(i).getLineNumber());
		}
		
		/*
		 * ���A method �W
		 */
		assertEquals(0, totalMethodRLList.size());
		
		/*
		 * nested try
		 */
		assertEquals(4, totalNTList.size());
		
		/*
		 * �b�_�� try-catch �n�b catch �W suppress bad smell ��
		 * ���[�b method �W suppress bad smell �ɥi�H���T���Q suppress
		 */
		assertEquals("suppress warning ����T�b nested ���U���|�Q�O����", 15, totalSSList.size());
	}
	
	@Test
	public void testFindExceptionTypes() throws Exception {
		Method methodFindExceptionTypes = ExceptionAnalyzer.class.getDeclaredMethod("findExceptionTypes", ASTNode.class, ITypeBinding[].class);
		methodFindExceptionTypes.setAccessible(true);
		// ��Ʋ���
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<ASTNode> methodlist = astMethodCollector.getMethodList();
		List<RLMessage> totalRLList = new ArrayList<RLMessage>();
		MethodDeclaration mDeclaration = (MethodDeclaration)methodlist.get(7);
		TryStatement tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		ExpressionStatement statement = (ExpressionStatement) tryStatement.getBody().statements().get(0);
		ClassInstanceCreation cic = (ClassInstanceCreation)statement.getExpression();
		
		// Class Instance Creation
		// ��l���A
		exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(7).getStartPosition(), 0);
		totalRLList.addAll(exceptionAnalyzer.getExceptionList());
		assertEquals(0, totalRLList.size());
		methodFindExceptionTypes.invoke(exceptionAnalyzer, (ASTNode) cic, cic.resolveConstructorBinding().getExceptionTypes());
		totalRLList.addAll(exceptionAnalyzer.getExceptionList());
		assertEquals(1, totalRLList.size());
		// �M�����
		exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(6).getStartPosition(), 0);
		totalRLList = new ArrayList<RLMessage>();
		assertEquals(0, totalRLList.size());
		mDeclaration = (MethodDeclaration) methodlist.get(6);
		tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		statement = (ExpressionStatement) tryStatement.getBody().statements().get(0);
		Assignment assignment = (Assignment) statement.getExpression();
		cic = (ClassInstanceCreation) assignment.getRightHandSide();
		methodFindExceptionTypes.invoke(exceptionAnalyzer, (ASTNode) cic, cic.resolveConstructorBinding().getExceptionTypes());
		// �|�[����
		totalRLList.addAll(exceptionAnalyzer.getExceptionList());
		totalRLList.addAll(exceptionAnalyzer.getExceptionList());
		assertEquals(2, totalRLList.size());
	}
	
	@Test
	public void testAddRL() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		
		List<ASTNode> methodlist = astMethodCollector.getMethodList();
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
		// ��� 6 �� RL ���O�� method overloading for addRL(RLMessage rlmsg, int currentCatch)
		assertEquals(6, totalRLList.size());
		for (int i = 0; i < totalRLList.size(); i++) {
			methodAddRLForInt.invoke(exceptionAnalyzer, totalRLList.get(i), i);
		}
		// �N 6 �� RL ���O�� method �Q�� addRL �o�� method �O�_���\�[�J 
		assertEquals(6, exceptionAnalyzer.getExceptionList().size());

		totalRLList =  new ArrayList<RLMessage>();
		
		assertEquals(0, totalRLList.size());
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodAnnotation.invoke(exceptionAnalyzer, methodlist.get(i));
			totalRLList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
		}
		// ��� 6 �� RL ���O�� method overloading for addRL(RLMessage rlmsg, String key) 
		assertEquals(6, totalRLList.size());
		for (int i = 0; i < totalRLList.size(); i++) {
			methodAddRLForString.invoke(exceptionAnalyzer, totalRLList.get(i), "�����˪�id�s�ޤe��." + i);
		}
		assertEquals(6, exceptionAnalyzer.getExceptionList().size());
	}

	@Test
	public void testGetMethodThrowsList() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<ASTNode> methodlist = astMethodCollector.getMethodList();
		List<RLMessage> totalList = new ArrayList<RLMessage>();
		Method methodGetMethodThrowsList = ExceptionAnalyzer.class.getDeclaredMethod("getMethodThrowsList", ASTNode.class);
		methodGetMethodThrowsList.setAccessible(true);
		
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodThrowsList.invoke(exceptionAnalyzer, methodlist.get(i));
			totalList.addAll(exceptionAnalyzer.getExceptionList());
		}
		
		assertEquals("java.net.SocketTimeoutException",totalList.get(0).getRLData().getExceptionType().toString());
		assertEquals("java.io.InterruptedIOException",totalList.get(1).getRLData().getExceptionType().toString());
		assertEquals("java.net.SocketTimeoutException",totalList.get(2).getRLData().getExceptionType().toString());
		assertEquals("java.io.InterruptedIOException",totalList.get(3).getRLData().getExceptionType().toString());
		assertEquals("java.io.InterruptedIOException",totalList.get(4).getRLData().getExceptionType().toString());
		assertEquals("java.lang.ArithmeticException",totalList.get(5).getRLData().getExceptionType().toString());
		assertEquals("java.lang.Exception",totalList.get(6).getRLData().getExceptionType().toString());
		
		assertEquals(10, totalList.size());
	}

	@Test
	public void testGetMethodAnnotationForRLAnnotation() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<ASTNode> methodlist = astMethodCollector.getMethodList();
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
		
		assertEquals(130, totalList.get(0).getLineNumber());
		assertEquals(149, totalList.get(1).getLineNumber());
		assertEquals(169, totalList.get(2).getLineNumber());
		
		assertEquals(6, totalList.size());
	}

	@Test
	public void testGetMethodAnnotationForSuppressSemllAnnotation() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<ASTNode> methodlist = astMethodCollector.getMethodList();
		List<SSMessage> totalList = new ArrayList<SSMessage>();
		Method methodGetMethodAnnotation = ExceptionAnalyzer.class.getDeclaredMethod("getMethodAnnotation", ASTNode.class);
		methodGetMethodAnnotation.setAccessible(true);
		
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodAnnotation.invoke(exceptionAnalyzer, methodlist.get(i));
			totalList.addAll(exceptionAnalyzer.getSuppressSemllAnnotationList());
		}
		
		assertEquals("[Unprotected_Main_Program]", totalList.get(0).getSmellList().toString());
		assertEquals("[Dummy_Handler]", totalList.get(1).getSmellList().toString());
		assertEquals("[Nested_Try_Block, Dummy_Handler]", totalList.get(2).getSmellList().toString());
		assertEquals("[Nested_Try_Block, Dummy_Handler]", totalList.get(3).getSmellList().toString());
		assertEquals("[Ignore_Checked_Exception]",totalList.get(4).getSmellList().toString());
		assertEquals("[Careless_CleanUp]", totalList.get(5).getSmellList().toString());
		assertEquals("[Careless_CleanUp]", totalList.get(6).getSmellList().toString());
		assertEquals("[Careless_CleanUp]", totalList.get(7).getSmellList().toString());
		
		assertEquals(31, totalList.get(0).getLineNumber());
		assertEquals(40, totalList.get(1).getLineNumber());
		assertEquals(67, totalList.get(2).getLineNumber());
		assertEquals(84, totalList.get(3).getLineNumber());
		assertEquals(103, totalList.get(4).getLineNumber());
		assertEquals(130, totalList.get(5).getLineNumber());
		assertEquals(149, totalList.get(6).getLineNumber());
		assertEquals(169, totalList.get(7).getLineNumber());
		
		assertEquals(10, totalList.size());
	}

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
