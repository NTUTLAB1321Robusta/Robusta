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

public class EnableRLAnnotationTest {
	private EnableRLAnnotation enableRLAnnotation;
	JarFile answerJar = null;
	IWorkspace workspace;
	IWorkspaceRoot root;
	IProject project;

	private IJavaProject javaProj;
	JavaProjectMaker javaProjectMaker;
	@Before
	public void setUp() throws Exception {
		enableRLAnnotation = new EnableRLAnnotation();
		String projectname = "MyProject";
		javaProjectMaker = new JavaProjectMaker(projectname);
		javaProjectMaker.setJREDefaultContainer();
		workspace = ResourcesPlugin.getWorkspace();
		root = workspace.getRoot();
		project = root.getProject(projectname);
		javaProj = JavaCore.create(project);
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
			throw new RuntimeException(ex);	
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
			throw new RuntimeException(ex);	
			}
		Assert.assertFalse(testJarId.equals(answerJarId));
	}

	@Test
	public void testCopyFileUsingFileStreams() {
		String workingSapaceDir = System.getProperty("user.dir");
		File sampleFile = new File(workingSapaceDir + "/copytest/copy.txt");
		if (!sampleFile.exists()) {
			sampleFile.getParentFile().mkdirs();
			try {
				sampleFile.createNewFile();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		File destFileDir = new File(workingSapaceDir + "/copytest2/copy.txt");
		FileInputStream originFileInput =null;
		try {
			originFileInput = new FileInputStream(
					workingSapaceDir + "/copytest/copy.txt");
			Method testCopyFileUSingFileStreamMethod = EnableRLAnnotation.class
					.getDeclaredMethod("copyFileUsingFileStreams",
							InputStream.class, File.class);
			testCopyFileUSingFileStreamMethod.setAccessible(true);
			testCopyFileUSingFileStreamMethod.invoke(enableRLAnnotation,
					originFileInput, destFileDir);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}finally{
			try {
				originFileInput.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
				throw new RuntimeException(e);
			}
		}
		try {
			Method testSetBuildPathMethod = EnableRLAnnotation.class
					.getDeclaredMethod("setBuildPath", IProject.class,
							File.class);
			testSetBuildPathMethod.setAccessible(true);
			testSetBuildPathMethod.invoke(enableRLAnnotation, project, fileDest);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
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
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
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
			statecheck = (Boolean) testCheckExistInClassPathMethod.invoke(enableRLAnnotation, javaProj);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
		Assert.assertFalse(statecheck);
	}

	@After
	public void TearDown() throws CoreException {
		enableRLAnnotation = null;
		String workingSapaceDir = System.getProperty("user.dir");
		String filePath = workingSapaceDir + "/copytest/copy.txt";
		String filePath2 = workingSapaceDir + "/copytest2/copy.txt";
		deleteAll(filePath);
		deleteAll(filePath2);
		javaProjectMaker.deleteProject();
	}

	public void deleteAll(String path) {
		File fil = new File(path);
		fil.delete();
		File tmp = fil.getParentFile();
		tmp.delete();
	}
}
