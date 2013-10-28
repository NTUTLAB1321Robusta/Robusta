package ntut.csie.rleht.builder;

import java.io.File;
import java.lang.reflect.Method;

import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.filemaker.JavaProjectMaker;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RLBuilderTest {
	RLBuilder rlBuilder = new RLBuilder();
	JavaProjectMaker jpm;
	String testProjectName = "RLBuilderTestProject";

	@Before
	public void setUp() throws Exception {
		jpm = new JavaProjectMaker(testProjectName);
		jpm.setJREDefaultContainer();
		// Add src folder
		jpm.createSourceFolder();
		// Add test folder
		jpm.createSourceFolder("test", 0);
		// Add another non source folder
		jpm.createFolder("swt");
	}

	@After
	public void tearDown() throws Exception {
		jpm.deleteProject();
	}
	
	@Test
	public void testIsJavaFileWithNonIFile() throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(testProjectName);
		IResource resource = project.getFolder("src");
		Method isJavaFile = RLBuilder.class.getDeclaredMethod(
				"isJavaFile", IResource.class);
		isJavaFile.setAccessible(true);
		Assert.assertFalse((Boolean) isJavaFile.invoke(
				rlBuilder, resource));
	}
	
	@Test
	public void testIsJavaFileWithNonIFileWithTxtExtensionFile() throws Exception {
		File file =new File(testProjectName + "/src/test.txt");
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResource resource = workspace.getRoot().getProject(testProjectName)
				.getFile(file.getName());
		Method isJavaFile = RLBuilder.class.getDeclaredMethod(
				"isJavaFile", IResource.class);
		isJavaFile.setAccessible(true);
		Assert.assertFalse((Boolean) isJavaFile.invoke(
				rlBuilder, resource));
	}
	
	@Test
	public void testIsIResourceNeedToBeDetectedWithNoExtensionFile()
			throws Exception {
		File file =new File(testProjectName + "/src/test");
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IResource resource = workspace.getRoot().getProject(testProjectName)
				.getFile(file.getName());

		Method isJavaFile = RLBuilder.class.getDeclaredMethod(
				"isJavaFile", IResource.class);
		isJavaFile.setAccessible(true);
		Assert.assertFalse((Boolean) isJavaFile.invoke(
				rlBuilder, resource));
	}

	@Test
	public void testShouldGoInsideWithTheFolderNotIsTheSourceFolder()
			throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(testProjectName);
		
		// Load the RobustaSetting for the project
		Method loadRobustaSettingForProject = RLBuilder.class
				.getDeclaredMethod("loadRobustaSettingForProject", IProject.class);
		loadRobustaSettingForProject.setAccessible(true);
		loadRobustaSettingForProject.invoke(rlBuilder, project);

		Method shouldGoInside = RLBuilder.class.getDeclaredMethod(
				"shouldGoInInside", IResource.class);
		shouldGoInside.setAccessible(true);
		
		// Assert test folder is true, because it is source folder
		IResource resourceTest = project.getFolder("test");
		Assert.assertTrue((Boolean) shouldGoInside.invoke(rlBuilder,
				resourceTest));
		// swt is not a source folder
		IResource resourceSwt = project.getFolder("swt");
		Assert.assertFalse((Boolean) shouldGoInside.invoke(rlBuilder,
				resourceSwt));
	}

	@Test
	public void testShouldGoInsideWithSetNonVisitSourceFolder()
			throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IProject project = workspace.getRoot().getProject(testProjectName);
		
		// Add the src folder to ignore folder list to be detect bad smells
		RobustaSettings robustaSettings = new RobustaSettings(
				UserDefinedMethodAnalyzer
						.getRobustaSettingXMLPath(project),
						project);
		robustaSettings.setProjectDetectAttribute("src",
				RobustaSettings.ATTRIBUTE_ENABLE, false);
		robustaSettings.writeNewXMLFile(UserDefinedMethodAnalyzer
				.getRobustaSettingXMLPath(project));

		// Load the RobustaSetting for the project
		Method loadRobustaSettingForProject = RLBuilder.class
				.getDeclaredMethod("loadRobustaSettingForProject", IProject.class);
		loadRobustaSettingForProject.setAccessible(true);
		loadRobustaSettingForProject.invoke(rlBuilder, project);

		Method shouldGoInside = RLBuilder.class.getDeclaredMethod(
				"shouldGoInInside", IResource.class);
		shouldGoInside.setAccessible(true);
		// Assert test folder is true, because it is source folder
		IResource resourceFolder = project.getFolder("src");
		Assert.assertFalse((Boolean) shouldGoInside.invoke(rlBuilder,
				resourceFolder));
	}
}
