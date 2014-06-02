package ntut.csie.csdet.preference;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class BadSmellDataReader {
	final String TAG_EHSmellReport = "EHSmellReport";
	final String TAG_EHSmellList = "EHSmellList";
	final String TAG_Summary = "Summary";
	final String TAG_DateTime = "DateTime";

	private Document smellListDoc;

	public BadSmellDataReader(File xmlFile) {
		SAXBuilder builder = new SAXBuilder();
		try {
			smellListDoc = builder.build(xmlFile);
		} catch (JDOMException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public BadSmellDataReader(String xmlFilepath) {
		this(new File(xmlFilepath));
	}

	public Element getEHSmellListElement() {
		Element root = smellListDoc.getRootElement();
		Element tagEHSmellListElement = root.getChild(TAG_EHSmellList);
		return tagEHSmellListElement;
	}

	public Element getDateTimeElement() {
		Element root = smellListDoc.getRootElement();
		Element summaryElement = root.getChild(TAG_Summary);
		return summaryElement.getChild(TAG_DateTime);
	}
}
