package ntut.csie.analyzer.empty;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.analyzer.UserDefineDummyHandlerFish;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.dummy.DummyHandlerVisitor;
import ntut.csie.analyzer.dummy.example.PrintOrLogBySuperMethod;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EmptyCatchBlockVisitorTest {
	CompilationUnit compilationUnit;
	EmptyCatchBlockVisitor emptyCatchBlockVisitor;
	private TestEnvironmentBuilder environmentBuilder;
	SmellSettings smellSettings;
	public EmptyCatchBlockVisitorTest() {
	}
	
	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("EmptyCatchBlockTest");
		environmentBuilder.createEnvironment();
		smellSettings = environmentBuilder.getSmellSettings();
		Class<?> testedClass = EmptyCatchBlockExample.class;
		environmentBuilder.loadClass(testedClass);
		compilationUnit = environmentBuilder.getCompilationUnit(testedClass);
		compilationUnit.recordModifications();
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}
	
	@Test
	public void testVisitNode_withSettingFileAndIsDetectingFalse() {
		int emptyCatchSmellCount = 0;
		modifyIsDetectingStatus(false);
		assertTrue(new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH).exists());
		emptyCatchBlockVisitor = new EmptyCatchBlockVisitor(compilationUnit);
		compilationUnit.accept(emptyCatchBlockVisitor);
		if(emptyCatchBlockVisitor.getEmptyCatchList() != null)
			emptyCatchSmellCount = emptyCatchBlockVisitor.getEmptyCatchList().size();
		
		assertEquals(0, emptyCatchSmellCount);
	}
	
	@Test
	public void testVisitNode_withSettingFileAndIsDetectingTrue() {
		int emptyCatchSmellCount = 0;
		modifyIsDetectingStatus(true);
		assertTrue(new File(UserDefinedMethodAnalyzer.SETTINGFILEPATH).exists());
		emptyCatchBlockVisitor = new EmptyCatchBlockVisitor(compilationUnit);
		compilationUnit.accept(emptyCatchBlockVisitor);		
		if(emptyCatchBlockVisitor.getEmptyCatchList() != null)
			emptyCatchSmellCount = emptyCatchBlockVisitor.getEmptyCatchList().size();
		
		assertEquals(7, emptyCatchSmellCount);
	}

	@Test
	public void testVisitNode_withoutSettingFile() {
		int emptyCatchSmellCount = 0;
		emptyCatchBlockVisitor = new EmptyCatchBlockVisitor(compilationUnit);
		compilationUnit.accept(emptyCatchBlockVisitor);
		if(emptyCatchBlockVisitor.getEmptyCatchList() != null)
			emptyCatchSmellCount = emptyCatchBlockVisitor.getEmptyCatchList().size();
		
		assertEquals(7, emptyCatchSmellCount);
	}
	private void modifyIsDetectingStatus(boolean isDetecting) {
		SmellSettings smellSetting = new SmellSettings(
				UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSetting.setSmellTypeAttribute(
				SmellSettings.SMELL_EMPTYCATCHBLOCK,
				SmellSettings.ATTRIBUTE_ISDETECTING, isDetecting);
		smellSetting.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
