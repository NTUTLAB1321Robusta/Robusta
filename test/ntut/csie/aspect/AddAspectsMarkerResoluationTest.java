package ntut.csie.aspect;

import static org.junit.Assert.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.BadSmellVisitorFactory;
import ntut.csie.analyzer.SuppressWarningVisitor;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.careless.CarelessCleanupDefinitionExample;
import ntut.csie.analyzer.careless.ClosingResourceBeginningPositionFinder;
import ntut.csie.analyzer.dummy.DummyHandlerVisitor;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.data.SSMessage;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.ASTNodeFinder;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.util.AbstractBadSmellVisitor;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TryStatement;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AddAspectsMarkerResoluationTest {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnit;
	private DummyHandlerVisitor adVisitor;
	private SmellSettings smellSettings;
	private List<MarkerInfo> markerInfos;
	private AddAspectsMarkerResoluation resoluation;

	@Before
	public void setUp() throws Exception {
		setUpTestingEnvironment();
		detectPrintStackTrace();
		markerInfos = visitCompilationAndGetSmellList();
		setUpMethodIndexOfMarkerInfo();
		resoluation = new AddAspectsMarkerResoluation("test");
	}

	private void setUpTestingEnvironment() throws Exception, JavaModelException {
		environmentBuilder = new TestEnvironmentBuilder(
				"AddAspectsMarkerResoluationExampleProject");
		environmentBuilder.createEnvironment();
		environmentBuilder.loadClass(AddAspectsMarkerResoluationExample.class);
		environmentBuilder
				.loadClass(TestObjectAForAddAspectsMarkerResoluation.class);
		environmentBuilder
				.loadClass(TestObjectBForAddAspectsMarkerResoluation.class);
		compilationUnit = environmentBuilder
				.getCompilationUnit(AddAspectsMarkerResoluationExample.class);
		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	private void setUpMethodIndexOfMarkerInfo() throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		int methodIdx = -1;
		int markerInfosIndex = 0;
		for (MethodDeclaration method : methodCollector.getMethodList()) {
			methodIdx++;
			markerInfos.get(markerInfosIndex).setMethodIndex(methodIdx);
			markerInfosIndex++;
		}
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	@Test
	public void testGetFirstMethodDeclaration() {
		int theFirstMarkerInfo = 0;
		MockMarker marker = getSpecificMarkerByMarkerInfoIndex(theFirstMarkerInfo);
		Method method = getPrivateMethodWhichInputIsInterface(resoluation,
				"getMethodDeclaration", marker);
		try {
			MethodDeclaration returnValue = (MethodDeclaration) method.invoke(
					resoluation, marker);
			Assert.assertEquals(
					"aspectCaseForOneMethodInvocationWhichWillNotThrowIOException",
					returnValue.getName().toString());
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetThirdMethodDeclaration() {
		int thethirdMarkerInfo = 2;
		MockMarker marker = getSpecificMarkerByMarkerInfoIndex(thethirdMarkerInfo);
		Method method = getPrivateMethodWhichInputIsInterface(resoluation,
				"getMethodDeclaration", marker);
		try {
			MethodDeclaration returnValue = (MethodDeclaration) method.invoke(
					resoluation, marker);
			Assert.assertEquals(
					"aspectCaseForTwoMethodInvocationAndLastInvocationWillThrowIOException",
					returnValue.getName().toString());
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetLastMethodDeclaration() {
		int theLastMarkerInfo = 4;
		MockMarker marker = getSpecificMarkerByMarkerInfoIndex(theLastMarkerInfo);
		Method method = getPrivateMethodWhichInputIsInterface(resoluation,
				"getMethodDeclaration", marker);
		try {
			MethodDeclaration returnValue = (MethodDeclaration) method.invoke(
					resoluation, marker);
			Assert.assertEquals(
					"aspectCaseForTwoMethodInvocationAndTheFirstInvocationWillThrowIOException",
					returnValue.getName().toString());
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetAllTryStatementOfMethodDeclaration() {
		MethodDeclaration methodDec = ASTNodeFinder
				.getMethodDeclarationNodeByName(compilationUnit,
						"testDataForGetAllTryStatementOfMethodDeclaration");
		Method method = getPrivateMethodWhichInputIsObject(resoluation,
				"getAllTryStatementOfMethodDeclaration", methodDec);
		try {
			List<TryStatement> List = (List<TryStatement>) method.invoke(
					resoluation, methodDec);
			Assert.assertEquals(3, List.size());
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetFirstBadSmellLineNumberFromMarker() {
		int theFirstMarkerInfo = 0;
		MockMarker marker = getSpecificMarkerByMarkerInfoIndex(theFirstMarkerInfo);
		Method method = getPrivateMethodWhichInputIsInterface(resoluation,
				"getBadSmellLineNumberFromMarker", marker);
		try {
			int returnValue = (Integer) method.invoke(resoluation, marker);
			Assert.assertEquals(13, returnValue);
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetThirdBadSmellLineNumberFromMarker() {
		int theThirdMarkerInfo = 2;
		MockMarker marker = getSpecificMarkerByMarkerInfoIndex(theThirdMarkerInfo);
		Method method = getPrivateMethodWhichInputIsInterface(resoluation,
				"getBadSmellLineNumberFromMarker", marker);
		try {
			int returnValue = (Integer) method.invoke(resoluation, marker);
			Assert.assertEquals(33, returnValue);
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetLastBadSmellLineNumberFromMarker() {
		int theLastMarkerInfo = 4;
		MockMarker marker = getSpecificMarkerByMarkerInfoIndex(theLastMarkerInfo);
		Method method = getPrivateMethodWhichInputIsInterface(resoluation,
				"getBadSmellLineNumberFromMarker", marker);
		try {
			int returnValue = (Integer) method.invoke(resoluation, marker);
			Assert.assertEquals(55, returnValue);
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetMethodReturnTypeWhichWillReturnObject() {
		MethodInvocation methodInv = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(compilationUnit,
						"testDataForgetMethodReturnType",
						"a.getTestObjectBWillThrowIOException()").get(0);
		Method method = getPrivateMethodWhichInputIsObject(resoluation,
				"getMethodReturnType", methodInv);
		try {
			String returnValue = (String) method.invoke(resoluation, methodInv);
			Assert.assertEquals("TestObjectBForAddAspectsMarkerResoluation",
					returnValue);
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetMethodReturnTypeWhichWillNotReturn() {
		MethodInvocation methodInv = ASTNodeFinder
				.getMethodInvocationByMethodNameAndCode(
						compilationUnit,
						"aspectCaseForTwoMethodInvocationAndTheFirstInvocationWillThrowIOException",
						"a.getTestObjectBWillThrowIOException().doSomethingWillNotThrowException()")
				.get(0);
		Method method = getPrivateMethodWhichInputIsObject(resoluation,
				"getMethodReturnType", methodInv);
		try {
			String returnValue = (String) method.invoke(resoluation, methodInv);
			Assert.assertEquals("void", returnValue);
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInputAndTheFirstInvocationWillThrowException() {
		TryStatement tryStatement = ASTNodeFinder
				.getTryStatementNodeListByMethodDeclarationName(
						compilationUnit,
						"aspectCaseForTwoMethodInvocationAndTheFirstInvocationWillThrowIOException")
				.get(0);
		String exceptionType = "IOException";
		Method method = getPrivateMethodWhichTwoInputIsObject(
				resoluation,
				"getTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInput",
				exceptionType, tryStatement);
		try {
			MethodInvocation returnValue = (MethodInvocation) method.invoke(
					resoluation, exceptionType, tryStatement);
			Assert.assertEquals("a.getTestObjectBWillThrowIOException()",
					returnValue.getExpression() + "." + returnValue.getName()
							+ "()");
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInputAndTheLastInvocationWillThrowException() {
		TryStatement tryStatement = ASTNodeFinder
				.getTryStatementNodeListByMethodDeclarationName(
						compilationUnit,
						"aspectCaseForTwoMethodInvocationAndLastInvocationWillThrowIOException")
				.get(0);
		String exceptionType = "IOException";
		Method method = getPrivateMethodWhichTwoInputIsObject(
				resoluation,
				"getTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInput",
				exceptionType, tryStatement);
		try {
			MethodInvocation returnValue = (MethodInvocation) method.invoke(
					resoluation, exceptionType, tryStatement);
			Assert.assertEquals(
					"a.getTestObjectB().doSomethingWillThrowIOException()",
					returnValue.getExpression() + "." + returnValue.getName()
							+ "()");
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInputAndTheAllInvocationWillThrowException() {
		TryStatement tryStatement = ASTNodeFinder
				.getTryStatementNodeListByMethodDeclarationName(
						compilationUnit,
						"aspectCaseForTwoMethodInvocationAndTheTwoInvocationWillThrowIOException")
				.get(0);
		String exceptionType = "IOException";
		Method method = getPrivateMethodWhichTwoInputIsObject(
				resoluation,
				"getTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInput",
				exceptionType, tryStatement);
		try {
			MethodInvocation returnValue = (MethodInvocation) method.invoke(
					resoluation, exceptionType, tryStatement);
			Assert.assertEquals("a.getTestObjectBWillThrowIOException()",
					returnValue.getExpression() + "." + returnValue.getName()
							+ "()");
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	@Test
	public void testGetTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInputAndNotAnyInvocationWillThrowException() {
		TryStatement tryStatement = ASTNodeFinder
				.getTryStatementNodeListByMethodDeclarationName(
						compilationUnit,
						"aspectCaseForTwoMethodInvocationAndLastInvocationWillNotThrowIOException")
				.get(0);
		String exceptionType = "IOException";
		Method method = getPrivateMethodWhichTwoInputIsObject(
				resoluation,
				"getTheFirstMethodInvocationWhichWillThrowTheSameExceptionAsInput",
				exceptionType, tryStatement);
		try {
			MethodInvocation returnValue = (MethodInvocation) method.invoke(
					resoluation, exceptionType, tryStatement);
			Assert.assertNull(returnValue);
		} catch (IllegalAccessException e) {
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			Assert.fail("throw exception");
		}
	}

	//this test case will cause eclipse UI and core throw exception
	@Test
	public void testGetExceptionTypeOfCatchClauseWhichHasBadSmell() {
		int badSmellLineNumber = 13;
		TryStatement tryStatement = ASTNodeFinder
				.getTryStatementNodeListByMethodDeclarationName(
						compilationUnit,
						"aspectCaseForOneMethodInvocationWhichWillNotThrowIOException").get(0);
		List<CatchClause> catchClauses = (List<CatchClause>)tryStatement.catchClauses();
		Method method = null;
		try {
			method = AddAspectsMarkerResoluation.class.getDeclaredMethod("getExceptionTypeOfCatchClauseWhichHasBadSmell",
					Integer.TYPE,  List.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (SecurityException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		method.setAccessible(true);
		resoluation.setCompilationUnitForTesting(compilationUnit);		
		try {
			String returnValue = (String) method.invoke(
					resoluation, badSmellLineNumber, catchClauses);
			Assert.assertEquals("IOException", returnValue);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Assert.fail("throw exception");
		}
	}
	
	//this test case will cause eclipse UI and core throw exception
	@Test
	public void testGetTargetTryStetment() {
		int badSmellLineNumber = 79;
		List<TryStatement> tryStatements = ASTNodeFinder.getTryStatementNodeListByMethodDeclarationName(compilationUnit, "testDataForGetAllTryStatementOfMethodDeclaration");
		Method method = null;
		try {
			method = AddAspectsMarkerResoluation.class.getDeclaredMethod("getTargetTryStetment",
					  List.class, Integer.TYPE);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (SecurityException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		method.setAccessible(true);
		resoluation.setCompilationUnitForTesting(compilationUnit);		
		try {
			TryStatement returnValue = (TryStatement) method.invoke(
					resoluation, tryStatements, badSmellLineNumber);
			int returnLineNumber = compilationUnit.getLineNumber(returnValue.getStartPosition());
			Assert.assertEquals(77, returnLineNumber);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			Assert.fail("throw exception");
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			Assert.fail("throw exception");
		} catch (InvocationTargetException e) {
			e.printStackTrace();
			Assert.fail("throw exception");
		}
	}

	private MockMarker getSpecificMarkerByMarkerInfoIndex(int index) {
		MockMarker marker = new MockMarker();
		try {
			marker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX,
					Integer.toString(markerInfos.get(index).getMethodIndex()));
			marker.setAttribute(IMarker.LINE_NUMBER, new Integer(markerInfos
					.get(index).getLineNumber()));
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			Assert.fail("throw exception");
		}
		return marker;
	}

	private Method getPrivateMethodWhichInputIsInterface(Object calzz,
			String methodName, Object parameter) {
		Method method = null;
		try {
			method = calzz.getClass().getDeclaredMethod(methodName,
					parameter.getClass().getInterfaces());
		} catch (NoSuchMethodException e) {
			Assert.fail("throw exception");
		} catch (SecurityException e) {
			Assert.fail("throw exception");
		}
		method.setAccessible(true);
		return method;
	}

	private Method getPrivateMethodWhichInputIsObject(Object calzz,
			String methodName, Object parameter) {
		Method method = null;
		try {
			method = calzz.getClass().getDeclaredMethod(methodName,
					parameter.getClass());
		} catch (NoSuchMethodException e) {
			Assert.fail("throw exception");
		} catch (SecurityException e) {
			Assert.fail("throw exception");
		}
		method.setAccessible(true);
		return method;
	}

	private Method getPrivateMethodWhichTwoInputIsObject(Object calzz,
			String methodName, Object firstParameter, Object secondParameter) {
		Method method = null;
		try {
			method = calzz.getClass().getDeclaredMethod(methodName,
					firstParameter.getClass(), secondParameter.getClass());
		} catch (NoSuchMethodException e) {
			Assert.fail("throw exception");
		} catch (SecurityException e) {
			Assert.fail("throw exception");
		}
		method.setAccessible(true);
		return method;
	}

	private List<MarkerInfo> visitCompilationAndGetSmellList()
			throws JavaModelException {
		adVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(adVisitor);
		return adVisitor.getDummyHandlerList();
	}

	private void detectPrintStackTrace() {
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER,
				SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

}
