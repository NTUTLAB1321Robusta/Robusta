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
	
	//Report���
	ReportModel model;

	SmellReport(ReportModel reportModel) {
		model = reportModel;
	}

	/**
	 * ����Smell Report
	 */
	void build() {
		if (model != null) {
			//����XML
			String xmlString = createXML();
			//�Q��XML��XSL��������W�A�ò���HTM��
			createHTM(xmlString);
			//��XHTM�ɪ�Styles.css
			createStyles();
		}
	}

	/**
	 * ����XML (�Ѻ����ѷӸ��)
	 * @return
	 */
	private String createXML() {
		Element root = new Element("EHSmellReport");
		Document myDocument = new Document(root);

		//��Summary��ƥ[��XML Root
		printSummary(root);
		
		//��Code Information�[��XML Root
		printCodeInfo(root);

		//��Packages�`����ƥ[��XML Root
		printAllPackageList(root);
		
		//��Package��ƥ[��XML Root
		printPackageList(root);

		Format fmt = Format.getPrettyFormat();
		XMLOutputter xmlOut = new XMLOutputter(fmt);
		StringWriter writer = new StringWriter();
       	try {
       		//��XXML
			xmlOut.output(myDocument, writer);
//			//�L�XXML��C�V (Debug��)
//			FileWriter writeXML = new FileWriter("/myFile.xml");
//			xmlOut.output(myDocument, writeXML);
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		} finally {
			closeStingWriter(writer);
		}
		//��XML�ন�r��
       	return writer.getBuffer().toString();
	}

	/**
	 * ��Summary��ƥ[��XML Root
	 * @param root
	 */
	private void printSummary(Element root) {
		///Summary��ƿ�X///
		Element summary = new Element("Summary");
		summary.addContent(new Element("ProjectName").addContent(model.getProjectName()));
		summary.addContent(new Element("DateTime").addContent(model.getBuildTime()));
		summary.addContent(new Element("JPGPath").addContent("file:///" + model.getFilePath("Report.jpg", true)));
		if (model.isDerectAllproject()) {
			//�Y���������h�L�X
			summary.addContent(new Element("Filter").addContent("[All Project]"));		
		} else {
			//�Y������L�X����
			if (model.getFilterList().size() != 0)
				summary.addContent(new Element("Filter").addContent(model.getFilterList().toString().replace("EH_STAR", "*")));
			//�Y�S������h�L�X�S������
			else
				summary.addContent(new Element("Filter").addContent("[No Package Select]"));			
		}
		root.addContent(summary);

		///EH Smell List��ƿ�X///
		Element smellList = new Element("EHSmellList");
		smellList.addContent(new Element("IgnoreCheckedException").addContent(String.valueOf(model.getIgnoreTotalSize())));
		smellList.addContent(new Element("DummyHandler").addContent(String.valueOf(model.getDummyTotalSize())));
		smellList.addContent(new Element("UnprotectedMainProgram").addContent(String.valueOf(model.getUnMainTotalSize())));
		smellList.addContent(new Element("NestedTryBlock").addContent(String.valueOf(model.getNestedTryTotalSize())));
		smellList.addContent(new Element("Total").addContent(String.valueOf(model.getTotalSmellCount())));
		root.addContent(smellList);
	}
	
	/**
	 * ��Code Information�[��XML Root
	 * @param root
	 */
	private void printCodeInfo(Element root) {
		///Code Information List ��ƿ�X///
		Element codeInfoList = new Element("CodeInfoList");
		codeInfoList.addContent(new Element("LOC").addContent(String.valueOf(model.getTotalLine())));
		codeInfoList.addContent(new Element("TryNumber").addContent(String.valueOf(model.getTryCounter())));
		codeInfoList.addContent(new Element("CatchNumber").addContent(String.valueOf(model.getCatchCounter())));
		codeInfoList.addContent(new Element("FinallyNumber").addContent(String.valueOf(model.getFinallyCounter())));
		root.addContent(codeInfoList);
	}
	
	/**
	 * ��Packages�`����ƥ[��XML Root
	 * @param root
	 */
	private void printAllPackageList(Element root) {
		///AllPackage List��ƿ�X///
		Element allPackageList = new Element("AllPackageList");
		for (int i=0; i < model.getPackagesSize(); i++) {
			PackageModel packageModel = model.getPackage(i);

			Element allPackage = new Element("Package");
			//�Ĥ@����ҳs���MPackage�W��
			if (packageModel.getPackageName() == "") {
				allPackage.addContent(new Element("HrefPackageName").addContent("#" + "(default_package)"));
				allPackage.addContent(new Element("PackageName").addContent("(default package)"));
			} else {
				allPackage.addContent(new Element("HrefPackageName").addContent("#" + packageModel.getPackageName()));
				allPackage.addContent(new Element("PackageName").addContent(packageModel.getPackageName()));
			}
			allPackage.addContent(new Element("LOC")
								.addContent(String.valueOf(packageModel.getTotalLine())));
			allPackage.addContent(new Element("IgnoreCheckedException")
								.addContent(String.valueOf(packageModel.getIgnoreSize())));
			allPackage.addContent(new Element("DummyHandler")
								.addContent(String.valueOf(packageModel.getDummySize())));
			allPackage.addContent(new Element("UnprotectedMainProgram")
								.addContent(String.valueOf(packageModel.getUnMainSize())));
			allPackage.addContent(new Element("NestedTryBlock")
								.addContent(String.valueOf(packageModel.getNestedTrySize())));
			allPackage.addContent(new Element("PackageTotal")
								.addContent(String.valueOf(packageModel.getTotalSmellSize())));
			allPackageList.addContent(allPackage);
		}
		///AllPackage List �`�M��ƿ�X///
		Element total = new Element("Total");
		total.addContent(new Element("LOC").addContent(String.valueOf(model.getTotalLine())));
		total.addContent(new Element("IgnoreTotal").addContent(String.valueOf(model.getIgnoreTotalSize())));
		total.addContent(new Element("DummyTotal").addContent(String.valueOf(model.getDummyTotalSize())));
		total.addContent(new Element("UnMainTotal").addContent(String.valueOf(model.getUnMainTotalSize())));
		total.addContent(new Element("NestedTrTotal").addContent(String.valueOf(model.getNestedTryTotalSize())));
		total.addContent(new Element("AllTotal").addContent(String.valueOf(model.getTotalSmellCount())));
		allPackageList.addContent(total);
		root.addContent(allPackageList);
	}
	
	/**
	 * ��Package��ƥ[��XML Root
	 * @param root
	 */
	private void printPackageList(Element root) {
		//���Y�ϡG
		//	PackageList
		//		- Package
		//			- PackageName
		//			- ClassList
		//				-SmellData(�h��)
		//					- ClassName
		//					- MethodName
		//					- SmellType
		//					- Line
		//			- Total
		
		///Package List ��ƿ�X///
		Element packageList= new Element("PackageList");
		for (int i=0; i < model.getPackagesSize(); i++) {
			Element packages = new Element("Package");

			PackageModel pkTemp = model.getPackage(i);
			packages.addContent(new Element("PackageName").addContent(pkTemp.getPackageName()));
			Element classList = new Element("ClassList");
			for (int j=0; j<pkTemp.getClassSize(); j++) {
				ClassModel clTemp = pkTemp.getClass(j);
				//��Smell��T�[��ClassList
				if (clTemp.getSmellSize() > 0) {
					for (int k = 0; k < clTemp.getSmellSize(); k++) {
						Element smell = new Element("SmellData");
						smell.addContent(new Element("ClassName").addContent(clTemp.getClassName()));
						smell.addContent(new Element("State").addContent("0"));
						
						//���s����SourceCode��T�榡
						String codeLine = "#" + clTemp.getClassPath() + "#" + clTemp.getSmellLine(k) + "#";
						smell.addContent(new Element("LinkCode").addContent(codeLine));

						smell.addContent(new Element("MethodName").addContent(clTemp.getMethodName(k)));
						smell.addContent(new Element("SmellType").addContent(clTemp.getSmellType(k).replace("_", " ")));
						smell.addContent(new Element("Line").addContent(String.valueOf(clTemp.getSmellLine(k))));
						classList.addContent(smell);
					}
				//�YClass���S��Smell��ơA�L�XClass�W�١A�ç�Smell��T�]��"None"
				} else {
					Element smell = new Element("SmellData");
					smell.addContent(new Element("ClassName").addContent(clTemp.getClassName()));
					smell.addContent(new Element("State").addContent("1"));
					smell.addContent(new Element("MethodName").addContent("None"));
					smell.addContent(new Element("SmellType").addContent("None"));
					smell.addContent(new Element("Line").addContent("None"));
					classList.addContent(smell);
				}
			}
			packages.addContent(classList);
			packages.addContent(new Element("Total").addContent(String.valueOf(pkTemp.getTotalSmellSize())));
			
			packageList.addContent(packages);
		}
		root.addContent(packageList);
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
	 * ��XHTM�ɪ�Styles.css
	 */
	void createStyles()
	{
		FileWriter fw = null;
		try {
			InputStream inputStyle = this.getClass().getResourceAsStream("/xslTemplate/styles.css");

			BufferedReader bReader = new BufferedReader(new InputStreamReader(inputStyle, "UTF-8"));
			
			File stylePath = new File(model.getFilePath("styles.css", false));
			
			//�Y�S�����|�N�إ߸��|
			if(!stylePath.exists()) {
				fw = new FileWriter(model.getFilePath("styles.css", false));
	
				//��Ū���쪺��ƿ�X
				String thisLine = null;
				while ((thisLine = bReader.readLine()) != null) {
					fw.write(thisLine);
				}
			}

		} catch (IOException ex) {
			logger.error("[IOException] EXCEPTION ",ex);
		} finally {
			if (fw != null)
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
	 * �Q��XML��XSL��������W�A�ò���HTM��
	 * @param xmlString
	 */
	void createHTM(String xmlString) {
		try {
			InputStream inputStream = this.getClass().getResourceAsStream("/xslTemplate/sample.xsl");

			Source xslSource = new StreamSource(inputStream);

			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = null;
			transformer = tf.newTransformer(xslSource);
			Source xmlSource = new StreamSource(new StringReader(xmlString));

			FileOutputStream outputSteam = new FileOutputStream(model.getFilePath("sample.html", true));

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
