package ntut.csie.robusta.agile.exception;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddAgileExceptionToProjLib implements IObjectActionDelegate {
	private static Logger logger = LoggerFactory
			.getLogger(AddAgileExceptionToProjLib.class);
	private ISelection selection;
	// TODO globalize the pluginId and agileExceptionJarId
	private final String pluginId = "taipeitech.csie.robusta_1.6.7.1";
	private final String agileExceptionJarId = "taipeitech.csie.robusta.agile.exception_1.0.0";

	@Override
	public void run(IAction action) {
		if (selection instanceof IStructuredSelection) {
			for (Iterator<?> it = ((IStructuredSelection) selection).iterator(); it
					.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject) {
					project = (IProject) element;
				} else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element)
							.getAdapter(IProject.class);
				}

				if (project != null) {
					try {
						copyJarFile(project);
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					refreshProject(project);
				}
			}
		}
	}

	private void copyJarFile(IProject project) throws FileNotFoundException {
		IPath projPath = project.getLocation();
		URL installURL = Platform.getInstallLocation().getURL();
		Path eclipsePath = new Path(installURL.getPath());

		File projLib = new File(projPath + "/lib");
		File fileDest = new File(projLib.toString() + "/" + agileExceptionJarId
				+ ".jar");
		
		// check if AgileException.jar already resides in user's project
		if (fileDest.exists()) {
			return;
			// TODO warn user with a pop-up window?
		}

		try {
			JarFile RobustaJar = new JarFile(eclipsePath.toString()
					+ "/plugins/" + pluginId + ".jar");
			final Enumeration<JarEntry> entries = RobustaJar.entries();
			while (entries.hasMoreElements()) {
				final JarEntry entry = entries.nextElement();
				if (entry.getName().contains(agileExceptionJarId)) {
					InputStream is = RobustaJar.getInputStream(entry);
					copyFileUsingFileStreams(is, fileDest);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		setBuildPath(project, fileDest);
	}

	private static void copyFileUsingFileStreams(InputStream source, File dest)
			throws IOException {
		dest.getParentFile().mkdirs();
		OutputStream output = null;
		try {
			output = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = source.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}
		} finally {
			source.close();
			output.close();
		}
	}

	private void refreshProject(IProject project) {
		// build project to refresh
		try {
			project.build(IncrementalProjectBuilder.FULL_BUILD,
					new NullProgressMonitor());
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void setBuildPath(IProject project, File fileAlreadyExistChecker) {
		// add path of agileException.jar to .classpath file of the project
		try {
			addClasspathEntryToBuildPath(JavaCore.newLibraryEntry(new Path(
					fileAlreadyExistChecker.toString()), null, null), null,
					project);
			System.out.println("Adding class path");
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		// TODO Auto-generated method stub
	}

	/**
	 * Add class path entry to inner project's class path
	 */
	public void addClasspathEntryToBuildPath(IClasspathEntry classpathEntry,
			IProgressMonitor progressMonitor, IProject proj)
			throws JavaModelException {
		IJavaProject javaProject = JavaCore.create(proj);
		boolean classPathExist = checkExistInClassPath(javaProject);

		// if class path for AgileException.jar is already set, bypass
		// setting
		if (classPathExist) {
			return;
		} else {
			IClasspathEntry[] existedEntries = javaProject.getRawClasspath();
			IClasspathEntry[] extendedEntries = new IClasspathEntry[existedEntries.length + 1];
			System.arraycopy(existedEntries, 0, extendedEntries, 0,
					existedEntries.length);
			extendedEntries[existedEntries.length] = classpathEntry;
			javaProject.setRawClasspath(extendedEntries, progressMonitor);
		}
	}

	private boolean checkExistInClassPath(IJavaProject javaProject)
			throws JavaModelException {
		IClasspathEntry[] ICPEntry = javaProject.getRawClasspath();
		for (IClasspathEntry entry : ICPEntry) {
			if (entry.toString().contains(agileExceptionJarId)) {
				return true;
			}
		}
		return false;
	}

}
