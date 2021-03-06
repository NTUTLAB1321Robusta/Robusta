package ntut.csie.analyzer.careless.closingmethod;

import java.util.List;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.careless.CloseResourceMethodInvocationVisitor;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.testutility.Assertor;
import ntut.csie.testutility.TestEnvironmentBuilder;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CloseResourceMethodInvocationVisitorTest {
	
	private TestEnvironmentBuilder environmentBuilder;
	private CloseResourceMethodInvocationVisitor visitor;
	
	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("testCloseResourceProject");
		environmentBuilder.createEnvironment();

		environmentBuilder.loadClass(CloseResourceMethodInvocationExample.class);
		environmentBuilder.loadClass(ClassCanCloseButNotImplementCloseable.class);
		environmentBuilder.loadClass(ClassImplementCloseable.class);
		environmentBuilder.loadClass(UserDefinedCarelessCleanupMethod.class);
		environmentBuilder.loadClass(UserDefinedCarelessCleanupClass.class);
		environmentBuilder.loadClass(ClassImplementCloseableWithoutThrowException.class);
		environmentBuilder.loadClass(ResourceCloser.class);
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}
	
	@Test
	public void testVisitorWithoutAnyExtraRule() throws Exception {
		List<MethodInvocation> miList = 
				visitCompilationAndGetCloseMethodInvocation(CloseResourceMethodInvocationExample.class);
		Assertor.assertListSize(7, miList);
	}

	@Test
	public void testVisitorWithUserDefinedMethodClose() throws Exception {
		// Create user defined setting file
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.addCarelessCleanupPattern("*.close", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MethodInvocation> miList = 
				visitCompilationAndGetCloseMethodInvocation(CloseResourceMethodInvocationExample.class);

		Assertor.assertListSize(9, miList);
	}
	
	@Test
	public void testVisitorWithUserDefinedMethodBark() throws Exception {
		// Create user defined setting file
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MethodInvocation> miList = 
				visitCompilationAndGetCloseMethodInvocation(CloseResourceMethodInvocationExample.class);

		Assertor.assertListSize(9, miList);
	}

	@Test
	public void testVisitorWithUserDefinedClassUserDefinedCarelessCleanupClass() throws Exception {
		// Create user defined setting file
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		String className = "ntut.csie.analyzer.careless.closingmethod.UserDefinedCarelessCleanupClass"; 
		smellSettings.addCarelessCleanupPattern(className + ".*", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MethodInvocation> miList = 
				visitCompilationAndGetCloseMethodInvocation(CloseResourceMethodInvocationExample.class);

		Assertor.assertListSize(9, miList);
	}
	
	@Test
	public void testVisitorWithUserDefinedFullQualifiedMethods() throws Exception {
		// Create user defined setting file
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_ALSO_DETECT_OUT_OF_TRY_STATEMENT);
		String fullQualifiedName = "ntut.csie.analyzer.careless.closingmethod.UserDefinedCarelessCleanupClass.bark"; 
		smellSettings.addCarelessCleanupPattern(fullQualifiedName, true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MethodInvocation> miList = 
				visitCompilationAndGetCloseMethodInvocation(CloseResourceMethodInvocationExample.class);

		Assertor.assertListSize(8, miList);
	}

	private List<MethodInvocation> visitCompilationAndGetCloseMethodInvocation(Class clazz)
			throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(clazz);
		visitor = new CloseResourceMethodInvocationVisitor(compilationUnit);
		compilationUnit.accept(visitor);
		List<MethodInvocation> miList = visitor.getCloseMethodInvocations();
		return miList;
	}
}
