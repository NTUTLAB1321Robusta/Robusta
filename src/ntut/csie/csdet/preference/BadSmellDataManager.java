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

public class BadSmellDataManager {
	final String TAG_EHSMELLLIST = "EHSmellList";
	final String TAG_SUMMARY = "Summary";
	final String TAG_DATETIME = "DateTime";
	final String TAG_DESCRIPTION ="Description";

	private Document doc;

	public BadSmellDataManager(File xmlFile) {
		SAXBuilder builder = new SAXBuilder();
		try {
			doc = builder.build(xmlFile);
		} catch (JDOMException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public BadSmellDataManager(String xmlFilepath) {
		this(new File(xmlFilepath));
	}

	public Element getEHSmellListElement() {
		Element root = doc.getRootElement();
		Element tagEHSmellListElement = root.getChild(TAG_EHSMELLLIST);
		return tagEHSmellListElement;
	}

	public Element getDateTimeElement() {
		Element root = doc.getRootElement();
		Element summaryElement = root.getChild(TAG_SUMMARY);
		return summaryElement.getChild(TAG_DATETIME);
	}
	
	public Element getDescriptionElement(){
		Element root = doc.getRootElement();
		Element summaryElement = root.getChild(TAG_SUMMARY);
		return summaryElement.getChild(TAG_DESCRIPTION);
	}
	
	public void setDescriptionElement(String newDescription){
		Element descriptionElement = getDescriptionElement();
		if (descriptionElement == null) {
			Element root = doc.getRootElement();
			Element summaryElement = root.getChild(TAG_SUMMARY);
			descriptionElement = new Element(TAG_DESCRIPTION);
			summaryElement.addContent(descriptionElement);
		}
		descriptionElement.setText(newDescription);
	}
	
	public void writeXMLFile(String path) {
		FileWriter fw = null;
		XMLOutputter out = new XMLOutputter();
		try {
			fw = new FileWriter(path);
			out.output(doc, fw);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			close(fw);
		}
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
}
