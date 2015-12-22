package ntut.csie.rleht.builder;

import java.util.Iterator;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.robusta.marker.EditorTracker;
import ntut.csie.robusta.marker.MarkerModel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ToggleNatureAction implements IObjectActionDelegate {
	private static final String ADDDETECTOR = "Robusta.addRLNatureAction";
	private static final String REMOVEDETECTOR = "Robusta.removeRLNatureAction";
	private static Logger logger = LoggerFactory.getLogger(ToggleNatureAction.class);
	private ISelection selection;
	
	private static MarkerModel markerModel= new MarkerModel();
	
	//add listener on editor that when its opened or update editor's content, editor will be added annotation
	private static EditorTracker editorTracker = new EditorTracker(PlatformUI.getWorkbench(), markerModel);;

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
					toggleNature(project, action);
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
	private void toggleNature(IProject project, IAction action) {
		try {
			IProjectDescription description = project.getDescription();
			String[] natures = description.getNatureIds();

			SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
			smellSettings.activateAllConditionsIfNotConfugured(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
			
			if (action.getId().equals(REMOVEDETECTOR)) {
				for (int i = 0; i < natures.length; ++i) {
					if (RLNature.NATURE_ID.equals(natures[i])) {
						// Remove the nature
						String[] newNatures = new String[natures.length - 1];
						System.arraycopy(natures, 0, newNatures, 0, i);
						System.arraycopy(natures, i + 1, newNatures, i,
								natures.length - i - 1);
						description.setNatureIds(newNatures);
						project.setDescription(description, null);

						// delete Maker
						markerModel.deleteMarkers(project);
						markerModel.unregisterMarkerService(project);
						break;
					}
				}
			} else if (action.getId().equals(ADDDETECTOR)) {
				// Add the nature
				String[] newNatures = new String[natures.length + 1];
				System.arraycopy(natures, 0, newNatures, 0, natures.length);
				newNatures[natures.length] = RLNature.NATURE_ID;
				description.setNatureIds(newNatures);
				project.setDescription(description, null);
				
				markerModel.registerMarkerService(project);
			}

			// Build the project, even if marker exist prior to the click action
			project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
		}
		catch (CoreException ex) {
			logger.error("[toggleNature] EXCEPTION ",ex);
		}
	}
}
