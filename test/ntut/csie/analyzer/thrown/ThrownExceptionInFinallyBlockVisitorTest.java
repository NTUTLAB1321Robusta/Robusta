package ntut.csie.analyzer.thrown;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import ntut.csie.analyzer.ASTInitializerCollector;
import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.testutility.Assertor;
import ntut.csie.util.PathUtils;
import ntut.csie.testutility.TestEnvironmentBuilder;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ThrownExceptionInFinallyBlockVisitorTest {
	CompilationUnit compilationUnit;
	ExceptionThrownFromFinallyBlockVisitor thrownExceptionInFinallyBlockVisitor;
	SmellSettings smellSettings;
	List<MethodDeclaration> methodList;
	private TestEnvironmentBuilder environmentBuilder;
	
	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("ThrownExceptionInFinallyBlockTest");
		environmentBuilder.createEnvironment();
		
		environmentBuilder.loadClass(ExceptionThrownFromFinallyBlockExample.class);
		
		compilationUnit = environmentBuilder.getCompilationUnit(ExceptionThrownFromFinallyBlockExample.class);
		smellSettings = environmentBuilder.getSmellSettings();
		
		
		compilationUnit.recordModifications();

		thrownExceptionInFinallyBlockVisitor = new ExceptionThrownFromFinallyBlockVisitor(
				compilationUnit);

		// get the method list of the compilationUnit
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		methodList = methodCollector.getMethodList();
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();		
	}

	/**
	 * This test case will be fault if any of the rest test cases are fault
	 */
	@Test
	public void visitWithWholeCompilationUnit() {
		compilationUnit.accept(thrownExceptionInFinallyBlockVisitor);
		Assertor.assertMarkerInfoListSize(32,
				thrownExceptionInFinallyBlockVisitor.getThrownInFinallyList());
	}

	@Test
	public void visitWithSuperMethodInvocation() throws Exception {
		//Find the MethodDeclarations' which are named "superMethodInvocation"
		//It's the usage to find the corresponding test functions
		MethodDeclaration method = getMethodDeclarationByName("superMethodInvocation");
		method.accept(thrownExceptionInFinallyBlockVisitor);
		List<MarkerInfo> thrownInFinallyInfos = thrownExceptionInFinallyBlockVisitor
			.getThrownInFinallyList();
		
		assertEquals(1, thrownInFinallyInfos.size());
		assertEquals(360, thrownInFinallyInfos.get(0).getLineNumber());
	}

	@Test
	public void visitWithComplexExampleWithTEIFB() throws Exception {
		MethodDeclaration method = getMethodDeclarationByName("complexExampleWithTEIFB");
		method.accept(thrownExceptionInFinallyBlockVisitor);
		List<MarkerInfo> thrownInFinallyInfos = thrownExceptionInFinallyBlockVisitor
			.getThrownInFinallyList();
		//Choose five of the nine bad smells to check whether the chosen bad smells are marked
		assertEquals(9, thrownInFinallyInfos.size());
		assertEquals(183, thrownInFinallyInfos.get(0).getLineNumber());
		assertEquals(189, thrownInFinallyInfos.get(2).getLineNumber());
		assertEquals(196, thrownInFinallyInfos.get(4).getLineNumber());
		assertEquals(217, thrownInFinallyInfos.get(6).getLineNumber());
		assertEquals(230, thrownInFinallyInfos.get(8).getLineNumber());
	}

	@Test
	public void visitWithComplexExampleWithoutTEIFB() throws Exception {
		MethodDeclaration method = getMethodDeclarationByName("complexExampleWithoutTEIFB");
		method.accept(thrownExceptionInFinallyBlockVisitor);

		assertEquals(0, thrownExceptionInFinallyBlockVisitor
				.getThrownInFinallyList().size());
	}

	@Test
	public void visitWithThrownStatementInTryBlockInFinally() throws Exception {
		MethodDeclaration method = getMethodDeclarationByName("thrownStatementInTryBlockInFinally");
		method.accept(thrownExceptionInFinallyBlockVisitor);
		List<MarkerInfo> thrownInFinallyInfos = thrownExceptionInFinallyBlockVisitor
			.getThrownInFinallyList();
		
		assertEquals(1, thrownInFinallyInfos.size());
		assertEquals(159, thrownInFinallyInfos.get(0).getLineNumber());
	}

	@Test
	public void visitWithThrownStatementInCatchOrFinallyBlockInFinally()
			throws Exception {
		MethodDeclaration method = getMethodDeclarationByName("thrownStatementInCatchOrFinallyBlockInFinally");
		method.accept(thrownExceptionInFinallyBlockVisitor);
		List<MarkerInfo> thrownInFinallyInfos = thrownExceptionInFinallyBlockVisitor
			.getThrownInFinallyList();
		
		assertEquals(2, thrownInFinallyInfos.size());
		assertEquals(130, thrownInFinallyInfos.get(0).getLineNumber());
		assertEquals(132, thrownInFinallyInfos.get(1).getLineNumber());
	}

	@Test
	public void visitWithNewExceptionWithoutKeywordThrow() throws Exception {
		MethodDeclaration method = getMethodDeclarationByName("newExceptionWithoutKeywordThrow");
		method.accept(thrownExceptionInFinallyBlockVisitor);

		assertEquals(0, thrownExceptionInFinallyBlockVisitor
				.getThrownInFinallyList().size());
	}

	@Test
	public void visitWithAntGTExample2() throws Exception {
		MethodDeclaration method = getMethodDeclarationByName("antGTExample2");
		method.accept(thrownExceptionInFinallyBlockVisitor);
		List<MarkerInfo> thrownInFinallyInfos = thrownExceptionInFinallyBlockVisitor
			.getThrownInFinallyList();
		
		assertEquals(4, thrownInFinallyInfos.size());
		assertEquals(379, thrownInFinallyInfos.get(0).getLineNumber());
		assertEquals(397, thrownInFinallyInfos.get(1).getLineNumber());
		assertEquals(399, thrownInFinallyInfos.get(2).getLineNumber());
		assertEquals(402, thrownInFinallyInfos.get(3).getLineNumber());
	}

	@Test
	public void visitWithThrowInFinallyInEachBlockOfTryStatement()
			throws Exception {
		MethodDeclaration method = getMethodDeclarationByName("throwInFinallyInEachBlockOfTryStatement");
		method.accept(thrownExceptionInFinallyBlockVisitor);
		List<MarkerInfo> thrownInFinallyInfos = thrownExceptionInFinallyBlockVisitor
			.getThrownInFinallyList();
		
		assertEquals(3, thrownInFinallyInfos.size());
		assertEquals(418, thrownInFinallyInfos.get(0).getLineNumber());
		assertEquals(426, thrownInFinallyInfos.get(1).getLineNumber());
		assertEquals(434, thrownInFinallyInfos.get(2).getLineNumber());
	}

	@Test
	public void visitWithThrowInEachBlockOfTryStatementInFinally()
			throws Exception {
		MethodDeclaration method = getMethodDeclarationByName("throwInEachBlockOfTryStatementInFinally");
		method.accept(thrownExceptionInFinallyBlockVisitor);
		List<MarkerInfo> thrownInFinallyInfos = thrownExceptionInFinallyBlockVisitor
			.getThrownInFinallyList();
		
		assertEquals(3, thrownInFinallyInfos.size());
		assertEquals(458, thrownInFinallyInfos.get(0).getLineNumber());
		assertEquals(470, thrownInFinallyInfos.get(1).getLineNumber());
		assertEquals(478, thrownInFinallyInfos.get(2).getLineNumber());
	}

	@Test
	public void visitWithThrownInConstructor() {
		MethodDeclaration method = getMethodDeclarationByName("ExceptionThrownFromFinallyBlockExample");
		method.accept(thrownExceptionInFinallyBlockVisitor);
		List<MarkerInfo> thrownInFinallyInfos = thrownExceptionInFinallyBlockVisitor
			.getThrownInFinallyList();
		
		assertEquals(1, thrownInFinallyInfos.size());
		assertEquals(492, thrownInFinallyInfos.get(0).getLineNumber());
	}

	@Test
	public void visitWithThrownInInitializer() {
		// Detect all initializers
		ASTInitializerCollector initializerCollector = new ASTInitializerCollector();
		compilationUnit.accept(initializerCollector);
		List<Initializer> initializers = initializerCollector.getInitializerList();
	
		for (Initializer eachInitializer : initializers) {
			eachInitializer.accept(thrownExceptionInFinallyBlockVisitor);
		}

		List<MarkerInfo> thrownInFinallyInfos = thrownExceptionInFinallyBlockVisitor
				.getThrownInFinallyList();
		assertEquals(1, thrownInFinallyInfos.size());
		assertEquals(503, thrownInFinallyInfos.get(0).getLineNumber());
	}
	
	private MethodDeclaration getMethodDeclarationByName(String name) {
		return ASTNodeFinder.getMethodDeclarationNodeByName(compilationUnit,
				name);
	}
}