package ntut.csie.rleht;

import java.util.Map;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.report.ReportBuilder;
import ntut.csie.csdet.report.ReportModel;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class StandAloneApp implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		Map args = context.getArguments();  
		String[] appArgs = (String[]) args.get("application.args");  
		String projectPath = appArgs[0];

		IProjectDescription description = workspace.loadProjectDescription(new Path(projectPath+"/.project"));
		IProject project = workspace.getRoot().getProject(description.getName());
		if(!project.exists()) project.create(description, null);
		project.open(null);
		
		ReportModel reportModel = new ReportModel();
		ReportBuilder reportBuilder = new ReportBuilder(project, reportModel);
		
		//Active all smell type before run report builder
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		smellSettings.activateAllConditions(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		
		reportBuilder.run();
		
    	return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
	}
}
