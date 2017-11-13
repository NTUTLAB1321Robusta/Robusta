package ntut.csie.aspect.Dummy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.dummy.DummyHandlerVisitor;
import ntut.csie.aspect.AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock;
import ntut.csie.aspect.BadSmellTypeConfig;
import ntut.csie.aspect.MethodDeclarationVisitor;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.failFastUT.Dummy.dummyExample;
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

public class TestGenerateAspectJFileForDummy {
	private TestEnvironmentBuilder environmentBuilder;
	private CompilationUnit compilationUnit;
	private DummyHandlerVisitor adVisitor;
	private SmellSettings smellSettings;
	private List<MarkerInfo> markerInfos;
	private AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock resoluation;
	private Path addAspectsMarkerResoluationExamplePath;
	private Path addpackagePath;
	private IMarker marker;
	private IProject project;
	private String AspectPackage = "ntut.csie.test.DummyHandler";
	
	@Before
	public void setUp() throws Exception {
		setUpTestingEnvironment();
		detectPrintStackTrace();
		markerInfos = visitCompilationAndGetSmellList();
		setUpMethodIndexOfMarkerInfo();
		resoluation = new AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock(
				"test");
		addAspectsMarkerResoluationExamplePath = new Path(
				"AddAspectsMarkerResoluationExampleProject" + "/"
						+ JavaProjectMaker.FOLDERNAME_SOURCE + "/"
						+ PathUtils.dot2slash(dummyExample.class.getName())
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
		environmentBuilder.loadClass(dummyExample.class);
		compilationUnit = environmentBuilder
				.getCompilationUnit(dummyExample.class);
		// Get empty setting
		smellSettings = environmentBuilder.getSmellSettings();
	}

	private void setUpMethodIndexOfMarkerInfo() throws JavaModelException {
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		compilationUnit.accept(methodCollector);

		for (MarkerInfo m : markerInfos) {
			for (MethodDeclaration method : methodCollector.getMethodList()) {
				int methodIdx = -1;
				methodIdx++;
				MethodDeclarationVisitor declarationVisitor = new MethodDeclarationVisitor(
						compilationUnit);
				method.accept(declarationVisitor);

				for (Integer a_integer : declarationVisitor
						.getCatchClauseLineNumberList()) {
					if (m.getLineNumber() == (int) a_integer) {
						m.setMethodIndex(methodIdx);
						break;
					}
				}
				if (m.getMethodIndex() != -1)
					break;
			}
		}
	}

	private void detectPrintStackTrace() {
		smellSettings.addExtraRule(SmellSettings.SMELL_DUMMYHANDLER,
				SmellSettings.EXTRARULE_ePrintStackTrace);
		smellSettings.writeXMLFile(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}

	private List<MarkerInfo> visitCompilationAndGetSmellList()
			throws JavaModelException {
		adVisitor = new DummyHandlerVisitor(compilationUnit);
		compilationUnit.accept(adVisitor);
		return adVisitor.getDummyHandlerList();
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

	@Test
	public void createPackageTest() {
		int theFirstMarkerInfo = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(theFirstMarkerInfo);
		AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock markerResoluationForDummyAndEmpty = new AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock(
				"test");
		markerResoluationForDummyAndEmpty.setMarker(marker);
		markerResoluationForDummyAndEmpty.createPackage(AspectPackage);
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		File file = new File(createPackagePath);
		Assert.assertEquals(true, file.exists());
	}
	

	@Test
	public void buildUpAspectsFileTest() {
		int theFirstMarkerInfo = 0;
		marker = getSpecificMarkerByMarkerInfoIndex(theFirstMarkerInfo);
		BadSmellTypeConfig config = new BadSmellTypeConfig(marker);
		AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock marker = new AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock(
				"test");
		String packageChain = AspectPackage;
		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();
		String createPackagePath = workSpacePath + "/" + addpackagePath;
		String filePathAspectJFile = createPackagePath + "/testAspectJFile.aj";
		String Actual = config.buildUpAspectsFile(packageChain,
				filePathAspectJFile);
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/aspect/Dummy/AJFileExcepted";
		File file = new File(packages);
		String aspectContentExpected = "";
		aspectContentExpected = readFile(file);
		Assert.assertEquals(aspectContentExpected, Actual);
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
	public void buildUpAspectJSwitchFile() {
		String currentDirPath = System.getProperty("user.dir");
		String packages = currentDirPath + File.separator
				+ "test/ntut/csie/aspect/AspectJSwitchExpected";
		File file = new File(packages);
		String excepted = readFile(file);
		AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock am = new AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock(
				"add Aspect");
		String packageChain = "ntut.csie.aspect";
		Assert.assertEquals(excepted, am.buildUpAspectJSwitch(packageChain));

	}

	@Test
	public void writeFileTest() {
		AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock testWriteFile = new AddAspectsMarkerResoluationForDummyHandlerAndEmptyCatchBlock(
				"test");
		testWriteFile.buildUpAspectJSwitch(AspectPackage);
		String aspectFileContent = testWriteFile
				.buildUpAspectJSwitch(AspectPackage);
		addpackagePath = new Path("AddAspectsMarkerResoluationExampleProject"
				+ "/" + JavaProjectMaker.FOLDERNAME_SOURCE + "/"
				+ PathUtils.dot2slash("ntut.csie.failFastUT"));

		String workSpacePath = ResourcesPlugin.getWorkspace().getRoot()
				.getLocation().toString();

		String aspectJSwitchFilePath = workSpacePath + "/" + addpackagePath
				+ "/AspectJSwitch.java";
		
		testWriteFile.WriteFile(aspectFileContent, aspectJSwitchFilePath);
		File file = new File(aspectJSwitchFilePath);

		Assert.assertEquals(true, file.exists());

		String expected = readFile(file);
		expected = expected.trim();
		Assert.assertEquals(expected, aspectFileContent);
	}

}
