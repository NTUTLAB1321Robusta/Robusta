package ntut.csie.csdet.report.ui;

import ntut.csie.csdet.report.ReportBuilder;
import ntut.csie.csdet.report.ReportModel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * ProcessBar動作
 * @author Shiau
 */
public class ProgressActionJob extends Job {
	//Project的資料
	private IProject project;
	//Report的資料
	private ReportModel model;

	public ProgressActionJob(String name, IProject project, ReportModel model) {
		super(name);
		
		//取得資料
		this.model = model;
		this.project = project;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {

		if(monitor.isCanceled()){
			return Status.CANCEL_STATUS;
		}

		monitor.beginTask("Running....", IProgressMonitor.UNKNOWN);

		//建置Report
		ReportBuilder buildReport = new ReportBuilder(project,model);
		
		long start = System.currentTimeMillis();		
		buildReport.run();
		long end = System.currentTimeMillis();
		System.out.println("花費時間 "+(end - start) + " milli second.");

		monitor.worked(1);

		return Status.OK_STATUS;
	}

}
