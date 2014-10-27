package ntut.csie.robusta.agile.exception;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.jar.JarFile;

import ntut.csie.filemaker.JavaProjectMaker;
import ntut.csie.robusta.agile.exception.EnableRLAnnotation;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/*要先了解一下如何產生測試專案，才能繼續下去*/
public class EnableRLAnnotationTest {
	private EnableRLAnnotation enableRLAnnotation;

	JarFile answerJar = null;
	IWorkspace workspace;
	IWorkspaceRoot root;
	IProject project;
	IProject project2;

	private IJavaProject javaProj;
	private IJavaProject javaProj2;
	JavaProjectMaker javaProjectMaker;
	JavaProjectMaker javaProjectMaker2;

	@Before
	public void setUp() throws Exception {
		enableRLAnnotation = new EnableRLAnnotation();
		String projectname1 = "MyProject";
		javaProjectMaker = new JavaProjectMaker(projectname1);
		javaProjectMaker.setJREDefaultContainer();
		workspace = ResourcesPlugin.getWorkspace();
		root = workspace.getRoot();
		project = root.getProject(projectname1);
		javaProj = JavaCore.create(project);
		// 新增兩個不同空專案 
		String projectname2 = "MyProject2";
		javaProjectMaker2 = new JavaProjectMaker(projectname2);
		javaProjectMaker2.setJREDefaultContainer();
		workspace = ResourcesPlugin.getWorkspace();
		root = workspace.getRoot();
		project2 = root.getProject(projectname2);
		javaProj2 = JavaCore.create(project2);

	}

	@Test
	public void testExtractJarIdTrue() {
		String sampleFullJarId = "D:/Timelog-v2.4.2-x64/123/taipeitech.csie.robusta.agile.exception.txt";

		String answerJarId = "taipeitech.csie.robusta.agile.exception.txt";
		String testJarId = "";
		try {
			Method testExtraJarIdMethod = EnableRLAnnotation.class
					.getDeclaredMethod("extractJarId", String.class);
			testExtraJarIdMethod.setAccessible(true);
			testJarId = (String) testExtraJarIdMethod.invoke(
					enableRLAnnotation, sampleFullJarId);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		Assert.assertTrue(testJarId.equals(answerJarId));
	}

	@Test
	public void testExtractJarIdFalse() {
		String sampleFullJarId = "D:/Timelog-v2.4.2-x64/123";
		String answerJarId = "taipeitech.csie.robusta.agile.exception.txt";
		String testJarId = "";
		try {
			Method testExtraJarIdMethod = EnableRLAnnotation.class
					.getDeclaredMethod("extractJarId", String.class);
			testExtraJarIdMethod.setAccessible(true);
			testJarId = (String) testExtraJarIdMethod.invoke(
					enableRLAnnotation, sampleFullJarId);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		Assert.assertFalse(testJarId.equals(answerJarId));
	}

	@Test
	public void testCopyFileUsingFileStreams() {
		String workingSapaceDir = System.getProperty("user.dir");
		String filePath = workingSapaceDir + "/copytest/copy.txt";
		File sampleFile = new File(filePath);
		if (!sampleFile.exists()) {
			sampleFile.getParentFile().mkdirs();
			try {
				sampleFile.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		File destFileDir = new File(workingSapaceDir + "/copytest2/copy.txt");
		try {
			FileInputStream originFileInput = new FileInputStream(
					workingSapaceDir + "/copytest/copy.txt");
			Method testCopyFileUSingFileStreamMethod = EnableRLAnnotation.class
					.getDeclaredMethod("copyFileUsingFileStreams",
							InputStream.class, File.class);
			testCopyFileUSingFileStreamMethod.setAccessible(true);
			testCopyFileUSingFileStreamMethod.invoke(enableRLAnnotation,
					originFileInput, destFileDir);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		File searFile = new File(workingSapaceDir + "/copytest2/copy.txt");
		Assert.assertTrue(searFile.exists());
	}

	@Test
	public void testSetBuildPath() {
		IPath projPath = project.getLocation();
		File fileDest = new File(projPath + "/copytest2/copy.txt");
		if (!fileDest.exists()) {
			fileDest.getParentFile().mkdirs();
			try {
				fileDest.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		try {
			Method testSetBuildPathMethod = EnableRLAnnotation.class
					.getDeclaredMethod("setBuildPath", IProject.class,
							File.class);
			testSetBuildPathMethod.setAccessible(true);
			testSetBuildPathMethod
					.invoke(enableRLAnnotation, project, fileDest);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		File classPath = new File(projPath + "/.classpath");
		String line = "";
		BufferedReader bufferedReader;
		StringBuffer stringBuffer = null;
		try {
			FileReader fileReader = new FileReader(classPath);
			bufferedReader = new BufferedReader(fileReader);
			stringBuffer = new StringBuffer();

			while ((line = bufferedReader.readLine()) != null) {
				stringBuffer.append(line);
				stringBuffer.append("\n");
			}
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertTrue(stringBuffer.toString().contains("copy.txt"));
	}

	@Test
	public void testCheckExistInClassPath() {
		boolean statecheck = true;
		try {
			Method testCheckExistInClassPathMethod = EnableRLAnnotation.class
					.getDeclaredMethod("checkExistInClassPath",
							IJavaProject.class);
			testCheckExistInClassPathMethod.setAccessible(true);
			statecheck = (Boolean) testCheckExistInClassPathMethod.invoke(
					enableRLAnnotation, javaProj2);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		Assert.assertFalse(statecheck);
	}

	@After
	public void shutDown() throws CoreException {
		enableRLAnnotation = null;
		String workingDir = System.getProperty("user.dir");
		String filePath = workingDir + "/copytest/copy.txt";
		String filePath2 = workingDir + "/copytest2/copy.txt";
		deleteAll(filePath);
		deleteAll(filePath2);
		javaProjectMaker.deleteProject();
		javaProjectMaker2.deleteProject();
	}

	public void deleteAll(String path) {
		File fil = new File(path);
		fil.delete();
		File tmp = fil.getParentFile();
		tmp.delete();
	}
}
