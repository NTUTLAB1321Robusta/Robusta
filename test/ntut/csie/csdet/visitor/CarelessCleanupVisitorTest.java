package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.aidvisitor.MethodInvocationMayInterruptByExceptionChecker;
import ntut.csie.csdet.visitor.aidvisitor.MethodInvocationMayInterruptByExceptionCheckerExample;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.TestEnvironmentBuilder;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupAdvancedExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupBaseExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupIntegratedExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.MethodInvocationBeforeClose;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassCanCloseButNotImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassImplementCloseableWithoutThrowException;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ResourceCloser;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.UserDefinedCarelessCleanupClass;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.UserDefinedCarelessCleanupMethod;
import ntut.csie.robusta.util.PathUtils;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CarelessCleanupVisitorTest {
	
	private TestEnvironmentBuilder environmentBuilder;
	private NewCarelessCleanupVisitor ccVisitor;

	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("CarelessCleanupExampleProject");
		environmentBuilder.createTestEnvironment();

		environmentBuilder.loadClass(CarelessCleanupBaseExample.class);
		environmentBuilder.loadClass(CarelessCleanupAdvancedExample.class);
		environmentBuilder.loadClass(MethodInvocationBeforeClose.class);
		environmentBuilder.loadClass(CarelessCleanupIntegratedExample.class);
		environmentBuilder.loadClass(UserDefinedCarelessCleanupClass.class);
		environmentBuilder.loadClass(UserDefinedCarelessCleanupMethod.class);
		environmentBuilder.loadClass(ResourceCloser.class);
		//ClassCanCloseButNotImplementCloseable
		//ClassImplementCloseable
		//ClassImplementCloseableWithoutThrowException
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanTestEnvironment();
	}

	@Test
	public void testBaseExampleWithDefaultSetting() throws JavaModelException {
		// Create setting file
		CreateSettings();

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupBaseExample.class);

		assertListSize(smellList, 8);
	}

	@Test
	public void testAdvancedExampleWithDefaultSetting()
			throws JavaModelException {
		// Create setting file
		CreateSettings();

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupAdvancedExample.class);

		// FIXME Now the actual will be less 1 then expected, because
		// "super.close() haven been treat as closeInvocation
		assertListSize(smellList, 17);
	}

	@Test
	public void testIntegratedExampleWithDefaultSetting()
			throws JavaModelException {
		// Create setting file
		CreateSettings();

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		assertListSize(smellList, 7);
	}

	@Test
	public void testIntegratedExampleWithFullUserDefined()
			throws JavaModelException {
		// Create setting file with user defined
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupClass.class.getName() + ".*", true);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		assertListSize(smellList, 11);
	}

	@Test
	public void testIntegratedExampleWithUserDefinedClass()
			throws JavaModelException {
		// Create setting file with user defined
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupClass.class.getName() + ".*", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		assertListSize(smellList, 9);
	}

	@Test
	public void testIntegratedExampleWithUserDefinedClassAndMethod()
			throws JavaModelException {
		// Create setting file with user defined
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupClass.class.getName() + ".bite", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		assertListSize(smellList, 8);
	}

	@Test
	public void testIntegratedExampleWithUserDefinedMethod()
			throws JavaModelException {
		// Create setting file with user defined
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		assertListSize(smellList, 10);
	}

	private List<MarkerInfo> visitCompilationAndGetSmellList(Class clazz)
			throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(clazz);
		ccVisitor = new NewCarelessCleanupVisitor(compilationUnit);
		compilationUnit.accept(ccVisitor);
		List<MarkerInfo> smellList = ccVisitor.getCarelessCleanupList();
		return smellList;
	}
	
	private void assertListSize(List<MarkerInfo> smellList, int size) {
		assertEquals(colloectBadSmellListContent(smellList), size,
				smellList.size());
	}

	/**
	 * append bad smell to a string and return it
	 */
	private String colloectBadSmellListContent(List<MarkerInfo> badSmellList) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		for (int i = 0; i < badSmellList.size(); i++) {
			MarkerInfo m = badSmellList.get(i);
			sb.append(m.getLineNumber()).append("\t").append(m.getStatement()).append("\n");
		}
		return sb.toString();
	}

	private void CreateSettings() {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
