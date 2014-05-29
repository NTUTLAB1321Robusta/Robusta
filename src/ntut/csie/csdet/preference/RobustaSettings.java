package ntut.csie.csdet.preference;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class RobustaSettings {
	public final static String SETTING_FILENAME = "RobustaSettings.xml";
	public final static String SETTING_REPORTFOLDERNAME = "Robusta_Report";
	public final static String TAG_ROOT = "RobustaSettings";
	public final static String TAG_PROJECTDETECT = "ProjectDetect";
	public final static String ATTRIBUTE_FOLDERNAME = "FolderName";
	public final static String ATTRIBUTE_ENABLE = "enable";
	
	private Document proDoc;
	private IProject project;

	public RobustaSettings() {
		proDoc = new Document(new Element(TAG_ROOT));
	}

	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public RobustaSettings(File xmlFile, IProject project) {
		this();
		this.project = project;
		if (!xmlFile.exists()) {
			return;
		}

		SAXBuilder builder = new SAXBuilder();
		try {
			proDoc = builder.build(xmlFile);
		} catch (JDOMException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public IProject getProject() {
		return project;
	}

	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public RobustaSettings(String xmlFilepath, IProject project) {
		this(new File(xmlFilepath), project);
	}

	public Element getProjectDetect(String preferenceName) {
		Element root = proDoc.getRootElement();
		List<?> elements = root.getChildren(TAG_PROJECTDETECT);
		Element tagPreferenceElement = null;
		for (Object s : elements) {
			Element preferenceElement = (Element) s;
			if (preferenceElement.getAttribute(ATTRIBUTE_FOLDERNAME).getValue()
					.equals(preferenceName)) {
				tagPreferenceElement = preferenceElement;
				return tagPreferenceElement;
			}
		}

		if (tagPreferenceElement == null) {
			tagPreferenceElement = new Element(TAG_PROJECTDETECT);
			tagPreferenceElement.setAttribute(ATTRIBUTE_FOLDERNAME,
					preferenceName);
			tagPreferenceElement.setAttribute(ATTRIBUTE_ENABLE,
					String.valueOf(true));
			root.addContent(tagPreferenceElement);
		}
		return tagPreferenceElement;
	}

	public void setProjectDetectAttribute(String preferenceName,
			String attributeName, Boolean attributeValue) {
		Element preElement = getProjectDetect(preferenceName);
		preElement.setAttribute(attributeName, String.valueOf(attributeValue));
	}

	public boolean getProjectDetectAttribute(String attributeName) {
		Element preElement = getProjectDetect(attributeName);
		return Boolean.parseBoolean(preElement
				.getAttributeValue(ATTRIBUTE_ENABLE));
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
	public void writeNewXMLFile(String path) {
		FileWriter fw = null;
		XMLOutputter out = new XMLOutputter();
		try {
			fw = new FileWriter(path);
			out.output(proDoc, fw);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			close(fw);
		}
	}
	
	public static String getRobustaReportFolder(String projectName) {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IPath path = project.getLocation();
		String projectPath = null;
		if(path != null)
			projectPath = path.toOSString() + "/" +SETTING_REPORTFOLDERNAME;
		return projectPath;
	}

	public static String getPathOfReportDescriptionFile(String projectName) {
		return getRobustaReportFolder(projectName)
				+ ReportDescription.SETTING_FILENAME;
	}

}
