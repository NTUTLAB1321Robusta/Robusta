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
		// Ū�������ɮ׼˥����e
		javaFileToString = new JavaFileToString();
		javaFileToString.read(SuppressWarningExampleForAnalyzer.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker = new JavaProjectMaker("ExceptionAnalyerTest");
		javaProjectMaker.setJREDefaultContainer();
		
		// �s�W�����J�� library
		javaProjectMaker.packAgileExceptionClasses2JarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		
		// �ھڴ����ɮ׼˥����e�إ߷s���ɮ�
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
		
		// �إ� XML
		CreateSettings();
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(SuppressWarningExampleForAnalyzer.class, javaProjectMaker.getProjectName()));
		
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
	
	@Test
	public void testExceptionAnalyzerWithIntArgument() {
		ASTMethodCollector collector = new ASTMethodCollector();
		compilationUnit.accept(collector);
		
		// �x�s�M��
		List<MethodDeclaration> methodList = collector.getMethodList();
		List<MarkerInfo> totalNTList = new ArrayList<MarkerInfo>();
		List<RLMessage> totalMethodRLList = new ArrayList<RLMessage>();
		List<SSMessage> totalSSList = new ArrayList<SSMessage>();
		
		for (int i = 0; i < methodList.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit,  methodList.get(i).getStartPosition(), 0);
			compilationUnit.accept(exceptionAnalyzer);
			totalNTList.addAll(exceptionAnalyzer.getNestedTryList());
			totalMethodRLList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
			totalSSList.addAll(exceptionAnalyzer.getSuppressSemllAnnotationList());
		}
		
		assertEquals(8, totalNTList.size());
		for (int i = 0; i < totalNTList.size(); i++) {
			assertTrue(totalNTList.get(i).getCodeSmellType().toString().equals("Nested_Try_Block"));
		}
		assertEquals(78, totalNTList.get(0).getLineNumber());
		assertEquals(98, totalNTList.get(1).getLineNumber());
		assertEquals(254, totalNTList.get(2).getLineNumber());
		assertEquals(258, totalNTList.get(3).getLineNumber());
		assertEquals(308, totalNTList.get(4).getLineNumber());
		assertEquals(324, totalNTList.get(5).getLineNumber());
		assertEquals(443, totalNTList.get(6).getLineNumber());
		assertEquals(447, totalNTList.get(7).getLineNumber());
		
		assertEquals(6, totalMethodRLList.size());

		assertEquals(totalMethodRLList.get(0).getRLData().getExceptionType().toString(),"java.lang.RuntimeException");
		assertEquals(totalMethodRLList.get(1).getRLData().getExceptionType().toString(),"java.lang.RuntimeException");
		assertEquals(totalMethodRLList.get(2).getRLData().getExceptionType().toString(),"java.lang.RuntimeException");
		assertEquals(totalMethodRLList.get(3).getRLData().getExceptionType().toString(),"java.io.IOException");
		assertEquals(totalMethodRLList.get(4).getRLData().getExceptionType().toString(),"java.io.IOException");
		assertEquals(totalMethodRLList.get(5).getRLData().getExceptionType().toString(),"java.io.IOException");
		
		assertEquals(133,totalMethodRLList.get(0).getLineNumber());
		assertEquals(152,totalMethodRLList.get(1).getLineNumber());
		assertEquals(172,totalMethodRLList.get(2).getLineNumber());
		assertEquals(204,totalMethodRLList.get(3).getLineNumber());
		assertEquals(218,totalMethodRLList.get(4).getLineNumber());
		assertEquals(231,totalMethodRLList.get(5).getLineNumber());

		assertEquals("suppress warning ����T�b nested ���U���|�Q�O����C���ӬO26�ӡA���O�ثe�\��u���ˬd��19��",19, totalSSList.size());
	}

	@Ignore
	public void testExceptionAnalyzerWithBooleanArgument() {
		fail("�ĤG��overloading��ExceptionAnalyzer���Q����");
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
	
	@Test
	public void testProcessTryStatement() throws Exception {
		Method methodProcessTryStatement = ExceptionAnalyzer.class.getDeclaredMethod("processTryStatement", ASTNode.class);
		methodProcessTryStatement.setAccessible(true);
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		
		List<MethodDeclaration> methodList = astMethodCollector.getMethodList();
		List<MarkerInfo> totalNTList = new ArrayList<MarkerInfo>();
		List<RLMessage> totalMethodRLList = new ArrayList<RLMessage>();
		List<SSMessage> totalSSList = new ArrayList<SSMessage>();
		
		for (int i = 0; i < methodList.size(); i++) {
			MethodDeclaration md = methodList.get(i);
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
		int[] lineNumber = { 78, 98, 254, 258, 308, 324, 443, 447 };
		
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
		assertEquals(8, totalNTList.size());
		
		/*
		 * �b�_�� try-catch �n�b catch �W suppress bad smell ��
		 * ���[�b method �W suppress bad smell �ɥi�H���T���Q suppress
		 */
		assertEquals("suppress warning ����T�b nested ���U���|�Q�O����C�w�p�n���15�ӡA���O�ثe�\��u����9��", 9, totalSSList.size());
	}
	
	@Test
	public void testFindExceptionTypes() throws Exception {
		Method methodFindExceptionTypes = ExceptionAnalyzer.class.getDeclaredMethod("findExceptionTypes", ASTNode.class, ITypeBinding[].class);
		methodFindExceptionTypes.setAccessible(true);
		
		// ��Ʋ���
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<MethodDeclaration> methodlist = astMethodCollector.getMethodList();
		List<RLMessage> totalRLList = new ArrayList<RLMessage>();
		MethodDeclaration mDeclaration = methodlist.get(7);
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
		mDeclaration = methodlist.get(6);
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
		// ��� 6 �� Tag ���O�� method overloading for addRL(RLMessage rlmsg, int currentCatch)
		assertEquals(6, totalRLList.size());
		for (int i = 0; i < totalRLList.size(); i++) {
			methodAddRLForInt.invoke(exceptionAnalyzer, totalRLList.get(i), i);
		}
		// �N 6 �� Tag ���O�� method �Q�� addRL �o�� method �O�_���\�[�J 
		assertEquals(6, exceptionAnalyzer.getExceptionList().size());

		totalRLList =  new ArrayList<RLMessage>();
		
		assertEquals(0, totalRLList.size());
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodAnnotation.invoke(exceptionAnalyzer, methodlist.get(i));
			totalRLList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
		}
		// ��� 6 �� Tag ���O�� method overloading for addRL(RLMessage rlmsg, String key) 
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

	@Test
	public void testGetMethodAnnotationForSuppressSemllAnnotation() throws Exception {
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<MethodDeclaration> methodlist = astMethodCollector.getMethodList();
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
		
		assertEquals(34, totalList.get(0).getLineNumber());
		assertEquals(43, totalList.get(1).getLineNumber());
		assertEquals(70, totalList.get(2).getLineNumber());
		assertEquals(87, totalList.get(3).getLineNumber());
		assertEquals(106, totalList.get(4).getLineNumber());
		assertEquals(133, totalList.get(5).getLineNumber());
		assertEquals(152, totalList.get(6).getLineNumber());
		assertEquals(172, totalList.get(7).getLineNumber());
		
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
