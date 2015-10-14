package ntut.csie.robusta.agile.exception;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import ntut.csie.util.PopupDialog;
import ntut.csie.util.RLAnnotationFileUtil;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
/**
 * @author Peter
 * 
 */
public class EnableRLAnnotation implements IObjectActionDelegate {
	private static Logger logger = LoggerFactory
			.getLogger(EnableRLAnnotation.class);
	private ISelection selection;
	
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
					enableRLAnnotation(project);
					// save project to a final variable so that it can be used in Job,
					// it should be safe for that project should not change over time
					final IProject project2 = project;
					Job job = new Job("Refreshing Project"){
						protected IStatus run(IProgressMonitor monitor){
							refreshProject(project2);
							return Status.OK_STATUS;
						}
					};
					job.setPriority(Job.SHORT);
					job.schedule();
				}
			}
		}
	}
	
	private void enableRLAnnotation(IProject project) {
		URL installURL = Platform.getInstallLocation().getURL();
		Path eclipsePath = new Path(installURL.getPath());
		JarFile RobustaJar = RLAnnotationFileUtil.getRobustaJar(eclipsePath);

		JarEntry RLAnnotationJar = RLAnnotationFileUtil.getRLAnnotationJarEntry(project);
		InputStream is = null;
		
		if(RLAnnotationJar != null) {
			try {
				File projLib = RLAnnotationFileUtil.getProjectLibFolder(project);
				String RLAnnotationJarId = RLAnnotationFileUtil.extractRLAnnotationJarId(RLAnnotationJar.getName());
				File fileDest = new File(projLib.toString() + "/" + RLAnnotationJarId);
				is = RobustaJar.getInputStream(RLAnnotationJar);
				
				RLAnnotationFileUtil.copyFileUsingFileStreams(is, fileDest);
				setBuildPath(project, fileDest);
			} catch (IOException e) {
				throw new RuntimeException("Functional failure: Fail to copy RLAnnotation jar to user lib", e);
			} finally {
				closeStream(is);
			}
		} else {
			showOneButtonPopUpMenu(
					"Functional failure",
					"Fail to locate Robusta jar in eclipse plugin folder, please make sure it's installed properly");
		}
	}

	private void showOneButtonPopUpMenu(final String title, final String msg) {
		PopupDialog.showDialog(title, msg);
	}

	private void refreshProject(IProject project) {
		// build project to refresh
		try {
			project.build(IncrementalProjectBuilder.FULL_BUILD,
					new NullProgressMonitor());
		} catch (CoreException e) {
			showOneButtonPopUpMenu("Refresh failed",
					"Fail to refresh your project, please do it manually");
		}
	}

	private void setBuildPath(IProject project, File fileAlreadyExistChecker) {
		// add path of agileException.jar to .classpath file of the project
		try {
			addClasspathEntryToBuildPath(JavaCore.newLibraryEntry(new Path(
					fileAlreadyExistChecker.toString()), null, null), null,
					project);
		} catch (JavaModelException e) {
			throw new RuntimeException(
					"Fail to add agile exception jar to user's project build path",
					e);
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
		boolean classPathExist = RLAnnotationFileUtil.doesRLAnnotationExistInClassPath(javaProject);

		// if class path for AgileException.jar is already set, bypass
		// setting procedures
		if (classPathExist) {
			showOneButtonPopUpMenu("Oops...",
			"Robustness Level annotation already enabled :)");
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
	
	private void closeStream(Closeable io) {
		try {
			if (io != null)
				io.close();
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		}
	}

}
