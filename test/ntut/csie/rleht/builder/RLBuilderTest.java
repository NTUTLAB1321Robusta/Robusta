package ntut.csie.rleht.builder;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;

import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RLBuilderTest {
	RLBuilder rlBuilder = new RLBuilder();
	JavaFileToString javaFile2String;
	JavaProjectMaker javaProjectMaker;
	CompilationUnit compilationUnit;

	String projectName;
	RobustaSettings robustaSettings ;

	public RLBuilderTest() {
		
	}
	@Before
	public void setUp() throws Exception {
		projectName = "FirstTest";
		
	}
		
	@Test
	public void testIsIResourceNeedToBeDetectedWithNonIFile() throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResource resource = workspace.getRoot().getProject(projectName)
				.getFolder("srcTest");

		Method isIResourceNeedToBeDetected = RLBuilder.class.getDeclaredMethod(
				"isIResourceNeedToBeDetected", IResource.class);
		isIResourceNeedToBeDetected.setAccessible(true);
		Assert.assertFalse((Boolean) isIResourceNeedToBeDetected.invoke(
				rlBuilder, resource));

	}

	@Test
	public void testIsIResourceNeedToBeDetectedWithTxtExtensionFile()
			throws Exception {
		File file =new File("D:/runtime-EclipseApplication/FirstTest/test.txt");
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResource resource = workspace.getRoot().getProject(projectName)
				.getFile(file.getName());

		Method isIResourceNeedToBeDetected = RLBuilder.class.getDeclaredMethod(
				"isIResourceNeedToBeDetected", IResource.class);
		isIResourceNeedToBeDetected.setAccessible(true);
		Assert.assertFalse((Boolean) isIResourceNeedToBeDetected.invoke(
				rlBuilder, resource));
	}

	@Test
	public void testIsIResourceNeedToBeDetectedWithNoExtensionFile()
			throws Exception {
		File file =new File("D:/runtime-EclipseApplication/FirstTest/test");
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResource resource = workspace.getRoot().getProject(projectName)
				.getFile(file.getName());

		Method isIResourceNeedToBeDetected = RLBuilder.class.getDeclaredMethod(
				"isIResourceNeedToBeDetected", IResource.class);
		isIResourceNeedToBeDetected.setAccessible(true);
		Assert.assertFalse((Boolean) isIResourceNeedToBeDetected.invoke(
				rlBuilder, resource));
	}

	@Test
	public void testIsIResourceNeedToBeDetectedWithUserDoNotWantToDetectedJavaFile()
			throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResource resource = workspace
				.getRoot()
				.getProject(projectName)
				.getFile("./srcTest/TestTwo.java");
		String path=UserDefinedMethodAnalyzer.getRobustaSettingXMLPath(projectName);
		
		File file=new File(getProjectPath(projectName));
		file.mkdir();
		
		robustaSettings = new RobustaSettings(path, projectName);
		robustaSettings.getProjectDetect("srcTest");
		robustaSettings.setProjectDetectAttribute("srcTest", "enable", false);
		robustaSettings.writeNewXMLFile(path);

		Method isIResourceNeedToBeDetected = RLBuilder.class.getDeclaredMethod(
				"isIResource" +
				"NeedToBeDetected", IResource.class);
		isIResourceNeedToBeDetected.setAccessible(true);
		Assert.assertFalse((Boolean) isIResourceNeedToBeDetected.invoke(
				rlBuilder, resource));

	}
	
	@Test
	public void testIsIResourceNeedToBeDetectedWithUserWantToDetectedJavaFile()
			throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResource resource = workspace
				.getRoot()
				.getProject(projectName)
				.getFile("./srcTest/TestTwo.java");
		String path=UserDefinedMethodAnalyzer.getRobustaSettingXMLPath(projectName);
	
		File file=new File(getProjectPath(projectName));
		file.mkdir();
		
		robustaSettings = new RobustaSettings(path, projectName);
		
		robustaSettings.setProjectDetectAttribute("srcTest", "enable", false);
		robustaSettings.writeNewXMLFile(path);

		Method isIResourceNeedToBeDetected = RLBuilder.class.getDeclaredMethod(
				"isIResource" +
				"NeedToBeDetected", IResource.class);
		isIResourceNeedToBeDetected.setAccessible(true);
		Assert.assertFalse((Boolean) isIResourceNeedToBeDetected.invoke(
				rlBuilder, resource));
		
		
		robustaSettings.setProjectDetectAttribute("srcTest", "enable", true);
		robustaSettings.writeNewXMLFile(path);

		isIResourceNeedToBeDetected = RLBuilder.class.getDeclaredMethod(
				"isIResource" +
				"NeedToBeDetected", IResource.class);
		isIResourceNeedToBeDetected.setAccessible(true);
		Assert.assertTrue((Boolean) isIResourceNeedToBeDetected.invoke(
				rlBuilder, resource));
			

	}
	private String getProjectPath(String projectName) {
		return ResourcesPlugin.getWorkspace().getRoot().getLocation()
				.toOSString()
				+ File.separator
				+ projectName;
				
	}
	

}
