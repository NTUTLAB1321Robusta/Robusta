package ntut.csie.rleht.builder;

import java.util.Iterator;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToggleNatureAction implements IObjectActionDelegate {
	private static Logger logger = LoggerFactory.getLogger(ToggleNatureAction.class);
	private ISelection selection;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	public void run(IAction action) {
		if (selection instanceof IStructuredSelection) {
			for (Iterator<?> it = ((IStructuredSelection) selection).iterator(); it.hasNext();) {
				Object element = it.next();
				IProject project = null;
				if (element instanceof IProject) {
					project = (IProject) element;
				}
				else if (element instanceof IAdaptable) {
					project = (IProject) ((IAdaptable) element).getAdapter(IProject.class);
				}

				if (project != null) {
					toggleNature(project);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction,
	 *      org.eclipse.jface.viewers.ISelection)
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		this.selection = selection;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.IObjectActionDelegate#setActivePart(org.eclipse.jface.action.IAction,
	 *      org.eclipse.ui.IWorkbenchPart)
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * Toggles sample nature on a project
	 * 
	 * @param project
	 *            to have sample nature added or removed
	 */
	private void toggleNature(IProject project) {
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			// 沒有設定檔存在時，幫使用者預設為所有的條件都勾選
			SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
			smellSettings.activateAllConditions(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
			
			for (int i = 0; i < natures.length; ++i) {
				if (RLNature.NATURE_ID.equals(natures[i])) {
					// Remove the nature
					String[] newNatures = new String[natures.length - 1];
					System.arraycopy(natures, 0, newNatures, 0, i);
					System.arraycopy(natures, i + 1, newNatures, i, natures.length - i - 1);
					description.setNatureIds(newNatures);
					project.setDescription(description, null);
					
					// 刪除Maker
					project.accept(new RLResourceVisitor());
					return;
				}
			}

			// Add the nature
			String[] newNatures = new String[natures.length + 1];
			System.arraycopy(natures, 0, newNatures, 0, natures.length);
			newNatures[natures.length] = RLNature.NATURE_ID;
			description.setNatureIds(newNatures);
			project.setDescription(description, null);
		}
		catch (CoreException ex) {
			logger.error("[toggleNature] EXCEPTION ",ex);
		}
	}

	// ==========================================================================

	class RLMethodDeltaVisitor implements IResourceDeltaVisitor {
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					// handle added resource
					deleteMarkers(resource);
					break;
				case IResourceDelta.REMOVED:
					// handle removed resource
					break;
				case IResourceDelta.CHANGED:
					// handle changed resource
					deleteMarkers(resource);
					break;
			}
			// return true to continue visiting children.
			return true;
		}
	}

	class RLResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			deleteMarkers(resource);
			// return true to continue visiting children.
			return true;
		}
	}


	private void deleteMarkers(IResource resource) {
		try {
			if (resource instanceof IFile && resource.getName().endsWith(".java")) {
				IFile file = (IFile) resource;
				file.deleteMarkers(RLBuilder.MARKER_TYPE, false, IResource.DEPTH_ZERO);
			}
		}
		catch (CoreException ex) {
			logger.error("[deleteMarkers] EXCEPTION ",ex);
		}
	}

}
