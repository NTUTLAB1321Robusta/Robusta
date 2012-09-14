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
 * 產生專案以及專案中的java檔。
 * step 1. 建立輸出資料夾{@link #createOutputFolder()}
 * step 2. 設定JRE{@link #setJREDefaultContainer()}
 * step 3. 直接產生java檔(@link {@link #createJavaFile(String, String, String)}
 * @author charles
 *
 */
public class JavaProjectMaker {
	/** 要被產生的專案 */
	IProject _project;
	/** 想被產生的專案是Java Project */
	IJavaProject _javaproject;
	/** functional code的資料夾名稱 */
	String sourceCodeFolderName;
	/** test code的資料夾名稱 */
	String testCodeFolderName;
	/** 專案名稱 */
	String projectName;
	/** 儲存RL.jar的路徑 */
	String libraryPath;
	
	/** 存放functional code的資料夾*/
	public static final int FUNTIONAL_CODE_FOLDER = 16;
	/** 存放unit test code的資料夾 */
	public static final int UNITTEST_CODE_FOLDER = 17;
	/** 定義強健度等級的class位於哪個Package */
	public static final String RL_PACKAGE_NAME = "ntut.csie.robusta.agile.exception";
	
	/** 專案存放Jar檔的資料夾名稱 */
	public static final String FOLDERNAME_LIB_JAR = "lib";
	/** 專案存放Class檔的資料夾名稱 */
	public static final String FOLDERNAME_BIN_CLASS = "bin";
	/** 測試程式碼資料夾的名稱 */
	public static final String FOLDERNAME_TEST = "test";
	/** 功能性程式碼的資料夾名稱 */
	public static final String FOLDERNAME_SOURCE = "src";
	/** 取得RL.jar的路徑 */
	public static final String RL_LIBRARY_PATH = FOLDERNAME_LIB_JAR
			+ "/ntut.csie.robusta.agile.exception_1.0.0.jar";
	/** java 副檔名 */
	public static final String JAVA_FILE_EXTENSION = ".java";
	/**
	 * 產生一個Java專案。
	 * 用法：<br />
	 * 1. new JavaProjectMaker(String)建構<br />
	 * 2. setJREDefaultContainer()設定JRE<br />
	 * 3. addAgileExceptionClasses() 產生RL.jar<br />
	 * 4. addJarFromXXXToBuildPath(String)將指定的jar檔加入至Referenced Libraries<br />
	 * 5. createJavaFile(String, String, String)產生java檔<br />
	 * 其他關於產生資料夾，產生package的方法，都可以不用呼叫。
	 * @param projectName
	 * @throws CoreException
	 */
	public JavaProjectMaker(String projectName) throws CoreException {
		// 取得workspace
		IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
		// 設定專案名稱
		_project = workspace.getProject(projectName);
		// 產生.project
		_project.create(null);
		// 將專案打開(預設在workspace中是關閉)
		_project.open(null);
		_javaproject = JavaCore.create(_project);
		// 產生.classpath
		IProjectDescription projectDescription = _project.getDescription();
		projectDescription.setNatureIds(new String[] { JavaCore.NATURE_ID });
		_project.setDescription(projectDescription, null);
		_javaproject.setRawClasspath(new IClasspathEntry[0], null);
		
		this.projectName = projectName; 
		
		/* 預設資料夾是src & test */
		sourceCodeFolderName = FOLDERNAME_SOURCE;
		testCodeFolderName = FOLDERNAME_TEST;
		libraryPath = _project.getLocation().toFile().getAbsolutePath() + "/" + RL_LIBRARY_PATH;
	}
	
	/**
	 * 產生資料夾
	 * @param folderName
	 * @throws CoreException
	 */
	public void createFolder(String folderName) throws CoreException {
		IFolder srcFolder = _project.getFolder(folderName);
		srcFolder.create(false, true, null);
	}

	/**
	 * 等價於createOutputFolder("bin")
	 * @throws CoreException
	 */
	public void createOutputFolder() throws CoreException {
		createOutputFolder(FOLDERNAME_BIN_CLASS);
	}
	
	/**
	 * 建立binary folder，通常就是存放.class檔的資料夾。
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
	 * 等價於createSourceFolder(&quot;src&quot;);
	 * @throws CoreException
	 */
	public void createSourceFolder() throws CoreException {
		createSourceFolder(sourceCodeFolderName, 0);
	}
	
	/**
	 * 在專案中產生資料夾的時候，可以設定資料夾為Source(source code folder)。
	 * 這個method，就是在新增一個source code folder。
	 * @param folderName 使用者想要產生的資料夾名稱。如果傳入null，則預設資料夾名字為src。
	 * @param folderType 使用者想要產生的資料夾是用來存放functional code或是test code，可以在這裡設定type。
	 * @throws CoreException
	 */
	public void createSourceFolder(String folderName, int folderType) throws CoreException {
		IFolder sourceFolder = _project.getFolder(folderName);
		sourceFolder.create(false, true, null);
		IPackageFragmentRoot root = _javaproject.getPackageFragmentRoot(sourceFolder);
		// .class檔案中，已經存在的內容
		IClasspathEntry[] existedEntries = _javaproject.getRawClasspath();
		// 準備寫入.class檔的內容
		IClasspathEntry[] extendedEntries = new IClasspathEntry[existedEntries.length + 1];
		// 把舊的entry複製起來
		System.arraycopy(existedEntries, 0, extendedEntries, 0, existedEntries.length);
		// 附加上使用者指定的source資料夾
		extendedEntries[existedEntries.length] = JavaCore.newSourceEntry(root.getPath());
		_javaproject.setRawClasspath(extendedEntries, null);
		
		recordFolderNameByType(folderName, folderType);
	}

	/**
	 * 如果使用者產生的資料夾是存放source code/test code的資料夾，名字會被記錄起來
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
	 * 設定Compile用的JRE(選擇預設的JRE)。
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
	 * 產生package。可以直接用a.b.c產生多層資料夾，即a資料夾下面有b資料夾，然後b資料夾包含c資料夾。
	 * @param packageName 可以直接輸入a.b.c的格式。
	 * @throws CoreException 
	 */
	public IPackageFragment createPackage(String packageName) throws CoreException {
		// 如果source資料夾不存在，就要先產生一個
		IFolder sourceFolder = _project.getFolder(sourceCodeFolderName);
		if(!sourceFolder.exists()) {
			createSourceFolder();
		}
		IPackageFragmentRoot root = _javaproject.getPackageFragmentRoot(sourceFolder);
		return root.createPackageFragment(packageName, false, null);
	}
	
	/**
	 * 產生一個java檔(class)。
	 * @param packageName 你想把java檔放在哪個package下面。可以直接輸入a.b.c.d的格式。
	 * @param className 這個java檔的檔名(class名字)。
	 * @param content java檔的內容。
	 * @throws CoreException 
	 */
	public void createJavaFile(String packageName, String className, String content) throws CoreException {
		createDotJavaFile(packageName, className, content, FUNTIONAL_CODE_FOLDER);
	}
	
	/**
	 * 產生一個JUnit測試範例檔(副檔名.java)。
	 * @param packageName 你想把java檔放在哪個package下面。可以直接輸入a.b.c.d的格式。
	 * @param className 這個test case檔的檔名(xxx.java)。
	 * @param content test case檔的內容。
	 * @throws CoreException 
	 */
	public void createTestCase(String packageName, String className, String content) throws CoreException {
		// 產生一個JUnit test case
		createDotJavaFile(packageName, className, content, UNITTEST_CODE_FOLDER);
	}

	/**
	 * 產生一個副檔名為java的檔案
	 * 
	 * @param packageName 可以直接輸入a.b.c.d的格式
	 * @param className 也是java檔名
	 * @param content java檔案內容
	 * @param type
	 *            FUNTIONAL_CODE | UNITTEST_CODE <br />(請用
	 *            {@link JavaProjectMaker#FUNTIONAL_CODE_FOLDER}和
	 *            {@link JavaProjectMaker#UNITTEST_CODE_FOLDER}指定)
	 * @throws CoreException
	 */
	private void createDotJavaFile(String packageName, String className, String content, int type) throws CoreException {
		String folderName = "";
		// 放在src資料夾
		if(type == FUNTIONAL_CODE_FOLDER) {
			folderName = sourceCodeFolderName;
		// 放在test資料夾
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
		
		/* 判斷傳進來的檔名有沒有副檔名(.java)，如果沒有就幫他加上去 */
		if(!className.endsWith(JAVA_FILE_EXTENSION)) {
			className += JAVA_FILE_EXTENSION;
		}
		
		// 產生java檔
		ipf.createCompilationUnit(className, content, false, null);
	}
	
	/**
	 * 從Plug-in專案中的jar檔，加入為測試專案的Referenced Libraries
	 * @param jarFilePath jar檔在專案下的相對路徑
	 * @throws IOException 
	 * @throws JavaModelException 
	 */
	public void addJarFromProjectToBuildPath(String jarFilePath) throws IOException, JavaModelException {
		// jar檔的路徑
		Path jarPath = null;
		
		/* 用Bundle和PLUGIN-ID找出jar檔路徑 */
		Bundle bundle = Platform.getBundle(RLEHTPlugin.PLUGIN_ID);
		URL jarURL = bundle.getEntry(jarFilePath);
		URL localJarURL = FileLocator.toFileURL(jarURL);
		jarPath = new Path(localJarURL.getPath());
		
		/* 將找出來的jar檔路徑，寫入.classpath */
		IClasspathEntry[] existedEntries = _javaproject.getRawClasspath();
		IClasspathEntry[] extendedEntries = new IClasspathEntry[existedEntries.length + 1];
		System.arraycopy(existedEntries, 0, extendedEntries, 0, existedEntries.length);
		extendedEntries[existedEntries.length] = JavaCore.newLibraryEntry(jarPath, null, null);
		_javaproject.setRawClasspath(extendedEntries, null);
	}
	
	/**
	 * 從測試專案中的jar檔，加入為測試專案的Referenced Libraries
	 * @param jarFilePath jar檔在專案下的相對路徑
	 * @throws IOException 
	 * @throws JavaModelException 
	 */
	public void addJarFromTestProjectToBuildPath(String jarFilePath) throws IOException, JavaModelException {
		File libFile = new File(_project.getLocation().toFile().getAbsolutePath() + jarFilePath);
		// jar檔的路徑
		Path jarPath = new Path(libFile.getPath());
		
		/* 將找出來的jar檔路徑，寫入.classpath */
		IClasspathEntry[] existedEntries = _javaproject.getRawClasspath();
		IClasspathEntry[] extendedEntries = new IClasspathEntry[existedEntries.length + 1];
		System.arraycopy(existedEntries, 0, extendedEntries, 0, existedEntries.length);
		extendedEntries[existedEntries.length] = JavaCore.newLibraryEntry(jarPath, null, null);
		_javaproject.setRawClasspath(extendedEntries, null);
		
	}
	
	/**
	 * 將定義強健度等級註記的Class檔，打包成Jar並放到待測專案的lib資料夾中
	 * @param libFoldername 存放.jar檔的資料夾
	 * @param binFoldername 存放.class檔的資料夾
	 * @throws CoreException
	 */
	public void packAgileExceptionClasses2JarIntoLibFolder(String libFoldername, String binFoldername) throws CoreException {
		createFolder(libFoldername);
		File libFile = new File(libraryPath);
		JarFileMaker jarFileMaker = new JarFileMaker();
		jarFileMaker.createJarFile(libFile, new File(binFoldername), RL_PACKAGE_NAME);
	}
	
	/**
	 * 刪除由建構元建立的專案
	 * @throws CoreException 
	 */
	public synchronized void deleteProject() throws CoreException {
		_project.delete(true, null);
    }
	
	/**
	 * 取得專案名稱
	 * @return projectName
	 */
	public String getProjectName() {
		return projectName;
	}
}
