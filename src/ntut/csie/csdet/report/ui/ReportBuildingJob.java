package ntut.csie.csdet.report.ui;

import ntut.csie.csdet.report.BadSmellDataStorage;
import ntut.csie.csdet.report.ReportBuilder;
import ntut.csie.csdet.report.ReportModel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Action Job to execute building report asynchronously
 * 
 */
public class ReportBuildingJob extends Job {
	//Project的資料
	private IProject project;
	private BadSmellDataStorage dataStorage;

	public ReportBuildingJob(String name, IProject project, BadSmellDataStorage dataStorage) {
		super(name);
		this.dataStorage = dataStorage;
		this.project = project;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {

		if(monitor.isCanceled()){
			return Status.CANCEL_STATUS;
		}

		monitor.beginTask("Running....", IProgressMonitor.UNKNOWN);

		ReportBuilder reportBuilder = new ReportBuilder(project, monitor);

		long start = System.currentTimeMillis();
		IStatus status = reportBuilder.run();
		if(status.isOK()) {
			ReportModel reportModel = reportBuilder.getReportModel();
			dataStorage.save(reportModel);
		}
		
		long end = System.currentTimeMillis();
		System.out.println("Completed: " + (end - start) + " milli second.");
		
		return status;
	}

}
