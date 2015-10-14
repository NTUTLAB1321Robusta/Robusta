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
