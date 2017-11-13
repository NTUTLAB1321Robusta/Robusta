package ntut.csie.analyzer.careless;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import ntut.csie.analyzer.careless.closingmethod.AppendCloseInvocationObject;
import ntut.csie.analyzer.careless.closingmethod.ClassImplementCloseable;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.testutility.TestEnvironmentBuilder;

import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CarelessCleanupExampleFoundInEzScrumTest {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnit;
	private CloseResourceMethodInvocationVisitor visitor;

	@Before
	public void setUp() throws Exception {
		environmentBuilder = new TestEnvironmentBuilder("CarelessCleanupExampleFoundInEzScrumProject");
		environmentBuilder.createEnvironment();

		environmentBuilder.loadClass(CarelessCleanupExampleFoundInEzScrum.class);
		environmentBuilder.loadClass(AppendCloseInvocationObject.class);
		environmentBuilder.loadClass(ClassImplementCloseable.class);
		compilationUnit = environmentBuilder.getCompilationUnit(CarelessCleanupExampleFoundInEzScrum.class);
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	@Test
	public void testCloseAppendedAfterMethodInvocationAndThereISAMethodInvocationAbove()
			throws Exception {
		List<MethodInvocation> carelessCleanupSuspectList = visitCompilationAndGetSmellList();
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeAppendedAfterMethodInvocationAndThereISAMethodInvocationAbove",
				"object.getClosableObj().close()");
		assertTrue(contains(carelessCleanupSuspectList, methodInvocation));
	}

	@Test
	public void testCloseAppendedAfterMethodInvocationAndThereISAMethodInvocationAboveWhichWillThrowIOException()
			throws Exception {
		List<MethodInvocation> carelessCleanupSuspectList = visitCompilationAndGetSmellList();
		MethodInvocation methodInvocation = getMethodInvocationByMethodNameAndCode(
				"closeAppendedAfterMethodInvocationAndThereISAMethodInvocationAboveWhichWillThrowIOException",
				"object.getClosableObj().close()");
		assertTrue(contains(carelessCleanupSuspectList, methodInvocation));
	}

	/* 
	 *  utility methods
	 */
	private MethodInvocation getMethodInvocationByMethodNameAndCode(
			String methodName, String code) {
		List<MethodInvocation> methodInvocation = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						methodName, code);
		assertEquals(methodInvocation.size(), 1);
		return methodInvocation.get(0);
	}

	private List<MethodInvocation> visitCompilationAndGetSmellList()
			throws JavaModelException {
		visitor = new CloseResourceMethodInvocationVisitor(compilationUnit);
		compilationUnit.accept(visitor);
		List<MethodInvocation> miList = visitor.getCloseMethodInvocations();
		return miList;
	}
	
	private boolean contains(List<MethodInvocation> list, MethodInvocation methodInvocationPassedIn) {
		boolean contains = false;
		for(MethodInvocation methodInvocationInList : list) {
			if(methodInvocationInList.toString().equals(methodInvocationPassedIn.toString()))
				contains = true;	
		}
		return contains;
	}
}
