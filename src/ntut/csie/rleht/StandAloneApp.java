package ntut.csie.rleht;

import java.util.Map;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.report.BadSmellDataStorage;
import ntut.csie.csdet.report.ReportBuilder;
import ntut.csie.csdet.report.ReportModel;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.jdt.ui.wizards.JavaCapabilityConfigurationPage;

public class StandAloneApp implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		Map args = context.getArguments();  
		String[] appArgs = (String[]) args.get("application.args");  
		String projectPath = appArgs[0];

		IProjectDescription description = workspace.loadProjectDescription(new Path(projectPath + "/.project"));
		
		IProject project = workspace.getRoot().getProject(description.getName());
		
		JavaCapabilityConfigurationPage.createProject(project, description.getLocationURI(), null);
		
		BadSmellDataStorage dataStorage = new BadSmellDataStorage(project.getLocation().toString());
		ReportBuilder reportBuilder = new ReportBuilder(project, new NullProgressMonitor());
		
		//Active all smell type before run report builder
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.activateAllConditions(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		IStatus returnStatus = reportBuilder.run();
		if(returnStatus.isOK()) {
			dataStorage.save(reportBuilder.getReportModel());
		}
		
		ResourcesPlugin.getWorkspace().save(true, null);
    	return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
	}
}
