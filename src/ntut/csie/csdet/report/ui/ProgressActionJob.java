package ntut.csie.csdet.report.ui;

import ntut.csie.csdet.report.ReportBuilder;
import ntut.csie.csdet.report.ReportModel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * ProcessBar�ʧ@
 * @author Shiau
 */
public class ProgressActionJob extends Job {
	//Project�����
	private IProject project;
	//Report�����
	private ReportModel model;

	public ProgressActionJob(String name, IProject project, ReportModel model) {
		super(name);
		
		//���o���
		this.model = model;
		this.project = project;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {

		if(monitor.isCanceled()){
			return Status.CANCEL_STATUS;
		}

		monitor.beginTask("Running....", IProgressMonitor.UNKNOWN);

		//�ظmReport
		ReportBuilder buildReport = new ReportBuilder(project,model);
		
		long start = System.currentTimeMillis();		
		buildReport.run();
		long end = System.currentTimeMillis();
		System.out.println("��O�ɶ� "+(end - start) + " milli second.");

		monitor.worked(1);

		return Status.OK_STATUS;
	}

}
