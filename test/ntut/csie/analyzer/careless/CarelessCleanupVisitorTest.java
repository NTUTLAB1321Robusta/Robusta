package ntut.csie.analyzer.careless;

import static org.junit.Assert.*;

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
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CarelessCleanupVisitorTest {
	
	private TestEnvironmentBuilder environmentBuilder;
	private CarelessCleanupVisitor ccVisitor;
	SmellSettings smellSettings;

	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("CarelessCleanupExampleProject");
		environmentBuilder.createEnvironment();

		environmentBuilder.loadClass(CarelessCleanupBaseExample.class);
		environmentBuilder.loadClass(CarelessCleanupAdvancedExample.class);
		environmentBuilder.loadClass(MethodInvocationBeforeClose.class);
		environmentBuilder.loadClass(CarelessCleanupIntegratedExample.class);
		environmentBuilder.loadClass(UserDefinedCarelessCleanupClass.class);
		environmentBuilder.loadClass(UserDefinedCarelessCleanupMethod.class);
		environmentBuilder.loadClass(ResourceCloser.class);
		
		smellSettings = environmentBuilder.getSmellSettings();
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	@Test
	public void testBaseExampleWithDefaultSetting() throws JavaModelException {
		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupBaseExample.class);

		Assertor.assertMarkerInfoListSize(2, smellList);
	}

	@Test
	public void testBaseExampleWithExtraRule() throws JavaModelException {
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP,
				SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupBaseExample.class);

		Assertor.assertMarkerInfoListSize(6, smellList);
	}

	@Test
	/**
	 * Note that there are two sample We can not detect.
	 * 	1. SuperMethodInvocation
	 * 	2. Throw in expression in Do-while Statement
	 */
	public void testAdvancedExampleWithDefaultSetting()
			throws JavaModelException {
		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupAdvancedExample.class);

		Assertor.assertMarkerInfoListSize(4, smellList);
	}

	@Test
	/**
	 * Note that there are two sample We can not detect.
	 * 	1. SuperMethodInvocation
	 * 	2. Throw in expression in Do-while Statement
	 */
	public void testAdvancedExampleWithExtraRule()
			throws JavaModelException {
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP,SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupAdvancedExample.class);

		Assertor.assertMarkerInfoListSize(10, smellList);
	}

	@Ignore
	/**
	 * If we can detect "SuperMethodInvocation" then we can run this test case.
	 */
	public void testSuperMethodInvocation()
			throws JavaModelException {
		// "super.close()" in method "close" in class "ConcreteCloseable"
		SuperMethodInvocation superMethodInvocation = (SuperMethodInvocation) NodeFinder.perform(
						environmentBuilder.getCompilationUnit(CarelessCleanupAdvancedExample.class),
						3474 - 1, 13);
		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(
				CarelessCleanupAdvancedExample.class, superMethodInvocation);

		// Currently it is 0
		Assertor.assertMarkerInfoListSize(1, smellList);
	}

	@Ignore
	/**
	 * If we can detect "Do-while Statement" then we can run this test case.
	 */
	public void testDoWhileStatement()
			throws JavaModelException {
		// "fileInputStream.close()" in method "closeIsTheFirstExecuteStatementButStillUnsafeWithDoWhileStatement"
		MethodInvocation methodInvocation = (MethodInvocation) NodeFinder.perform(
						environmentBuilder.getCompilationUnit(CarelessCleanupAdvancedExample.class),
						5152 - 1, 23);
		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(
				CarelessCleanupAdvancedExample.class, methodInvocation);

		Assertor.assertMarkerInfoListSize(1, smellList);
	}

	final int DEFAULT_BAD_SMELLS_OF_INTEGRATED_EXAMPLE = 5;
	
	@Test
	public void testIntegratedExampleWithDefaultSetting()
			throws JavaModelException {
		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		Assertor.assertMarkerInfoListSize(DEFAULT_BAD_SMELLS_OF_INTEGRATED_EXAMPLE, smellList);
	}

	@Test
	public void testIntegratedExampleWithFullUserDefined()
			throws JavaModelException {
		// Create user defined setting file
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP,
		SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupClass.class.getName() + ".*", true);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.addCarelessCleanupPattern("*.close", true);
		smellSettings.addCarelessCleanupPattern("*.closeResourceDirectly", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		Assertor.assertMarkerInfoListSize(DEFAULT_BAD_SMELLS_OF_INTEGRATED_EXAMPLE + 6, smellList);
	}

	@Test
	public void testIntegratedExampleWithUserDefinedClass()
			throws JavaModelException {
		// Create user defined setting file
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP,
		SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupClass.class.getName() + ".*", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		Assertor.assertMarkerInfoListSize(DEFAULT_BAD_SMELLS_OF_INTEGRATED_EXAMPLE + 2, smellList);
	}

	@Test
	public void testIntegratedExampleWithUserDefinedBothClassAndMethod()
			throws JavaModelException {
		// Create user defined setting file
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP,
		SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupClass.class.getName() + ".bite", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		Assertor.assertMarkerInfoListSize(DEFAULT_BAD_SMELLS_OF_INTEGRATED_EXAMPLE + 1, smellList);
	}

	@Test
	public void testIntegratedExampleWithUserDefinedMethodBark()
			throws JavaModelException {
		// Create user defined setting file
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP,
		SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		Assertor.assertMarkerInfoListSize(DEFAULT_BAD_SMELLS_OF_INTEGRATED_EXAMPLE + 3, smellList);
	}

	@Test
	public void testIntegratedExampleWithUserDefinedMethodClose()
			throws JavaModelException {
		// Create user defined setting file
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP,
				SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.addCarelessCleanupPattern("*.close", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		Assertor.assertMarkerInfoListSize(DEFAULT_BAD_SMELLS_OF_INTEGRATED_EXAMPLE + 1, smellList);
	}

	@Test
	public void testCollectingAnnotationInfoInForCarelessCleanupIntegratedExample() throws JavaModelException {
		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);
		assertEquals(5, smellList.size());
		assertEquals(1, smellList.get(0).getAnnotationList().size());
		assertEquals(1, smellList.get(1).getAnnotationList().size());
		assertEquals(1, smellList.get(2).getAnnotationList().size());
		assertEquals(1, smellList.get(3).getAnnotationList().size());
	}
	
	@Test
	public void testCollectingAnnotationInfoInForCarelessCleanupIntegratedExamplewithUserDefinedClass() throws JavaModelException {
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.addCarelessCleanupPattern(ResourceCloser.class.getName() + ".*", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);
		
		assertEquals(6, smellList.size());
		assertEquals(1, smellList.get(0).getAnnotationList().size());
		assertEquals(1, smellList.get(1).getAnnotationList().size());
		assertEquals(1, smellList.get(2).getAnnotationList().size());
		assertEquals(1, smellList.get(3).getAnnotationList().size());
		assertEquals(1, smellList.get(4).getAnnotationList().size());
		assertEquals(1, smellList.get(5).getAnnotationList().size());
	}

	@Test
	public void testCollectingAnnotationInfoInForCarelessCleanupIntegratedExamplewithUserDefinedMethod() throws JavaModelException {
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupClass.class.getName() + ".bite", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupIntegratedExample.class);

		assertEquals(6, smellList.size());
		assertEquals(1, smellList.get(0).getAnnotationList().size());
		assertEquals(1, smellList.get(1).getAnnotationList().size());
		assertEquals(1, smellList.get(2).getAnnotationList().size());
		assertEquals(1, smellList.get(3).getAnnotationList().size());
		assertEquals(1, smellList.get(4).getAnnotationList().size());
		assertEquals(1, smellList.get(5).getAnnotationList().size());
	}
	
	private List<MarkerInfo> visitCompilationAndGetSmellList(Class clazz)
			throws JavaModelException {
		return visitCompilationAndGetSmellList(clazz,
				environmentBuilder.getCompilationUnit(clazz));
	}

	private List<MarkerInfo> visitCompilationAndGetSmellList(Class clazz, ASTNode node)
			throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(clazz);
		ccVisitor = new CarelessCleanupVisitor(compilationUnit, isDetectOutOfTryInSmellSetting());
		node.accept(ccVisitor);
		List<MarkerInfo> smellList = ccVisitor.getCarelessCleanupList();
		return smellList;
	}

	private boolean isDetectOutOfTryInSmellSetting() {
		return smellSettings.isExtraRuleExist(
				SmellSettings.SMELL_CARELESSCLEANUP,
				SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
	}
}
