package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;

import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.TestEnvironmentBuilder;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassCanCloseButNotImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassImplementCloseableWithoutThrowException;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.CloseResourceMethodInvocationExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ResourceCloser;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.UserDefinedCarelessCleanupClass;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.UserDefinedCarelessCleanupMethod;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class CloseResourceMethodInvocationVisitorTest {
	
	private TestEnvironmentBuilder environmentBuilder;
	private SmellSettings smellSettings;
	private CloseResourceMethodInvocationVisitor visitor;
	
	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("testCloseResourceProject");
		environmentBuilder.createTestEnvironment();

		environmentBuilder.loadClass(CloseResourceMethodInvocationExample.class);
		environmentBuilder.loadClass(ClassCanCloseButNotImplementCloseable.class);
		environmentBuilder.loadClass(ClassImplementCloseable.class);
		environmentBuilder.loadClass(UserDefinedCarelessCleanupMethod.class);
		environmentBuilder.loadClass(UserDefinedCarelessCleanupClass.class);
		environmentBuilder.loadClass(ClassImplementCloseableWithoutThrowException.class);
		environmentBuilder.loadClass(ResourceCloser.class);
		
		smellSettings = environmentBuilder.getSmellSettings();

	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanTestEnvironment();
	}
	
	@Test
	public void testExampleWithOutAnyExtraRule() throws Exception {
		List<MethodInvocation> miList = 
				visitCompilationAndGetSmellList(CloseResourceMethodInvocationExample.class);
		assertEquals(4, miList.size());
	}

	@Test
	public void testExampleWithWithUserDefinedMethodShine() throws Exception {
		// Create setting file with user defined
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.addCarelessCleanupPattern("*.Shine", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		List<MethodInvocation> miList = 
				visitCompilationAndGetSmellList(CloseResourceMethodInvocationExample.class);
		
		assertEquals(9, miList.size());
	}
	
	@Ignore
	public void testGetCloseMethodInvocationListWithUserDefiendLibs() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupMethod.class.getName() + ".*", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		environmentBuilder.accept(Object.class, visitor);
		assertEquals(42, visitor.getCloseMethodInvocations().size());
	}
	
	@Ignore
	public void testGetCloseMethodInvocationListWithUserDefinedMethods() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		environmentBuilder.accept(Object.class, visitor);
		assertEquals(38, visitor.getCloseMethodInvocations().size());
	}
	
	@Ignore
	public void testGetCloseMethodInvocationListWithUserDefinedFullQualifiedMethods() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupMethod.class.getName() + ".bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		environmentBuilder.accept(Object.class, visitor);
		assertEquals(37, visitor.getCloseMethodInvocations().size());
	}

	private List<MethodInvocation> visitCompilationAndGetSmellList(Class clazz)
			throws JavaModelException {
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(clazz);
		visitor = new CloseResourceMethodInvocationVisitor(compilationUnit);
		compilationUnit.accept(visitor);
		List<MethodInvocation> miList = visitor.getCloseMethodInvocations();
		return miList;
	}
	
}
