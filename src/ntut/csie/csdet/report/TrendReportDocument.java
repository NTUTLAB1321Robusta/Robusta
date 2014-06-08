package ntut.csie.csdet.report;

import java.io.File;
import java.util.List;


import org.jdom.Document;
import org.jdom.Element;

public class TrendReportDocument {

	Document trendReportDoc;
	private final static String TAG_ROOT = "TrendReports";
	private final String TAG_REPORT = "Report";
	private final String PROJECT_INFO = "ProjectInfo";
	private final String PROJECT_NAME = "ProjectName";
	private String projectName;
	
	public TrendReportDocument(String projectName) {
		super();
		this.projectName = projectName;
	}

	public String getProjectName() {
		return projectName;
	}

	public Document collectTrendReportData(List<File> files) {
		Element root = new Element(TAG_ROOT);
		
		Element projectInfo = new Element(PROJECT_INFO);
		Element projectName = new Element(PROJECT_NAME);
		projectName.setText(getProjectName());
		projectInfo.addContent(projectName);
		root.addContent(projectInfo);

		trendReportDoc = new Document(root);
		
		for (File file : files) {
			Element reportElement = new Element(TAG_REPORT);
			root.addContent(reportElement);
			BadSmellData badSmellDataManager = new BadSmellData(file.getAbsolutePath());
			Element dateTimeElement = badSmellDataManager.getDateTimeElement();
			Element cloneElement = (Element) dateTimeElement.clone();
			reportElement.addContent(cloneElement);
			Element badsmellList = badSmellDataManager.getEHSmellListElement();
			cloneElement = (Element) badsmellList.clone();
			reportElement.addContent(cloneElement);
		}
		return trendReportDoc;
	}
}
