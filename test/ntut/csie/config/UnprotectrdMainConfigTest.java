package ntut.csie.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.dummy.DummyHandlerVisitor;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramVisitor;
import ntut.csie.aspect.AddAspectsMarkerResolutionForUnprotectedMain;
import ntut.csie.aspect.BadSmellTypeConfig;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.failFastUT.UnprotectedMain.NoEHBlockInMainExample;
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

public class UnprotectrdMainConfigTest {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnitUnprotectedMain;
	private UnprotectedMainProgramVisitor UnprotectedMainVisitor;
	private SmellSettings smellSettings;
	private List<MarkerInfo> UnprotectedMainMarkerInfos;
	private AddAspectsMarkerResolutionForUnprotectedMain UnprotectedMainResoluation;
	private IMarker marker;
	private Path UnprotectedMainExamplePath;

	@Before
	public void setUp() throws Exception {
		setUpTestingEnvironment();
		UnprotectedMainMarkerInfos = visitCompilationUnitUnprotectedMainAndGetSmellList();
		setUpMethodIndexOfMarkerInfo();
		UnprotectedMainResoluation = new AddAspectsMarkerResolutionForUnprotectedMain(
				"test");
		UnprotectedMainExamplePath = new Path(
				"AddAspectsMarkerResoluationExampleProject"
						+ "/"
						+ JavaProjectMaker.FOLDERNAME_SOURCE
						+ "/"
						+ PathUtils.dot2slash(NoEHBlockInMainExample.class
								.getName())
						+ JavaProjectMaker.JAVA_FILE_EXTENSION);
	}

	private List<MarkerInfo> visitCompilationUnitUnprotectedMainAndGetSmellList()
			throws JavaModelException {
		UnprotectedMainVisitor = new UnprotectedMainProgramVisitor(
				compilationUnitUnprotectedMain);
		compilationUnitUnprotectedMain.accept(UnprotectedMainVisitor);
		return UnprotectedMainVisitor.getUnprotectedMainList();
	}

	private String readFile(String fileName) throws FileNotFoundException,
			IOException, UnsupportedEncodingException {
		String ReadContent;
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/UnprotectedMain/" + fileName + ".java";
		File file = new File(packages);
		FileInputStream fis = new FileInputStream(file);
		byte[] data = new byte[(int) file.length()];
		fis.read(data);
		fis.close();
		ReadContent = new String(data, "UTF-8");
		return ReadContent;
	}

	private void setUpTestingEnvironment() throws Exception, JavaModelException {
		environmentBuilder = new TestEnvironmentBuilder(
				"AddAspectsMarkerResoluationExampleProject");
		environmentBuilder.createEnvironment();
		environmentBuilder.loadClass(NoEHBlockInMainExample.class);
		compilationUnitUnprotectedMain = environmentBuilder
				.getCompilationUnit(NoEHBlockInMainExample.class);
		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	private void setUpMethodIndexOfMarkerInfo() throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnitUnprotectedMain.accept(methodCollector);
		for (MarkerInfo m : UnprotectedMainMarkerInfos) {
			for (MethodDeclaration method : methodCollector.getMethodList()) {
				int methodDeclarationIdx = -1;
				methodDeclarationIdx++;

				if (method.toString().indexOf("main") > -1) {
					m.setMethodIndex(methodDeclarationIdx);
					break;
				}
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
					.toString(UnprotectedMainMarkerInfos.get(index)
							.getMethodIndex()));
			tempMarker.setAttribute(IMarker.LINE_NUMBER, new Integer(
					UnprotectedMainMarkerInfos.get(index).getLineNumber()));
			tempMarker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE,
					UnprotectedMainMarkerInfos.get(index).getCodeSmellType());

		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Assert.fail("throw exception");
		}
		return tempMarker;
	}

	@After
	public void tearDown() throws Exception {
		environmentBuilder.cleanEnvironment();
	}

	@Test
	public void getMethodDeclarationForUnprotectedMain() {
		String expected = "";
		try {
			expected = readFile("NoEHBlockInMainExample");

			expected = expected.substring(
					expected.indexOf("public static void main"),
					expected.indexOf("public static void demo()")).replaceAll(
					"\\s", "");
		} catch (IOException e) {
			e.printStackTrace();
		}
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				UnprotectedMainExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getMethodDeclarationWhichHasBadSmell()
				.toString().replaceAll("\\s", "");
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void getAllMethodInvocationInMainTest() {
		String expected ="[demo,println]";
		marker = getSpecificMarkerByMarkerInfoIndex(0,
				UnprotectedMainExamplePath);
		BadSmellTypeConfig builder = new BadSmellTypeConfig(marker);
		String actual = builder.getAllMethodInvocationInMain()
				.toString().replaceAll("\\s", "");
		Assert.assertEquals(expected, actual);
	}
	
	
}
