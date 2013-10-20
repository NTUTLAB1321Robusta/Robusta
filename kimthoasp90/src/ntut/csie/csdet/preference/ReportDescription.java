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

public class ReportDescription {
	public final static String SETTING_FILENAME = "ReportDescription.xml";
	public final static String TAG_ROOT = "Reports";
	public final static String TAG_DESCRIPTION = "Report";
	public final static String ATTRIBUTE_NAME = "name";
	public final static String ATTRIBUTE_DESCRIPTION = "description";
	private Document reportDescriptionDoc;

	public ReportDescription() {
		reportDescriptionDoc = new Document(new Element(TAG_ROOT));
	}
	
	public ReportDescription(File xmlFile) {
		this();
		if (!xmlFile.exists()) {
			return;
		}

		SAXBuilder builder = new SAXBuilder();
		try {
			reportDescriptionDoc = builder.build(xmlFile);
		} catch (JDOMException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public ReportDescription(String xmlFilepath) {
		this(new File(xmlFilepath));
	}

	public Element getDescription(String reportName) {
		Element root = reportDescriptionDoc.getRootElement();
		List<?> elements = root.getChildren(TAG_DESCRIPTION);
		Element tagPreferenceElement = null;
		for (Object s : elements) {
			Element preferenceElement = (Element) s;
			if (preferenceElement.getAttribute(ATTRIBUTE_NAME).getValue()
					.equals(reportName)) {
				tagPreferenceElement = preferenceElement;
				return tagPreferenceElement;
			}
		}

		if (tagPreferenceElement == null) {
			tagPreferenceElement = new Element(TAG_DESCRIPTION);
			tagPreferenceElement.setAttribute(ATTRIBUTE_NAME, reportName);
			tagPreferenceElement.setAttribute(ATTRIBUTE_DESCRIPTION, "");
			root.addContent(tagPreferenceElement);
		}
		return tagPreferenceElement;
	}

	public void deleteElementInXml(String reportName) {
		Element root = reportDescriptionDoc.getRootElement();
		List<?> elements = root.getChildren(TAG_DESCRIPTION);
		for (Object s : elements) {
			Element preferenceElement = (Element) s;
			if (preferenceElement.getAttribute(ATTRIBUTE_NAME).getValue()
					.equals(reportName)) {
				root.removeContent(preferenceElement);
				break;
			}
		}

	}

	public void setDescriptionAttribute(String reportName, String detail,
			String newDescription) {
		Element preElement = getDescription(reportName);
		preElement.setAttribute(detail, newDescription);
	}

	public String getDescriptionAttribute(String attributeName) {
		Element preElement = getDescription(attributeName);
		return preElement.getAttributeValue(ATTRIBUTE_DESCRIPTION).toString();
	}

	private void close(Closeable ioInstance) {
		if (ioInstance != null) {
			try {
				ioInstance.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void writeXMLFile(String path) {
		FileWriter fw = null;
		XMLOutputter out = new XMLOutputter();
		try {
			fw = new FileWriter(path);
			out.output(reportDescriptionDoc, fw);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			close(fw);
		}
	}

}
