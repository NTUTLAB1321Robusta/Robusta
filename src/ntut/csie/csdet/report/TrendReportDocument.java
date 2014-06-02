package ntut.csie.csdet.report;

import java.io.File;
import java.util.List;

import ntut.csie.csdet.preference.BadSmellDataReader;

import org.jdom.Document;
import org.jdom.Element;

public class TrendReportDocument {

	Document trendReportDoc;
	private final static String TAG_ROOT = "TrendReports";
	private final String TAG_REPORT = "Report";

	public Document collectTrendReportData(List<File> files) {
		Element root = new Element(TAG_ROOT);
		trendReportDoc = new Document(root);
		for (File file : files) {
			Element reportElement = new Element(TAG_REPORT);
			root.addContent(reportElement);
			BadSmellDataReader smellList = new BadSmellDataReader(file.getAbsolutePath());
			Element dateTimeElement = smellList.getDateTimeElement();
			Element cloneElement = (Element) dateTimeElement.clone();
			reportElement.addContent(cloneElement);
			Element badsmellList = smellList.getEHSmellListElement();
			cloneElement = (Element) badsmellList.clone();
			reportElement.addContent(cloneElement);
		}
		return trendReportDoc;
	}
}
