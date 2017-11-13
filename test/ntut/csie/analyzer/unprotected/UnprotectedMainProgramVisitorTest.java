package ntut.csie.analyzer.unprotected;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.nested.NestedTryStatementExample;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.util.PathUtils;


import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class UnprotectedMainProgramVisitorTest {
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit unit1, unit2, unit3, unit4, unit5, unit6, unit7, unit8,
			unit9;
	SmellSettings smellSettings;
	UnprotectedMainProgramVisitor mainVisitor;
	private TestEnvironmentBuilder environmentBuilder;
	@Before
	public void setUp() throws Exception {
		String testProjectName = "UnprotectedMainProgramTest";
		environmentBuilder = new TestEnvironmentBuilder("UnprotectedMainProgramTest");
		environmentBuilder.createEnvironment();
		smellSettings = environmentBuilder.getSmellSettings();

		// unit1
		Class<?> testedClass1 = UnprotectedMainProgramExample.class;
		environmentBuilder.loadClass(testedClass1);
		unit1 = environmentBuilder.getCompilationUnit(testedClass1);
		unit1.recordModifications();
		// unit2
		Class<?> testedClass2 = UnprotectedMainProgramWithCatchThrowableExample.class;
		environmentBuilder.loadClass(testedClass2);
		unit2 = environmentBuilder.getCompilationUnit(testedClass2);
		unit2.recordModifications();
		// unit3
		Class<?> testedClass3 = UnprotectedMainProgramWithoutStatementExample.class;
		environmentBuilder.loadClass(testedClass3);
		unit3 = environmentBuilder.getCompilationUnit(testedClass3);
		unit3.recordModifications();
		// unit4
		Class<?> testedClass4 = UnprotectedMainProgramWithoutTryExample.class;
		environmentBuilder.loadClass(testedClass4);
		unit4 = environmentBuilder.getCompilationUnit(testedClass4);
		unit4.recordModifications();
		// unit5
		Class<?> testedClass5 = UnprotectedmainProgramWithTryAtFirstStatement.class;
		environmentBuilder.loadClass(testedClass5);
		unit5 = environmentBuilder.getCompilationUnit(testedClass5);
		unit5.recordModifications();
		// unit6
		Class<?> testedClass6 = UnprotectedMainProgramWithTryAtMiddleStatement.class;
		environmentBuilder.loadClass(testedClass6);
		unit6 = environmentBuilder.getCompilationUnit(testedClass6);
		unit6.recordModifications();
		// unit7
		Class<?> testedClass7 = UnprotectedMainProgramWithTryAtLastStatement.class;
		environmentBuilder.loadClass(testedClass7);
		unit7 = environmentBuilder.getCompilationUnit(testedClass7);
		unit7.recordModifications();
		// unit8
		Class<?> testedClass8 = UnprotectedMainProgramWithTry.class;
		environmentBuilder.loadClass(testedClass8);
		unit8 = environmentBuilder.getCompilationUnit(testedClass8);
		unit8.recordModifications();
		// unit9
		Class<?> testedClass9 = UnprotectedMainProgramWithoutCatchRightExceptionExample.class;
		environmentBuilder.loadClass(testedClass9);
		unit9 = environmentBuilder.getCompilationUnit(testedClass9);
		unit9.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	@Test
	public void testGetNumber() throws Exception {
		// case 1 : annotation above method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit1.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit1);
		List<MethodDeclaration> list = methodCollector.getMethodList();
		MethodDeclaration md = list.get(0);
		// check precondition
		assertEquals(1, md.getBody().statements().size());
		// if there is an annotation on main function, the start position of the
		// method will start at annotation.
		// so the marker need to be added at next line.
		assertEquals(7, unit1.getLineNumber(md.getStartPosition()));
		// test target
		Method getLineNumber = UnprotectedMainProgramVisitor.class
				.getDeclaredMethod("getLineNumber", MethodDeclaration.class);
		getLineNumber.setAccessible(true);
		// check postcondition
		assertEquals(8, getLineNumber.invoke(mainVisitor, md));

		// case 2 : method without annotation
		methodCollector = new ASTMethodCollector();
		unit2.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit2);
		list = methodCollector.getMethodList();
		md = list.get(0);
		// check precondition
		assertEquals(1, md.getBody().statements().size());
		// due to there is no annotation on main function, so the marker will be added at the first line of main function.
		assertEquals(4, unit2.getLineNumber(md.getStartPosition()));
		// test target & check postcondition
		assertEquals(4, getLineNumber.invoke(mainVisitor, md));
	}

	@Test
	public void testVisit() {
		// case 1 : give the main function
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		unit4.accept(methodCollector);
		List<MethodDeclaration> list = methodCollector.getMethodList();
		MethodDeclaration md = list.get(0);
		// test target & check postcondition
		mainVisitor = new UnprotectedMainProgramVisitor(unit4);
		assertFalse(mainVisitor.visit(md));

		// case 2 : give the other function
		methodCollector = new ASTMethodCollector();
		unit1.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor();
		list = methodCollector.getMethodList();
		md = list.get(0);
		// test target & check postcondition
		assertTrue(mainVisitor.visit(md));

		// case 3 : main body with try block but not catch Exception.class
		methodCollector = new ASTMethodCollector();
		unit9.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit9);
		list = methodCollector.getMethodList();
		md = list.get(0);
		// check precondition
		assertEquals(1, md.getBody().statements().size());
		// test target & check postcondition
		assertFalse(mainVisitor.visit(md));

		// case 4 : main body is empty
		methodCollector = new ASTMethodCollector();
		unit3.accept(methodCollector);
		mainVisitor = new UnprotectedMainProgramVisitor(unit3);
		list = methodCollector.getMethodList();
		md = list.get(0);
		// check precondition
		assertEquals(0, md.getBody().statements().size());
		// test target & check postcondition
		assertTrue(mainVisitor.visit(md));

	}

	@Test
	public void testUnprotectedMainProgramExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit1);
		unit1.accept(mainVisitor);
		assertEquals(0, mainVisitor.getUnprotectedMainList().size());
	}

	@Test
	public void testUnprotectedMainProgramWithoutCatchExceptionExample_doNotDetect() {
		smellSettings.setSmellTypeAttribute(
				SmellSettings.SMELL_UNPROTECTEDMAINPROGRAM,
				SmellSettings.ATTRIBUTE_ISDETECTING, false);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		mainVisitor = new UnprotectedMainProgramVisitor(unit2);
		unit2.accept(mainVisitor);
		assertEquals(0, mainVisitor.getUnprotectedMainList().size());
	}

	@Test
	public void testUnprotectedMainProgramWithCatchRuntimeExceptionExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit2);
		unit2.accept(mainVisitor);
		assertEquals(0, mainVisitor.getUnprotectedMainList().size());
	}

	@Test
	public void testUnprotectedMainProgramWithoutCatchExceptionExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit9);
		unit9.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}

	@Test
	public void testUnprotectedMainProgramWithoutStatementExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit3);
		unit3.accept(mainVisitor);
		assertEquals(0, mainVisitor.getUnprotectedMainList().size());
	}

	@Test
	public void testUnprotectedMainProgramWithoutTryExample() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit4);
		unit4.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}

	@Test
	public void testUnprotectedmainProgramWithTryAtFirstStatement() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit5);
		unit5.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}

	@Test
	public void testUnprotectedMainProgramWithTryAtLastStatement() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit6);
		unit6.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}

	@Test
	public void testUnprotectedMainProgramWithTryAtMiddleStatement() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit7);
		unit7.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}

	@Test
	public void testUnprotectedMainProgramWithTry() {
		mainVisitor = new UnprotectedMainProgramVisitor(unit8);
		unit8.accept(mainVisitor);
		assertEquals(1, mainVisitor.getUnprotectedMainList().size());
	}
}
