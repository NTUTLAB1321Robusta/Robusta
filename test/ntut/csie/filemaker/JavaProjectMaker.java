package ntut.csie.filemaker;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import ntut.csie.rleht.RLEHTPlugin;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;
import org.osgi.framework.Bundle;

/**
 * generate eclipse and java file
 * step 1. establish output folder{@link #createOutputFolder()}
 * step 2. set up JRE{@link #setJREDefaultContainer()}
 * step 3. generate java file(@link {@link #createJavaFile(String, String, String)}
 * @author charles
 *
 */
public class JavaProjectMaker {
	IProject _project;
	IJavaProject _javaproject;
	String sourceCodeFolderName;
	String testCodeFolderName;
	String projectName;
	String libraryPath;
	
	public static final int FUNTIONAL_CODE_FOLDER = 16;
	public static final int UNITTEST_CODE_FOLDER = 17;
	public static final String RL_PACKAGE_NAME = "ntut.csie.robusta.agile.exception";
	public static final String FOLDERNAME_LIB_JAR = "lib";
	public static final String FOLDERNAME_BIN_CLASS = "bin";
	public static final String FOLDERNAME_TEST = "test";
	public static final String FOLDERNAME_EXPERIMENT = "experiment";
	public static final String FOLDERNAME_SOURCE = "src";
	public static final String RL_LIBRARY_PATH = FOLDERNAME_LIB_JAR
			+ "/ntut.csie.robusta.agile.exception_1.0.0.jar";
	public static final String JAVA_FILE_EXTENSION = ".java";
	/**
	 * 
	 * generate Java project
	 * steps：<br />
	 * 1. new JavaProjectMaker(String) constructor<br />
	 * 2. setJREDefaultContainer() set JRE<br />
	 * 3. addAgileExceptionClasses() generate RL.jar<br />
	 * 4. addJarFromXXXToBuildPath(String) add specified jar file into Referenced Libraries<br />
	 * 5. createJavaFile(String, String, String) generate java file<br />
	 * @param projectName
	 * @throws CoreException
	 */
	public JavaProjectMaker(String projectName) throws CoreException {
		IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
		_project = workspace.getProject(projectName);
		// generate .project file
		_project.create(null);
		// open project in workspace(project's default state is closed in workspace)
		_project.open(null);
		_javaproject = JavaCore.create(_project);
		// generate .classpath file
		IProjectDescription projectDescription = _project.getDescription();
		projectDescription.setNatureIds(new String[] { JavaCore.NATURE_ID });
		_project.setDescription(projectDescription, null);
		_javaproject.setRawClasspath(new IClasspathEntry[0], null);
		
		this.projectName = projectName; 
		
		sourceCodeFolderName = FOLDERNAME_SOURCE;
		testCodeFolderName = FOLDERNAME_TEST;
		libraryPath = _project.getLocation().toFile().getAbsolutePath() + "/" + RL_LIBRARY_PATH;
	}
	
	public void createFolder(String folderName) throws CoreException {
		IFolder srcFolder = _project.getFolder(folderName);
		srcFolder.create(false, true, null);
	}

	/**
	 * create output folder named "bin"
	 * @throws CoreException
	 */
	public void createOutputFolder() throws CoreException {
		createOutputFolder(FOLDERNAME_BIN_CLASS);
	}
	
	
	public void createOutputFolder(String folderName) throws CoreException {
		IFolder outputFolder = _project.getFolder(folderName);
		outputFolder.create(false, true, null);
		IPath outputLocation = outputFolder.getFullPath();
		_javaproject.setOutputLocation(outputLocation, null);
	}
	
	/**
	 * create output folder named "src"
	 * @throws CoreException
	 */
	public void createSourceFolder() throws CoreException {
		createSourceFolder(sourceCodeFolderName, 0);
	}
	
	public void createSourceFolder(String folderName, int folderType) throws CoreException {
		IFolder sourceFolder = _project.getFolder(folderName);
		sourceFolder.create(false, true, null);
		IPackageFragmentRoot root = _javaproject.getPackageFragmentRoot(sourceFolder);
		// original content in .class file
		IClasspathEntry[] existedEntries = _javaproject.getRawClasspath();
		// prepare one more extra space 
		IClasspathEntry[] extendedEntries = new IClasspathEntry[existedEntries.length + 1];
		// copy data from existedEntries to extendedEntries
		System.arraycopy(existedEntries, 0, extendedEntries, 0, existedEntries.length);
		// add extra folder in one more extra space
		extendedEntries[existedEntries.length] = JavaCore.newSourceEntry(root.getPath());
		_javaproject.setRawClasspath(extendedEntries, null);
		
		recordFolderNameByType(folderName, folderType);
	}

	private void recordFolderNameByType(String folderName, int folderType) {
		if(folderType == FUNTIONAL_CODE_FOLDER) {
			sourceCodeFolderName = folderName;
		} else if(folderType == UNITTEST_CODE_FOLDER) {
			testCodeFolderName = folderName;
		}
	}

	/**
	 * set JRE for Compiling(default JRE file)。
	 * @throws JavaModelException
	 */
	public void setJREDefaultContainer() throws JavaModelException {
		addClasspathEntryToBuildPath(JavaRuntime.getDefaultJREContainerEntry(), null);
	}
	//packageName could be in format like a.b.c.d
	public IPackageFragment createPackage(String packageName) throws CoreException {
		IFolder sourceFolder = _project.getFolder(sourceCodeFolderName);
		if(!sourceFolder.exists()) {
			createSourceFolder();
		}
		IPackageFragmentRoot root = _javaproject.getPackageFragmentRoot(sourceFolder);
		return root.createPackageFragment(packageName, false, null);
	}
	
	public void createJavaFile(String packageName, String className, String content) throws CoreException {
		createDotJavaFile(packageName, className, content, FUNTIONAL_CODE_FOLDER);
	}
	
	public void createTestCase(String packageName, String className, String content) throws CoreException {
		createDotJavaFile(packageName, className, content, UNITTEST_CODE_FOLDER);
	}

	/**
	 * 
	 * @param type
	 *            FUNTIONAL_CODE | UNITTEST_CODE <br />(please use 
	 *            {@link JavaProjectMaker#FUNTIONAL_CODE_FOLDER} and
	 *            {@link JavaProjectMaker#UNITTEST_CODE_FOLDER} to specify)
	 * @throws CoreException
	 */
	private void createDotJavaFile(String packageName, String className, String content, int type) throws CoreException {
		String folderName = "";
		if(type == FUNTIONAL_CODE_FOLDER) {
			folderName = sourceCodeFolderName;
		} else if (type == UNITTEST_CODE_FOLDER) {
			folderName = testCodeFolderName;
		}
		IPackageFragmentRoot root = _javaproject.getPackageFragmentRoot(folderName);
		IPackageFragment ipf;
		if(!root.exists()) {
			ipf = createPackage(packageName);
		} else {
			root.open(null);
			ipf = root.getPackageFragment(packageName);
		}
		
		if(!className.endsWith(JAVA_FILE_EXTENSION)) {
			className += JAVA_FILE_EXTENSION;
		}
		
		ipf.createCompilationUnit(className, content, false, null);
	}
	
	
	public void addJarFromProjectToBuildPath(String jarFilePath) throws IOException, JavaModelException {
		Path jarPath = null;
		
		/* use Bundle and PLUGIN-ID to find out jar file path */
		Bundle bundle = Platform.getBundle(RLEHTPlugin.PLUGIN_ID);
		URL jarURL = bundle.getEntry(jarFilePath);
		URL localJarURL = FileLocator.toFileURL(jarURL);
		jarPath = new Path(localJarURL.getPath());
		
		/* add jar path to .classpath file */
		addClasspathEntryToBuildPath(JavaCore.newLibraryEntry(jarPath, null, null), null);
	}

	/**
	 * Add class path entry to inner project's class path
	 */
	public void addClasspathEntryToBuildPath(IClasspathEntry classpathEntry,
			IProgressMonitor progressMonitor) throws JavaModelException {
		IClasspathEntry[] existedEntries = _javaproject.getRawClasspath();
		IClasspathEntry[] extendedEntries = new IClasspathEntry[existedEntries.length + 1];
		System.arraycopy(existedEntries, 0, extendedEntries, 0,
				existedEntries.length);
		extendedEntries[existedEntries.length] = classpathEntry;
		_javaproject.setRawClasspath(extendedEntries, progressMonitor);
	}

	public void addJarFromTestProjectToBuildPath(String jarFilePath) throws IOException, JavaModelException {
		File libFile = new File(_project.getLocation().toFile().getAbsolutePath() + jarFilePath);
		Path jarPath = new Path(libFile.getPath());
		
		/* add jar path to .classpath file */
		addClasspathEntryToBuildPath(JavaCore.newLibraryEntry(jarPath, null, null), null);
		
	}
	
	public void packageAgileExceptionClassesToJarIntoLibFolder(String libFoldername, String binFoldername) throws CoreException {
		createFolder(libFoldername);
		File libFile = new File(libraryPath);
		JarFileMaker jarFileMaker = new JarFileMaker();
		jarFileMaker.createJarFile(libFile, new File(binFoldername), RL_PACKAGE_NAME);
	}
	
	public synchronized void deleteProject() throws CoreException {
		_project.delete(true, null);
    }
	
	public String getProjectName() {
		return projectName;
	}
}
