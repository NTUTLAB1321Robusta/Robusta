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
 * ���ͱM�ץH�αM�פ���java�ɡC
 * step 1. �إ߿�X��Ƨ�{@link #createOutputFolder()}
 * step 2. �]�wJRE{@link #setJREDefaultContainer()}
 * step 3. ��������java��(@link {@link #createJavaFile(String, String, String)}
 * @author charles
 *
 */
public class JavaProjectMaker {
	/** �n�Q���ͪ��M�� */
	IProject _project;
	/** �Q�Q���ͪ��M�׬OJava Project */
	IJavaProject _javaproject;
	/** functional code����Ƨ��W�� */
	String sourceCodeFolderName;
	/** test code����Ƨ��W�� */
	String testCodeFolderName;
	/** �M�צW�� */
	String projectName;
	/** �x�sRL.jar�����| */
	String libraryPath;
	
	/** �s��functional code����Ƨ�*/
	public static final int FUNTIONAL_CODE_FOLDER = 16;
	/** �s��unit test code����Ƨ� */
	public static final int UNITTEST_CODE_FOLDER = 17;
	/** ���oRL.jar�����| */
	public static final String LIBRARY_PATH = "lib/RL.jar";
	
	/**
	 * ���ͤ@��Java�M�סC
	 * �Ϊk�G<br />
	 * 1. new JavaProjectMaker(String)�غc<br />
	 * 2. setJREDefaultContainer()�]�wJRE<br />
	 * 3. addAgileExceptionClasses() ����RL.jar<br />
	 * 4. addJarFromXXXToBuildPath(String)�N���w��jar�ɥ[�J��Referenced Libraries<br />
	 * 5. createJavaFile(String, String, String)����java��<br />
	 * ��L���󲣥͸�Ƨ��A����package����k�A���i�H���ΩI�s�C
	 * @param projectName
	 * @throws CoreException
	 */
	public JavaProjectMaker(String projectName) throws CoreException {
		// ���oworkspace
		IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
		// �]�w�M�צW��
		_project = workspace.getProject(projectName);
		// ����.project
		_project.create(null);
		// �N�M�ץ��}(�w�]�bworkspace���O����)
		_project.open(null);
		_javaproject = JavaCore.create(_project);
		// ����.classpath
		IProjectDescription projectDescription = _project.getDescription();
		projectDescription.setNatureIds(new String[] { JavaCore.NATURE_ID });
		_project.setDescription(projectDescription, null);
		_javaproject.setRawClasspath(new IClasspathEntry[0], null);
		
		this.projectName = projectName; 
		
		/* �w�]��Ƨ��Osrc & test */
		sourceCodeFolderName = "src";
		testCodeFolderName = "test";
		libraryPath = _project.getLocation().toFile().getAbsolutePath() + "/lib/RL.jar";
	}
	
	/**
	 * ���͸�Ƨ�
	 * @param folderName
	 * @throws CoreException
	 */
	public void createFolder(String folderName) throws CoreException {
		IFolder srcFolder = _project.getFolder(folderName);
		srcFolder.create(false, true, null);
	}

	/**
	 * ������createOutputFolder("bin")
	 * @throws CoreException
	 */
	public void createOutputFolder() throws CoreException {
		createOutputFolder("bin");
	}
	
	/**
	 * �إ�binary folder�A�q�`�N�O�s��.class�ɪ���Ƨ��C
	 * @param folderName
	 * @throws CoreException
	 */
	public void createOutputFolder(String folderName) throws CoreException {
		IFolder outputFolder = _project.getFolder(folderName);
		outputFolder.create(false, true, null);
		IPath outputLocation = outputFolder.getFullPath();
		_javaproject.setOutputLocation(outputLocation, null);
	}
	
	/**
	 * ������createSourceFolder(&quot;src&quot;);
	 * @throws CoreException
	 */
	public void createSourceFolder() throws CoreException {
		createSourceFolder(sourceCodeFolderName, 0);
	}
	
	/**
	 * �b�M�פ����͸�Ƨ����ɭԡA�i�H�]�w��Ƨ���Source(source code folder)�C
	 * �o��method�A�N�O�b�s�W�@��source code folder�C
	 * @param folderName �ϥΪ̷Q�n���ͪ���Ƨ��W�١C�p�G�ǤJnull�A�h�w�]��Ƨ��W�r��src�C
	 * @param folderType �ϥΪ̷Q�n���ͪ���Ƨ��O�ΨӦs��functional code�άOtest code�A�i�H�b�o�̳]�wtype�C
	 * @throws CoreException
	 */
	public void createSourceFolder(String folderName, int folderType) throws CoreException {
		IFolder sourceFolder = _project.getFolder(folderName);
		sourceFolder.create(false, true, null);
		IPackageFragmentRoot root = _javaproject.getPackageFragmentRoot(sourceFolder);
		// .class�ɮפ��A�w�g�s�b�����e
		IClasspathEntry[] existedEntries = _javaproject.getRawClasspath();
		// �ǳƼg�J.class�ɪ����e
		IClasspathEntry[] extendedEntries = new IClasspathEntry[existedEntries.length + 1];
		// ���ª�entry�ƻs�_��
		System.arraycopy(existedEntries, 0, extendedEntries, 0, existedEntries.length);
		// ���[�W�ϥΪ̫��w��source��Ƨ�
		extendedEntries[existedEntries.length] = JavaCore.newSourceEntry(root.getPath());
		_javaproject.setRawClasspath(extendedEntries, null);
		
		recordFolderNameByType(folderName, folderType);
	}

	/**
	 * �p�G�ϥΪ̲��ͪ���Ƨ��O�s��source code/test code����Ƨ��A�W�r�|�Q�O���_��
	 * @param folderName
	 * @param folderType
	 */
	private void recordFolderNameByType(String folderName, int folderType) {
		if(folderType == FUNTIONAL_CODE_FOLDER) {
			sourceCodeFolderName = folderName;
		} else if(folderType == UNITTEST_CODE_FOLDER) {
			testCodeFolderName = folderName;
		}
	}

	/**
	 * �]�wCompile�Ϊ�JRE(��ܹw�]��JRE)�C
	 * @throws JavaModelException
	 */
	public void setJREDefaultContainer() throws JavaModelException {
		IClasspathEntry[] existedEntries = _javaproject.getRawClasspath();
		IClasspathEntry[] extendedEntries = new IClasspathEntry[existedEntries.length + 1];
		System.arraycopy(existedEntries, 0, extendedEntries, 0, existedEntries.length);
		extendedEntries[existedEntries.length] = JavaRuntime.getDefaultJREContainerEntry();
		_javaproject.setRawClasspath(extendedEntries, null);
	}
	
	/**
	 * ����package�C�i�H������a.b.c���ͦh�h��Ƨ��A�Ya��Ƨ��U����b��Ƨ��A�M��b��Ƨ��]�tc��Ƨ��C
	 * @param packageName �i�H������Ja.b.c���榡�C
	 * @throws CoreException 
	 */
	public IPackageFragment createPackage(String packageName) throws CoreException {
		// �p�Gsource��Ƨ����s�b�A�N�n�����ͤ@��
		IFolder sourceFolder = _project.getFolder(sourceCodeFolderName);
		if(!sourceFolder.exists()) {
			createSourceFolder();
		}
		IPackageFragmentRoot root = _javaproject.getPackageFragmentRoot(sourceFolder);
		return root.createPackageFragment(packageName, false, null);
	}
	
	/**
	 * ���ͤ@��java��(class)�C
	 * @param packageName �A�Q��java�ɩ�b����package�U���C�i�H������Ja.b.c.d���榡�C
	 * @param className �o��java�ɪ��ɦW(class�W�r)�C
	 * @param content java�ɪ����e�C
	 * @throws CoreException 
	 */
	public void createJavaFile(String packageName, String className, String content) throws CoreException {
		createDotJavaFile(packageName, className, content, FUNTIONAL_CODE_FOLDER);
	}
	
	/**
	 * ���ͤ@��JUnit���սd����(���ɦW.java)�C
	 * @param packageName �A�Q��java�ɩ�b����package�U���C�i�H������Ja.b.c.d���榡�C
	 * @param className �o��test case�ɪ��ɦW(xxx.java)�C
	 * @param content test case�ɪ����e�C
	 * @throws CoreException 
	 */
	public void createTestCase(String packageName, String className, String content) throws CoreException {
		// ���ͤ@��JUnit test case
		createDotJavaFile(packageName, className, content, UNITTEST_CODE_FOLDER);
	}

	/**
	 * ���ͤ@�Ӱ��ɦW��java���ɮ�
	 * 
	 * @param packageName �i�H������Ja.b.c.d���榡
	 * @param className �]�Ojava�ɦW
	 * @param content java�ɮפ��e
	 * @param type
	 *            FUNTIONAL_CODE | UNITTEST_CODE <br />(�Х�
	 *            {@link JavaProjectMaker#FUNTIONAL_CODE_FOLDER}�M
	 *            {@link JavaProjectMaker#UNITTEST_CODE_FOLDER}���w)
	 * @throws CoreException
	 */
	private void createDotJavaFile(String packageName, String className, String content, int type) throws CoreException {
		String folderName = "";
		// ��bsrc��Ƨ�
		if(type == FUNTIONAL_CODE_FOLDER) {
			folderName = sourceCodeFolderName;
		// ��btest��Ƨ�
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
		
		/* �P�_�Ƕi�Ӫ��ɦW���S�����ɦW(.java)�A�p�G�S���N���L�[�W�h */
		final String javaExtension = ".java";
		if(!className.endsWith(javaExtension)) {
			className += javaExtension;
		}
		
		// ����java��
		ipf.createCompilationUnit(className, content, false, null);
	}
	
	/**
	 * �qPlug-in�M�פ���jar�ɡA�[�J�����ձM�ת�Referenced Libraries
	 * @param jarFilePath jar�ɦb�M�פU���۹���|
	 * @throws IOException 
	 * @throws JavaModelException 
	 */
	public void addJarFromProjectToBuildPath(String jarFilePath) throws IOException, JavaModelException {
		// jar�ɪ����|
		Path jarPath = null;
		
		/* ��Bundle�MPLUGIN-ID��Xjar�ɸ��| */
		Bundle bundle = Platform.getBundle(RLEHTPlugin.PLUGIN_ID);
		URL jarURL = bundle.getEntry(jarFilePath);
		URL localJarURL = FileLocator.toFileURL(jarURL);
		jarPath = new Path(localJarURL.getPath());
		
		/* �N��X�Ӫ�jar�ɸ��|�A�g�J.classpath */
		IClasspathEntry[] existedEntries = _javaproject.getRawClasspath();
		IClasspathEntry[] extendedEntries = new IClasspathEntry[existedEntries.length + 1];
		System.arraycopy(existedEntries, 0, extendedEntries, 0, existedEntries.length);
		extendedEntries[existedEntries.length] = JavaCore.newLibraryEntry(jarPath, null, null);
		_javaproject.setRawClasspath(extendedEntries, null);
	}
	
	/**
	 * �q���ձM�פ���jar�ɡA�[�J�����ձM�ת�Referenced Libraries
	 * @param jarFilePath jar�ɦb�M�פU���۹���|
	 * @throws IOException 
	 * @throws JavaModelException 
	 */
	public void addJarFromTestProjectToBuildPath(String jarFilePath) throws IOException, JavaModelException {
		File libFile = new File(_project.getLocation().toFile().getAbsolutePath() + jarFilePath);
		// jar�ɪ����|
		Path jarPath = new Path(libFile.getPath());
		
		/* �N��X�Ӫ�jar�ɸ��|�A�g�J.classpath */
		IClasspathEntry[] existedEntries = _javaproject.getRawClasspath();
		IClasspathEntry[] extendedEntries = new IClasspathEntry[existedEntries.length + 1];
		System.arraycopy(existedEntries, 0, extendedEntries, 0, existedEntries.length);
		extendedEntries[existedEntries.length] = JavaCore.newLibraryEntry(jarPath, null, null);
		_javaproject.setRawClasspath(extendedEntries, null);
		
	}
	
	/**
	 * �b���ձM�פ����ͩw�q�j���׵��ŵ��O�����O
	 * @param libPath ���[�J��lib���x�s��m
	 * @throws CoreException
	 * @throws IOException 
	 */
	public void addAgileExceptionClasses() throws CoreException {
		createFolder("lib");
		File libFile = new File(libraryPath);
		JarFileMaker jarFileMaker = new JarFileMaker();
		jarFileMaker.createJarFile(libFile, new File("bin").listFiles());
	}
	
	/**
	 * �R���ѫغc���إߪ��M��
	 * @param projectName
	 * @throws CoreException 
	 */
	public synchronized void deleteProject() throws CoreException {
		_project.delete(true, null);
    }
	
	/**
	 * ���o�M�צW��
	 * @return projectName
	 */
	public String getProjectName() {
		return projectName;
	}
}
