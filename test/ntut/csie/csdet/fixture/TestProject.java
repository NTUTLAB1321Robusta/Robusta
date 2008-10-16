package ntut.csie.csdet.fixture;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

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
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchConstants;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.core.search.SearchPattern;
import org.eclipse.jdt.core.search.TypeNameRequestor;
import org.eclipse.jdt.launching.JavaRuntime;
import org.osgi.framework.Bundle;

public class TestProject {
	public IProject project;

	public IJavaProject javaProject;

	private IPackageFragmentRoot sourceFolder;

	public TestProject(){
		this("Project-1");
	}
	
	public TestProject(String name){
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		project = root.getProject(name);

		try {
			project.create(null);
			project.open(null);
			javaProject = JavaCore.create(project);
			IFolder binFolder = createBinFolder();
			setJavaNature();
			javaProject.setRawClasspath(new IClasspathEntry[0], null);
			createOutputFolder(binFolder);
			addSystemLibraries();
		} catch (CoreException e) {
			e.printStackTrace();
		}		
	}

	public IProject getProject() {
		return project;
	}

	public IJavaProject getJavaProject() {
		return javaProject;
	}

	public IType getIType(String name){		
		try {
			return javaProject.findType(name);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}return null;
	}
	
	public IType getIType(String pack,String name){		
		try {
			return javaProject.findType(pack,name);
		} catch (JavaModelException e) {
			e.printStackTrace();
		}return null;
	}

	public void addJar(String plugin,String jar) throws MalformedURLException,
			IOException, JavaModelException {
		Path result = findFileInPlugin(plugin,jar);
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaCore.newLibraryEntry(result, null,
				null);
		javaProject.setRawClasspath(newEntries, null);
	}

	public IPackageFragment createPackage(String name) throws CoreException {
		return createPackage(sourceFolder,name);
	}
	
	public IPackageFragment createPackage(IPackageFragmentRoot folder,String name) throws CoreException {
		if (folder == null)
			folder = createSourceFolder("src");
		return folder.createPackageFragment(name, false, null);
	}
	
	public IType createType(IPackageFragment pack, String cuName, String source)
			throws JavaModelException {
		StringBuffer buf = new StringBuffer();
		//buf.append("package " + pack.getElementName() + ";\n");
		buf.append("\n");
		buf.append(source);
		ICompilationUnit cu = pack.createCompilationUnit(cuName,
				buf.toString(), false, null);
		return cu.getTypes()[0];
	}

	public void dispose() throws CoreException {
		waitForIndexer();
		project.delete(true, true, null);
	}

	private IFolder createBinFolder() throws CoreException {
		IFolder binFolder = project.getFolder("bin");
		binFolder.create(false, true, null);
		return binFolder;
	}

	private void setJavaNature() throws CoreException {
		IProjectDescription description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(description, null);
	}

	private void createOutputFolder(IFolder binFolder)
			throws JavaModelException {
		IPath outputLocation = binFolder.getFullPath();
		javaProject.setOutputLocation(outputLocation, null);
	}

	public IPackageFragmentRoot createSourceFolder(String name) throws CoreException {
		IFolder folder = project.getFolder(name);
		folder.create(false, true, null);
		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(folder);

		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaCore.newSourceEntry(root.getPath());
		javaProject.setRawClasspath(newEntries, null);
		return root;
	}

	private void addSystemLibraries() throws JavaModelException {
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaRuntime.getDefaultJREContainerEntry();
		javaProject.setRawClasspath(newEntries, null);
	}

	private Path findFileInPlugin(String plugin,String file)
			throws MalformedURLException, IOException {
		//get the bundle with highest version 
		Bundle bundle=Platform.getBundle(plugin);
		URL jarURL=bundle.getEntry(file);		
		/*Bundle[] bundle=Platform.getBundles(plugin, version);
		URL jarURL=bundle[0].getEntry(file);*/
		
		URL localJarURL = FileLocator.toFileURL(jarURL);		
		return new Path(localJarURL.getPath());
	}

	private void waitForIndexer() throws JavaModelException {
		new SearchEngine().searchAllTypeNames(null, null,
				SearchPattern.R_CASE_SENSITIVE | SearchPattern.R_EXACT_MATCH,
				IJavaSearchConstants.CLASS, 
				SearchEngine.createJavaSearchScope(new IJavaElement[0]),
				new TypeNameRequestor(){},
				//WAIT_UNTIL_READY_TO_SEARCH = IJob.WaitUntilReady;
				//IJob.WaitUntilReady is a waiting policy of IJob
				IJavaSearchConstants.WAIT_UNTIL_READY_TO_SEARCH, null);
	}
}
