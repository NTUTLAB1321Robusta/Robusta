package ntut.csie.csdet.report.ui;

import java.util.List;

public interface ISmellReportView {
	public void setBrowserText(String text);
	public void setBrowserUrl(String url);
	public void updateProjectList(List<String> projectList);
}
