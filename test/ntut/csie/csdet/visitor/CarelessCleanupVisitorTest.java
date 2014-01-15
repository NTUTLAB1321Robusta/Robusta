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
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.filemaker.TestEnvironmentBuilder;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupAdvancedExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.CarelessCleanupBaseExample;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.MethodInvocationBeforeClose;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassCanCloseButNotImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ClassImplementCloseableWithoutThrowException;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CarelessCleanupVisitorTest {
	
	TestEnvironmentBuilder environmentBuilder;
	NewCarelessCleanupVisitor ccVisitor;

	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("CarelessCleanupExampleProject");
		environmentBuilder.createTestEnvironment();

		environmentBuilder.loadClass(CarelessCleanupBaseExample.class);
		environmentBuilder.loadClass(CarelessCleanupAdvancedExample.class);
		//ClassCanCloseButNotImplementCloseable
		//ClassImplementCloseable
		//UserDefinedCarelessCleanupMethod
		//UserDefinedCarelessCleanupClass
		//ClassImplementCloseableWithoutThrowException
		environmentBuilder.loadClass(MethodInvocationBeforeClose.class);
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

		assertEquals(colloectBadSmellListContent(smellList), 8,
				smellList.size());
	}

	@Test
	public void testAdvancedExampleWithDefaultSetting() throws JavaModelException {
		// Create setting file
		CreateSettings();

		List<MarkerInfo> smellList = visitCompilationAndGetSmellList(CarelessCleanupAdvancedExample.class);

		assertEquals(
				colloectBadSmellListContent(smellList),
				7, smellList.size()); // 7(now6)
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
	
	/**
	 * 紀錄所有badSmell內容以及行號
	 * @param badSmellList
	 * @return
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
