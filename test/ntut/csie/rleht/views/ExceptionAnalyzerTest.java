package ntut.csie.rleht.views;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.SuppressWarningExampleForAnalyzer;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.util.PathUtils;

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
		javaFileToString = new JavaFileToString();
		javaFileToString.read(SuppressWarningExampleForAnalyzer.class, JavaProjectMaker.FOLDERNAME_TEST);
		javaProjectMaker = new JavaProjectMaker("ExceptionAnalyerTest");
		javaProjectMaker.setJREDefaultContainer();
		
		javaProjectMaker.packageAgileExceptionClassesToJarIntoLibFolder(
				JavaProjectMaker.FOLDERNAME_LIB_JAR,
				JavaProjectMaker.FOLDERNAME_BIN_CLASS);
		javaProjectMaker.addJarFromTestProjectToBuildPath("/"
				+ JavaProjectMaker.RL_LIBRARY_PATH);
		
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
		
		CreateSettings();
		Path path = new Path(PathUtils.getPathOfClassUnderSrcFolder(SuppressWarningExampleForAnalyzer.class, javaProjectMaker.getProjectName()));
		
		// Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		
		parser.setSource(JavaCore.createCompilationUnitFrom(ResourcesPlugin.getWorkspace().getRoot().getFile(path)));
		parser.setResolveBindings(true);
		
		compilationUnit = (CompilationUnit) parser.createAST(null); 
		compilationUnit.recordModifications();
	}
	
	@After
	public void tearDown() throws Exception {
		javaProjectMaker.deleteProject();
	}
	

	@Ignore
	public void testExceptionAnalyzerWithBooleanArgument() {
		fail("analyzer with boolean argument has not been tested");
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
	public void testFindExceptionTypes() throws Exception {
		Method methodFindExceptionTypes = ExceptionAnalyzer.class.getDeclaredMethod("findExceptionTypes", ASTNode.class, ITypeBinding[].class);
		methodFindExceptionTypes.setAccessible(true);
		
		// generate data
		ASTMethodCollector astMethodCollector = new ASTMethodCollector();
		compilationUnit.accept(astMethodCollector);
		List<MethodDeclaration> methodlist = astMethodCollector.getMethodList();
		List<RLMessage> totalRLList = new ArrayList<RLMessage>();
		MethodDeclaration mDeclaration = methodlist.get(7);
		TryStatement tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		ExpressionStatement statement = (ExpressionStatement) tryStatement.getBody().statements().get(0);
		ClassInstanceCreation cic = (ClassInstanceCreation)statement.getExpression();
		
		// Class Instance Creation
		// initial state
		exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(7).getStartPosition(), 0);
		totalRLList.addAll(exceptionAnalyzer.getExceptionList());
		assertEquals(0, totalRLList.size());
		methodFindExceptionTypes.invoke(exceptionAnalyzer, (ASTNode) cic, cic.resolveConstructorBinding().getExceptionTypes());
		totalRLList.addAll(exceptionAnalyzer.getExceptionList());
		assertEquals(1, totalRLList.size());
		
		// clear data
		exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(6).getStartPosition(), 0);
		totalRLList = new ArrayList<RLMessage>();
		assertEquals(0, totalRLList.size());
		mDeclaration = methodlist.get(6);
		tryStatement = (TryStatement) mDeclaration.getBody().statements().get(1);
		statement = (ExpressionStatement) tryStatement.getBody().statements().get(0);
		Assignment assignment = (Assignment) statement.getExpression();
		cic = (ClassInstanceCreation) assignment.getRightHandSide();
		methodFindExceptionTypes.invoke(exceptionAnalyzer, (ASTNode) cic, cic.resolveConstructorBinding().getExceptionTypes());
		
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
		assertEquals(6, totalRLList.size());
		for (int i = 0; i < totalRLList.size(); i++) {
			methodAddRLForInt.invoke(exceptionAnalyzer, totalRLList.get(i), i);
		}
		assertEquals(6, exceptionAnalyzer.getExceptionList().size());

		totalRLList =  new ArrayList<RLMessage>();
		
		assertEquals(0, totalRLList.size());
		for (int i = 0; i < methodlist.size(); i++) {
			exceptionAnalyzer = new ExceptionAnalyzer(compilationUnit, methodlist.get(i).getStartPosition(), 0);
			methodGetMethodAnnotation.invoke(exceptionAnalyzer, methodlist.get(i));
			totalRLList.addAll(exceptionAnalyzer.getMethodRLAnnotationList());
		}
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
		
		assertEquals(115, totalList.get(0).getLineNumber());
		assertEquals(131, totalList.get(1).getLineNumber());
		assertEquals(146, totalList.get(2).getLineNumber());
		
		assertEquals(6, totalList.size());
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
