 package ntut.csie.analyzer.nested;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.empty.EmptyCatchBlockExample;
import ntut.csie.analyzer.empty.EmptyCatchBlockVisitor;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.testutility.Assertor;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NestedTryStatementVisitorTest {
	CompilationUnit compilationUnit;
	private TestEnvironmentBuilder environmentBuilder;
	SmellSettings smellSettings;
	NestedTryStatementVisitor nestedTryStatementVisitor;
	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("NestedTryStatementExampleProject");
		environmentBuilder.createEnvironment();
		smellSettings = environmentBuilder.getSmellSettings();
		Class<?> testedClass = NestedTryStatementExample.class;
		environmentBuilder.loadClass(testedClass);
		compilationUnit = environmentBuilder.getCompilationUnit(testedClass);
		compilationUnit.recordModifications();
		nestedTryStatementVisitor = new NestedTryStatementVisitor(
				compilationUnit);
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	@Test
	public void testNestedTryStatementVisitor() {
		assertNotNull(compilationUnit);
		assertNotNull(nestedTryStatementVisitor);
		compilationUnit.accept(nestedTryStatementVisitor);

		Assertor.assertMarkerInfoListSize(27,
				nestedTryStatementVisitor.getNestedTryStatementList());
	}

	@Test
	public void testNestedTryStatementVisitor_doNotDetect() {
		modifyIsDetectingStatus(false);
		nestedTryStatementVisitor = new NestedTryStatementVisitor(
				compilationUnit);

		assertNotNull(compilationUnit);
		assertNotNull(nestedTryStatementVisitor);
		compilationUnit.accept(nestedTryStatementVisitor);

		Assertor.assertMarkerInfoListSize(0,
				nestedTryStatementVisitor.getNestedTryStatementList());
	}

	private void modifyIsDetectingStatus(boolean isDetecting) {
		smellSettings = new SmellSettings(
				UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.setSmellTypeAttribute(
				SmellSettings.SMELL_NESTEDTRYSTATEMENT,
				SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
