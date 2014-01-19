package ntut.csie.csdet.visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.TestEnvironmentBuilder;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupAdvancedExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupBaseExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.MethodInvocationBeforeClose;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.CloseResourceMethodInvocationExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassCanCloseButNotImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassImplementCloseableWithoutThrowException;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ResourceCloser;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.UserDefinedCarelessCleanupClass;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.UserDefinedCarelessCleanupMethod;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
		environmentBuilder = new TestEnvironmentBuilder("testProject");
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
	public void testGetCloseMethodInvocationListWithOutAnyExtraRule() throws Exception {
		// create visitor and visit the class
		CompilationUnit compilationUnit = environmentBuilder
				.getCompilationUnit(CloseResourceMethodInvocationExample.class);
		visitor = new CloseResourceMethodInvocationVisitor(compilationUnit);
		compilationUnit.accept(visitor);
		
		assertEquals(4, visitor.getCloseMethodInvocations().size());
	}
	
	@Ignore
	public void testGetCloseMethodInvocationListWithExtraRule() throws Exception {
		smellSettings.addExtraRule(SmellSettings.SMELL_CARELESSCLEANUP, SmellSettings.EXTRARULE_CARELESSCLEANUP_DETECTISRELEASEIOCODEINDECLAREDMETHOD);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		environmentBuilder.accept(CloseResourceMethodInvocationExample.class, visitor);
		assertEquals(39, visitor.getCloseMethodInvocations().size());
	}
	
	@Ignore
	public void testGetCloseMethodInvocationListWithUserDefiendLibs() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupMethod.class.getName() + ".*", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		environmentBuilder.accept(null, visitor);
		assertEquals(42, visitor.getCloseMethodInvocations().size());
	}
	
	@Ignore
	public void testGetCloseMethodInvocationListWithUserDefinedMethods() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern("*.bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		environmentBuilder.accept(null, visitor);
		assertEquals(38, visitor.getCloseMethodInvocations().size());
	}
	
	@Ignore
	public void testGetCloseMethodInvocationListWithUserDefinedFullQualifiedMethods() throws Exception {
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.addCarelessCleanupPattern(UserDefinedCarelessCleanupMethod.class.getName() + ".bark", true);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		environmentBuilder.accept(null, visitor);
		assertEquals(37, visitor.getCloseMethodInvocations().size());
	}

}
