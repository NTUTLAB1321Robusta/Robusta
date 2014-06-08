package ntut.csie.csdet.report.ui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.report.BadSmellDataStorage;
import ntut.csie.csdet.report.PastReportsHistory;
import ntut.csie.csdet.report.ReportContentCreator;
import ntut.csie.csdet.report.TrendReportDocument;
import ntut.csie.rleht.RLEHTPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

public class ReportViewPresentationModel {
	ISmellReportView smellReportView;
	
	int selectProjectIndex = -1;
	private BadSmellDataStorage dataStorage;
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));

	private IResourceChangeListener listener;
	
	public ReportViewPresentationModel(ISmellReportView smellReportView) {
		super();
		this.smellReportView = smellReportView;
	}
	
	public int getSelectProjectIndex() {
		return selectProjectIndex;
	}

	public void setSelectProjectIndex(int selectProjectIndex) {
		this.selectProjectIndex = selectProjectIndex;
	}
	
	public String getSelectProjectName() {
		if(getSelectProjectIndex() == -1) {
			return null;
		} else {
			return getProjectNameList().get(getSelectProjectIndex());
		}
	}

	public List<String> getProjectNameList() {
		List<String> projectName = new ArrayList<String>();

		IProject[] projectList = getCurrentOpenProjectList();
		for (int i=0; i < projectList.length; i++)
			if (projectList[i].isOpen())
				projectName.add(projectList[i].getName());
		return projectName;
	}

	private IProject[] getCurrentOpenProjectList() {
		IProject[] projectList  = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		return projectList;
	}
	
	//To update the current project list
	public void subscribeProjectEvents() {
		listener = new IResourceChangeListener() {
			@Override
			public void resourceChanged(IResourceChangeEvent event) {
				IResourceDelta root = event.getDelta();
				if(root == null) {
					return;
				}
				IResourceDelta[] projectDeltas = root.getAffectedChildren();
				for (int i = 0; i < projectDeltas.length; i++) {
					final IResourceDelta delta = projectDeltas[i];
					IResource resource = delta.getResource();
					if (resource.getType() == IResource.PROJECT) {
						PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							public void run() {
								smellReportView.updateProjectList(getProjectNameList());
							}
						});
					}
				}
			}
		};
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.addResourceChangeListener(listener, IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE);
	}
	
	public void unsubscribeProjectEvents() {
		if (listener != null) {
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			workspace.removeResourceChangeListener(listener);
		}
	}
	
	public void generateTrendReport() {
		IProject[] projectList = getCurrentOpenProjectList();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.activateAllConditionsIfNotConfugured(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		for (IProject project : projectList) {
			if (project.getName().equals(getSelectProjectName())) {
				PastReportsHistory pastReportsHistory = new PastReportsHistory();
				List<File> files = pastReportsHistory.getFileList(project.getName());
				if (files.size() > 0) {
					TrendReportDocument trendReportDocument = new TrendReportDocument(project.getName());
					Document doc = trendReportDocument.collectTrendReportData(files);
					openBrowserForTrendReport(project.getName(), doc);
				}
				break;
			}
		}
	}
	
	public void generateDensityReport() {
		IProject[] projectList = getCurrentOpenProjectList();

		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.activateAllConditionsIfNotConfugured(UserDefinedMethodAnalyzer.SETTINGFILEPATH);

		for (IProject project : projectList) {
			if (project.getName().equals(getSelectProjectName())) {
				buildDensityReport(project);
				break;
			}
		}
	}
	
	public void openDensityReport(String dataPath, String projectName) {
		Document inputXmlDoc = getInputXmlDocument(dataPath);
		
		String JS_DATA_PATH = "/js/data.js";
		String REPORT_DATA_TRANSFORM = "/report/datatransform.xsl";
		
		ReportContentCreator reportContentCreator = new ReportContentCreator(JS_DATA_PATH, REPORT_DATA_TRANSFORM, inputXmlDoc, projectName);
		reportContentCreator.exportReportResources();
		reportContentCreator.transformDataFile();
		smellReportView.setBrowserUrl("file:///" + reportContentCreator.getDestinationFolderPath() + "/index.html");
	}
	private Document getInputXmlDocument(String dataPath) {
		Document inputXmlDoc;
		SAXBuilder builder = new SAXBuilder();
		try {
			inputXmlDoc = builder.build(dataPath);
		} catch (JDOMException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return inputXmlDoc;
	}	
	
	public void buildDensityReport(IProject project) {
		
		dataStorage = new BadSmellDataStorage(project.getLocation().toString());
		final ReportBuildingJob job = new ReportBuildingJob(resource.getString("SmellReport.generateReportProgressBarTitle"), project, dataStorage);
		
		job.setPriority(Job.SHORT);
		
		final IWorkbenchSiteProgressService progressService = (IWorkbenchSiteProgressService) RLEHTPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite().getAdapter(IWorkbenchSiteProgressService.class);
		progressService.showInDialog(RLEHTPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite().getShell(), job);

		job.addJobChangeListener(new JobChangeAdapter() {
			public void done(IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					openDensityReportBrowser(true);
				} else {
					openDensityReportBrowser(false);
				}
			}
		});
		job.schedule();
	}
	

	private void openDensityReportBrowser(final boolean isGenerateSucceed) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (isGenerateSucceed) {
					String dataPath = dataStorage.getResultDataPath();
					openDensityReport(dataPath, getSelectProjectName());
				} else {
					smellReportView.setBrowserText(resource.getString("SmellReport.browser.canceled"));
				}
			}
		});
	}
	
	public void openBrowserForTrendReport(final String projectName, final Document doc) {
		String JS_TRENDREPORTDATA_PATH = 	"/js/datatrend.js";
		String TRENDREPORT_DATA_TRANSFORM = "/report/trenddatatransform.xsl";
		ReportContentCreator reportContentCreator = new ReportContentCreator(JS_TRENDREPORTDATA_PATH, TRENDREPORT_DATA_TRANSFORM, doc, projectName);
		reportContentCreator.exportReportResources();
		reportContentCreator.transformDataFile();
		smellReportView.setBrowserUrl("file:///" + reportContentCreator.getDestinationFolderPath()+"/trendreport.html");
	}
}
