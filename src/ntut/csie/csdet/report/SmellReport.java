package ntut.csie.csdet.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmellReport {
	private static Logger logger = LoggerFactory.getLogger(SmellReport.class);
	ReportModel model;
	
	SmellReport(ReportModel reportModel) {
		model = reportModel;
	}

	/**
	 * 產生Smell Report
	 */
	void build() {
		if (model != null) {
			//產生XML
			String xmlString = createXML();
			//利用XML把XSL內的欄位填上，並產生HTM檔
			createHTM(xmlString);
			//輸出HTM檔的Styles.css
			createStyles();
		}
	}
	
	/**
	 * 產生XML
	 * @return
	 */
	private String createXML() {
		Element root = new Element("EHSmellReport");
		Document myDocument = new Document(root);
		
		///Summary資料輸出///
		Element summary = new Element("Summary");
		summary.addContent(new Element("ProjectName").addContent(model.getProjectName()));
		summary.addContent(new Element("DateTime").addContent(model.getDateTime()));
		root.addContent(summary);

		///EH Smell List資料輸出///
		Element smellList = new Element("EHSmellList");
		smellList.addContent(new Element("IgnoreCheckedException").addContent(String.valueOf(model.getIgnoreTotalSize())));
		smellList.addContent(new Element("DummyHandler").addContent(String.valueOf(model.getDummyTotalSize())));
		smellList.addContent(new Element("UnprotectedMainProgram").addContent(String.valueOf(model.getUnMainTotalSize())));
		smellList.addContent(new Element("NestedTryBlock").addContent(String.valueOf(model.getNestedTryTotalSize())));
		smellList.addContent(new Element("Total").addContent(String.valueOf(model.getTotalSmellCount())));
		root.addContent(smellList);

		///Package List資料輸出///
		int ignoreTotalSize = 0;
		int dummyTotalSize = 0;
		int unMainTotalSize = 0;
		int nestedTryTotalSize = 0;
		for (int i=0; i < model.getPackagesSize(); i++) {
			PackageModel packageModel = model.getPackage(i);
			ignoreTotalSize += packageModel.getIgnoreSize();
			dummyTotalSize += packageModel.getDummySize();
			unMainTotalSize += packageModel.getUnMainSize();
			nestedTryTotalSize += packageModel.getNestedTrySize();

			Element pkList = new Element("PackageList");
			
			if (packageModel.getPackageName() == "")				
				pkList.addContent(new Element("PackageName").addContent("(default package)"));
			else
				pkList.addContent(new Element("PackageName").addContent(packageModel.getPackageName()));
			
			pkList.addContent(new Element("IgnoreCheckedException")
								.addContent(String.valueOf(packageModel.getIgnoreSize())));
			pkList.addContent(new Element("DummyHandler")
								.addContent(String.valueOf(packageModel.getDummySize())));
			pkList.addContent(new Element("UnprotectedMainProgram")
								.addContent(String.valueOf(packageModel.getUnMainSize())));
			pkList.addContent(new Element("NestedTryBlock")
								.addContent(String.valueOf(packageModel.getNestedTrySize())));
			pkList.addContent(new Element("Total")
								.addContent(String.valueOf(packageModel.getTotalSmellSize())));
			root.addContent(pkList);
		}

		///Package List 總和資料輸出///
		Element pkTotal = new Element("PackageListTotal");
		pkTotal.addContent(new Element("IgnoreTotal").addContent(String.valueOf(ignoreTotalSize)));
		pkTotal.addContent(new Element("DummyTotal").addContent(String.valueOf(dummyTotalSize)));
		pkTotal.addContent(new Element("UnMainTotal").addContent(String.valueOf(unMainTotalSize)));
		pkTotal.addContent(new Element("NestedTrTotal").addContent(String.valueOf(nestedTryTotalSize)));
		pkTotal.addContent(new Element("PackagesTotal")
			.addContent(String.valueOf(ignoreTotalSize + dummyTotalSize + unMainTotalSize + nestedTryTotalSize)));
		root.addContent(pkTotal);

		Format fmt = Format.getPrettyFormat();
		XMLOutputter xmlOut = new XMLOutputter(fmt);
		StringWriter writer = new StringWriter();
       	try {
			xmlOut.output(myDocument, writer);
//			FileWriter ABC = new FileWriter("/myFile.xml");
//			xmlOut.output(myDocument, ABC);
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		} finally {
			closeStingWriter(writer);
		}
       	return writer.getBuffer().toString();
	}

	/**
	 * Close StringWriter
	 * @param writer
	 */
	private void closeStingWriter(StringWriter writer) {
		try {
			writer.close();
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		}
	}

	/**
	 * 輸出HTM檔的Styles.css
	 */
	void createStyles()
	{
		FileWriter fw = null;
		try {
			InputStream inputStyle = this.getClass().getResourceAsStream("/xslTemplate/styles.css");

			BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStyle, "UTF-8"));
			fw = new FileWriter(model.getProjectPath() + "/styles.css");

			//把讀取到的資料輸出
			String thisLine = null;
			while ((thisLine = bReader.readLine()) != null) {
				fw.write(thisLine);
			}

		} catch (IOException ex) {
			logger.error("[IOException] EXCEPTION ",ex);
		} finally {
			closeFileWriter(fw);
		}
	}

	/**
	 * Close FileWriter
	 * @param fw
	 */
	private void closeFileWriter(FileWriter fw) {
		try {
			fw.close();
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		}
	}
	
	/**
	 * 利用XML把XSL內的欄位填上，並產生HTM檔
	 * @param xmlString
	 */
	void createHTM(String xmlString) {
		try {
			File metadataPath = new File(model.getProjectPath());
			
			if(!metadataPath.exists())
				metadataPath.mkdir();
			
			InputStream inputStream = this.getClass().getResourceAsStream("/xslTemplate/sample.xsl");

			Source xslSource = new StreamSource(inputStream);

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = null;
			transformer = tf.newTransformer(xslSource);
			Source xmlSource = new StreamSource(new StringReader(xmlString));

			FileOutputStream outputSteam = new FileOutputStream(model.getProjectPath() + "/sample.html");

			Result htmlResult = new StreamResult(outputSteam);
			transformer.transform(xmlSource, htmlResult);

			outputSteam.close();
		} catch (IOException ex) {
			logger.error("[IOException] EXCEPTION ",ex);
		} catch (TransformerConfigurationException ex) {
			logger.error("[Transformer Configuration Exception] EXCEPTION ",ex);
		} catch (TransformerException ex) {
			logger.error("[Transformer Exception] EXCEPTION ",ex);
		}
	}
}
