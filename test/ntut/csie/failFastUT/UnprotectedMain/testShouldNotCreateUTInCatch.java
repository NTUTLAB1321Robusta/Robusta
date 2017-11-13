package ntut.csie.failFastUT.UnprotectedMain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.dummy.DummyHandlerVisitor;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramVisitor;
import ntut.csie.aspect.AddAspectsMarkerResolutionForUnprotectedMain;
import ntut.csie.aspect.BadSmellTypeConfig;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.testutility.TestEnvironmentBuilder;
import ntut.csie.util.PathUtils;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
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


public class testShouldNotCreateUTInCatch {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnit;
	private UnprotectedMainProgramVisitor unprotectedMainVisitor;
	private SmellSettings smellSettings;
	private List<MarkerInfo> markerInfos;
	private AddAspectsMarkerResolutionForUnprotectedMain resoluation;
	private Path addAspectsMarkerResoluationExamplePath;
	private Path addpackagePath;
	private IMarker marker;
	private IProject project;
	private String AspectPackage = "ntut.csie.TestAspectPackage";

	@Before
	public void setUp() throws Exception {
		setUpTestingEnvironment();
		markerInfos = visitCompilationAndGetSmellList();
		setUpMethodIndexOfMarkerInfo();
		resoluation = new AddAspectsMarkerResolutionForUnprotectedMain(
				"test");
		addAspectsMarkerResoluationExamplePath = new Path(
				"AddAspectsMarkerResoluationExampleProject" + "/"
						+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
						+ PathUtils.dot2slash(ShouldNotCreateUTInCatchExample.class.getName())
						+ JavaProjectMaker.JAVA_FILE_EXTENSION);
		// /AddAspectsMarkerResoluationExampleProject/src/ntut/csie/TestAspectPackage
		addpackagePath = new Path("AddAspectsMarkerResoluationExampleProject"
				+ "/" + JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash(AspectPackage));

	}

	private void setUpTestingEnvironment() throws Exception, JavaModelException {
		environmentBuilder = new TestEnvironmentBuilder(
				"AddAspectsMarkerResoluationExampleProject");
		environmentBuilder.createEnvironment();
		environmentBuilder.loadClass(ShouldNotCreateUTInCatchExample.class);
		compilationUnit = environmentBuilder
				.getCompilationUnit(ShouldNotCreateUTInCatchExample.class);
		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	private void setUpMethodIndexOfMarkerInfo() throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);
		for (MarkerInfo m : markerInfos) {
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

	private List<MarkerInfo> visitCompilationAndGetSmellList()
			throws JavaModelException {
		unprotectedMainVisitor = new UnprotectedMainProgramVisitor(compilationUnit);
		compilationUnit.accept(unprotectedMainVisitor);
		return unprotectedMainVisitor.getUnprotectedMainList();
	}

	private IMarker getSpecificMarkerByMarkerInfoIndex(int index) {
		IJavaElement javaElement = JavaCore.create(ResourcesPlugin
				.getWorkspace().getRoot()
				.getFile(addAspectsMarkerResoluationExamplePath));
		IMarker tempMarker = null;
		try {
			tempMarker = javaElement.getResource().createMarker("test.test");

			tempMarker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX,
					Integer.toString(markerInfos.get(index).getMethodIndex()));
			tempMarker.setAttribute(IMarker.LINE_NUMBER, new Integer(
					markerInfos.get(index).getLineNumber()));
			tempMarker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE,
					markerInfos.get(index).getCodeSmellType());

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

	private String readFile(File file) {
		String content = "";
		try {
			FileInputStream fis = new FileInputStream(file);
			byte[] data = new byte[(int) file.length()];
			fis.read(data);
			fis.close();
			content = new String(data, "UTF-8");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return content;
	}

	@Test
	public void shouldNotCreateUTInCatchTest(){
		int theFirstMarkerInfo = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(theFirstMarkerInfo);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResolutionForUnprotectedMain marker = new AddAspectsMarkerResolutionForUnprotectedMain(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathUTFile = createPackagePath + "/testUTFile.java";
		String Actual = marker.buildTestFile(config, packageChain,
				filePathUTFile).replaceAll("\\s", "");
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/failFastUT/UnprotectedMain/ShouldNotCreateUTInCatchExcepted";
		File file = new File(packages);
		String utContentExcepted = "";
		utContentExcepted = readFile(file).replaceAll("\\s", "");
		Assert.assertEquals(utContentExcepted, Actual);
	}
	
}