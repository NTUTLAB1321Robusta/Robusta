package ntut.csie.analyzer.careless;

import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.careless.closingmethod.ResourceCloser;
import ntut.csie.analyzer.careless.closingmethod.UserDefinedCarelessCleanupClass;
import ntut.csie.analyzer.careless.closingmethod.UserDefinedCarelessCleanupMethod;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.testutility.Assertor;
import ntut.csie.testutility.TestEnvironmentBuilder;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CarelessCleanupVisitorTest {
	
	private TestEnvironmentBuilder environmentBuilder;
	private CarelessCleanupVisitor ccVisitor;

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

		Assertor.assertMarkerInfoListSize(8, smellList);
	}

	@Test
	/**
	 * We can not detect "SuperMethodInvocation" now, this test case will ignore that case.
	 */
	public void testAdvancedExampleWithDefaultSettingWithoutSuperMethodInvocation()
			throws JavaModelException {
		// Create setting file
		CreateSettings();

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupAdvancedExample.class);

		Assertor.assertMarkerInfoListSize(19, smellList);
	}

	@Ignore
	/**
	 * If we can detect "SuperMethodInvocation"m then we can run this test case.
	 */
	public void testAdvancedExampleWithDefaultSettingWithSuperMethodInvocation()
			throws JavaModelException {
		// Create setting file
		CreateSettings();

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupAdvancedExample.class);

		/*
		 * The number will over 1 then the expectedSize of test case
		 * "testAdvancedExampleWithDefaultSettingWithoutSuperMethodInvocation"
		 */
		Assertor.assertMarkerInfoListSize(19 + 1, smellList);
	}

	@Test
	public void testIntegratedExampleWithDefaultSetting()
			throws JavaModelException {
		// Create setting file
		CreateSettings();

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		Assertor.assertMarkerInfoListSize(7, smellList);
	}

	@Test
	public void testIntegratedExampleWithFullUserDefined()
			throws JavaModelException {
		// Create setting file with user defined
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupClass.class.getName() + ".*", true);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.addCarelessCleanupPattern("*.close", true);
		smellSettings.addCarelessCleanupPattern("*.closeResourceDirectly", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		Assertor.assertMarkerInfoListSize(13, smellList);
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

		Assertor.assertMarkerInfoListSize(9, smellList);
	}

	@Test
	public void testIntegratedExampleWithUserDefinedBothClassAndMethod()
			throws JavaModelException {
		// Create setting file with user defined
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupClass.class.getName() + ".bite", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		Assertor.assertMarkerInfoListSize(8, smellList);
	}

	@Test
	public void testIntegratedExampleWithUserDefinedMethodBark()
			throws JavaModelException {
		// Create setting file with user defined
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		Assertor.assertMarkerInfoListSize(10, smellList);
	}

	@Test
	public void testIntegratedExampleWithUserDefinedMethodClose()
			throws JavaModelException {
		// Create setting file with user defined
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.addCarelessCleanupPattern("*.close", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		Assertor.assertMarkerInfoListSize(8, smellList);
	}

	private List<MarkerInfo> visitCompilationAndGetSmellList(Class clazz)
			throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(clazz);
		ccVisitor = new CarelessCleanupVisitor(compilationUnit);
		compilationUnit.accept(ccVisitor);
		List<MarkerInfo> smellList = ccVisitor.getCarelessCleanupList();
		return smellList;
	}
	
	private void CreateSettings() {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
}
