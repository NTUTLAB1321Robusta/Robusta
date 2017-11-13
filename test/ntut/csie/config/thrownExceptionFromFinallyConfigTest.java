package ntut.csie.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.thrown.ExceptionThrownFromFinallyBlockVisitor;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramVisitor;
import ntut.csie.aspect.AddAspectsMarkerResolutionForThrowFromFinally;
import ntut.csie.aspect.BadSmellTypeConfig;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.failFastUT.Thrown.thrownFromFinallyExample;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class thrownExceptionFromFinallyConfigTest {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnitThrowFromFinally;
	private ExceptionThrownFromFinallyBlockVisitor throwFromFinallyVisitor;
	private SmellSettings smellSettings;
	private List<MarkerInfo> ThrowFromFinallyMarkerInfos;
	private AddAspectsMarkerResolutionForThrowFromFinally ThrowFromFinallyResoluation;
	private IMarker marker;
	private Path ThrowFromFinallyExamplePath;

	@Before
	public void setUp() throws Exception {
		setUpTestingEnvironment();
		ThrowFromFinallyMarkerInfos = visitCompilationUnitThrowFromFinallyAndGetSmellList();
		setUpMethodIndexOfMarkerInfo();
		ThrowFromFinallyResoluation = new AddAspectsMarkerResolutionForThrowFromFinally(
				"test");
		ThrowFromFinallyExamplePath = new Path(
				"AddAspectsMarkerResoluationExampleProject"
						+ "/"
						+ JavaProjectMaker.FOLDERNAME_SOURCE
						+ "/"
						+ PathUtils.dot2slash(thrownFromFinallyExample.class
								.getName())
						+ JavaProjectMaker.JAVA_FILE_EXTENSION);
	}

	private void setUpTestingEnvironment() throws Exception, JavaModelException {
		environmentBuilder = new TestEnvironmentBuilder(
				"AddAspectsMarkerResoluationExampleProject");
		environmentBuilder.createEnvironment();
		environmentBuilder.loadClass(thrownFromFinallyExample.class);
		compilationUnitThrowFromFinally = environmentBuilder
				.getCompilationUnit(thrownFromFinallyExample.class);
		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	private void setUpMethodIndexOfMarkerInfo() throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnitThrowFromFinally.accept(methodCollector);
		for (MarkerInfo m : ThrowFromFinallyMarkerInfos) {
			int methodIdx = -1;
			for (MethodDeclaration method : methodCollector.getMethodList()) {
								
				methodIdx++;
				
				if(m.getLineNumber()<compilationUnitThrowFromFinally.getLineNumber(method.getStartPosition()))
					m.setMethodIndex(methodIdx-1);

			}
		}
	}

	private IMarker getSpecificMarkerByMarkerInfoIndex(int index, Path filePath) {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin
				.getWorkspace().getRoot().getFile(filePath));
		IMarker tempMarker = null;
		try {
			tempMarker = javaElement.getResource().createMarker("test.test");
			tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, Integer
					.toString(ThrowFromFinallyMarkerInfos.get(index)
							.getMethodIndex()));
			tempMarker.setAttribute(IMarker.LINE_NUMBER, new Integer(
					ThrowFromFinallyMarkerInfos.get(index).getLineNumber()));
			tempMarker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE,
					ThrowFromFinallyMarkerInfos.get(index).getCodeSmellType());

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("throw exception");
		}
		return tempMarker;
	}

	private List<MarkerInfo> visitCompilationUnitThrowFromFinallyAndGetSmellList()
			throws JavaModelException {
		throwFromFinallyVisitor = new ExceptionThrownFromFinallyBlockVisitor(
				compilationUnitThrowFromFinally);
		compilationUnitThrowFromFinally.accept(throwFromFinallyVisitor);
		return throwFromFinallyVisitor.getThrownInFinallyList();
	}

	private String readFile(String fileName) throws FileNotFoundException,
			IOException, UnsupportedEncodingException {
		String ReadContent;
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/Thrown/" + fileName + ".java";
		File file = new File(packages);
		FileInputStream fis = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();
		ReadContent = new String(data, "UTF-8");
		return ReadContent;
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}
	
	@Test
	public void getTryStatementsTest(){
		String expected = "";
		try {
			expected = readFile("thrownFromFinallyExample");

			expected = expected.substring(
					expected.indexOf("try {"),
					expected.indexOf("private static")).replaceAll(
					"\\s", "");
			expected = expected.substring(0 ,expected.lastIndexOf("}"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getTryStatements().get(0)
				.toString().replaceAll("\\s", "");
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void getTryStatementWillBeInjectTest(){
		String expected = "";
		try {
			expected = readFile("thrownFromFinallyExample");

			expected = expected.substring(
					expected.indexOf("try {"),
					expected.indexOf("private static")).replaceAll(
					"\\s", "");
			expected = expected.substring(0 ,expected.lastIndexOf("}"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getTryStatementWillBeInject()
				.toString().replaceAll("\\s", "");
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void getExceptionTypeFirstTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getExceptionType();
		Assert.assertEquals("IOException", actual);
	}
	
	@Test
	public void getExceptionTypeSecondTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(1,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getExceptionType();
		Assert.assertEquals("IOException", actual);
	}
	
	@Test
	public void getExceptionTypeThirdTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(2,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getExceptionType();
		Assert.assertEquals("SQLException", actual);
	}
	
	@Test
	public void getMethodInFinalFirstTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getMethodInFinal();
		Assert.assertEquals("close", actual);
	}
	
	@Test
	public void getMethodInFinalSecondTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(1,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getMethodInFinal();
		Assert.assertEquals("close", actual);
	
	}
	
	@Test
	public void getMethodInFinalThirdTest(){
		marker = getSpecificMarkerByMarkerInfoIndex(2,
				ThrowFromFinallyExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getMethodInFinal();
		Assert.assertEquals("throwEx", actual);
	}
	
}
