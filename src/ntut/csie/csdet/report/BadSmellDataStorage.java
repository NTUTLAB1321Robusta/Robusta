package ntut.csie.csdet.report;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BadSmellDataStorage {
	private static Logger logger = LoggerFactory.getLogger(BadSmellDataStorage.class);
	public static String reportDataFileName = "BSData.xml";
	private String projectReportFolderPath;
	private Date buildTime;

	public BadSmellDataStorage(String projectLocation) {
		setBuildTime();
		setProjectPath(projectLocation);
	}
	
	public void setBuildTime() {
		Calendar calendar= Calendar.getInstance();
		buildTime = calendar.getTime();
	}
	
	public String getBuildTime() {
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.LONG);
        return df.format(buildTime);
	}
	
	private void setProjectPath(String projectLocation) {
		this.projectReportFolderPath = projectLocation + "/" + RobustaSettings.SETTING_REPORTFOLDERNAME;
		File metadataPath = new File(projectReportFolderPath);
		if (!metadataPath.exists())
			metadataPath.mkdir();
		File htmlPath = new File(projectReportFolderPath + "/" + buildTime.getTime());
		htmlPath.mkdir();
	}
	
	public String getRobustaReportPath() {
		return this.projectReportFolderPath;
	}
	
	public String getFilePath(String fileName, boolean isAddTime) {
		if (isAddTime)
			return (projectReportFolderPath + "/" + buildTime.getTime() + "/"
					+ buildTime.getTime() + "_" + fileName);
		else
			return (projectReportFolderPath + "/" + fileName);
	}

	public void save(ReportModel model) {
		if (model != null) {
				Document xmlDoc = createXML(model);
				saveXML(xmlDoc);
		}
	}

	private Document createXML(ReportModel model) {
		Element root = new Element("EHSmellReport");
		Document xmlDocument = new Document(root);

		printSummary(root, model);
		printCodeInfo(root, model);
		printAllPackageList(root, model);
		printPackageList(root, model);
		
		return xmlDocument;
	}
	
	private void saveXML(Document xmlDocument) {
		Format fmt = Format.getPrettyFormat();
		fmt.setEncoding("UTF-8"); //Default already, but just make sure again ^^
		XMLOutputter xmlOut = new XMLOutputter(fmt);
		OutputStreamWriter outputWriter = null;
		try {
			String path = getFilePath(reportDataFileName, true);
			outputWriter = new OutputStreamWriter(new FileOutputStream(path), "UTF-8");
			//fileWriter = new FileWriter(path);
			xmlOut.output(xmlDocument, outputWriter);
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		} finally {
			closeStream(outputWriter);
		}
	}
	
	public String getResultDataPath() {
		return getFilePath(reportDataFileName, true);
	}
	
	private void printSummary(Element root, ReportModel model) {
		Element summary = new Element("Summary");
		summary.addContent(new Element("ProjectName").addContent(model
				.getProjectName()));
		summary.addContent(new Element("DateTime").addContent(this.getBuildTime()));
		root.addContent(summary);

		Element smellList = new Element("EHSmellList");
		smellList.addContent(new Element("EmptyCatchBlock").addContent(String
				.valueOf(model.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK))));
		smellList.addContent(new Element("DummyHandler").addContent(String
				.valueOf(model.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER))));
		smellList.addContent(new Element("UnprotectedMainProgram")
				.addContent(String.valueOf(model.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN))));
		smellList.addContent(new Element("NestedTryStatement").addContent(String
				.valueOf(model.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT))));
		smellList.addContent(new Element("CarelessCleanup").addContent(String
				.valueOf(model.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP))));
		smellList.addContent(new Element("OverLogging").addContent(String
				.valueOf(model.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING))));
		smellList.addContent(new Element("ThrownExceptionInFinallyBlock")
				.addContent(String.valueOf(model.getSmellSize(RLMarkerAttribute.CS_THROWN_EXCEPTION_IN_FINALLY_BLOCK))));
		smellList.addContent(new Element("Total").addContent(String
				.valueOf(model.getAllSmellSize())));
		root.addContent(smellList);
	}

	private void printCodeInfo(Element root, ReportModel model) {
		Element codeInfoList = new Element("CodeInfoList");
		codeInfoList.addContent(new Element("LOC").addContent(String
				.valueOf(model.getTotalLine())));
		codeInfoList.addContent(new Element("TryNumber").addContent(String
				.valueOf(model.getTryCounter())));
		codeInfoList.addContent(new Element("CatchNumber").addContent(String
				.valueOf(model.getCatchCounter())));
		codeInfoList.addContent(new Element("FinallyNumber").addContent(String
				.valueOf(model.getFinallyCounter())));
		root.addContent(codeInfoList);
	}

	private void printAllPackageList(Element root, ReportModel model) {
		Element allPackageList = new Element("AllPackageList");
		List<Element> packageList = new ArrayList<Element>();
		for (int i = 0; i < model.getPackagesSize(); i++) {
			PackageModel packageModel = model.getPackage(i);
			Element packages = new Element("Package");
			packages.addContent(new Element("ID").addContent(String.valueOf(i)));
			packages.addContent(new Element("LOC").addContent(String
					.valueOf(packageModel.getTotalLine())));
			packages.addContent(new Element("EmptyCatchBlock")
					.addContent(String.valueOf(packageModel.getSmellSize(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK))));
			packages.addContent(new Element("DummyHandler").addContent(String
					.valueOf(packageModel.getSmellSize(RLMarkerAttribute.CS_DUMMY_HANDLER))));
			packages.addContent(new Element("UnprotectedMainProgram")
					.addContent(String.valueOf(packageModel.getSmellSize(RLMarkerAttribute.CS_UNPROTECTED_MAIN))));
			packages.addContent(new Element("NestedTryStatement").addContent(String
					.valueOf(packageModel.getSmellSize(RLMarkerAttribute.CS_NESTED_TRY_STATEMENT))));
			packages.addContent(new Element("CarelessCleanup")
					.addContent(String.valueOf(packageModel
							.getSmellSize(RLMarkerAttribute.CS_CARELESS_CLEANUP))));
			packages.addContent(new Element("OverLogging").addContent(String
					.valueOf(packageModel.getSmellSize(RLMarkerAttribute.CS_OVER_LOGGING))));
			packages.addContent(new Element("ThrownExceptionInFinallyBlock")
					.addContent(String.valueOf(packageModel
							.getSmellSize(RLMarkerAttribute.CS_THROWN_EXCEPTION_IN_FINALLY_BLOCK))));
			packages.addContent(new Element("PackageTotal").addContent(String
					.valueOf(packageModel.getAllSmellSize())));
			if (packageModel.getAllSmellSize() > 0) {
				if (packageModel.getPackageName() == "") {
					packages.addContent(new Element("HrefPackageName")
							.addContent("#" + "(default_package)"));
					packages.addContent(new Element("PackageName")
							.addContent("(default package)"));
				} else {
					packages.addContent(new Element("HrefPackageName")
							.addContent("#" + packageModel.getPackageName()));
					packages.addContent(new Element("PackageName")
							.addContent(packageModel.getPackageName()));
				}
				allPackageList.addContent(packages);
			} else {
				if (packageModel.getPackageName() == "")
					packages.addContent(new Element("PackageName")
							.addContent("(default package)"));
				else
					packages.addContent(new Element("PackageName")
							.addContent(packageModel.getPackageName()));
				packages.addContent(new Element("HrefPackageName")
						.addContent("#Package_List"));
				packageList.add(packages);
			}
		}
		allPackageList.addContent(packageList);
		root.addContent(allPackageList);
	}

	private void printPackageList(Element root, ReportModel model) {
		Element packageList = new Element("PackageList");
		for (int i = 0; i < model.getPackagesSize(); i++) {
			Element packages = new Element("Package");
			PackageModel pkTemp = model.getPackage(i);
			if (pkTemp.getAllSmellSize() > 0) {
				packages.addContent(new Element("PackageName")
						.addContent(pkTemp.getPackageName()));
				Element classList = new Element("ClassList");
				for (int j = 0; j < pkTemp.getClassSize(); j++) {
					ClassModel clTemp = pkTemp.getClass(j);
					if (clTemp.getSmellSize() > 0) {
						for (int k = 0; k < clTemp.getSmellSize(); k++) {
							Element smell = new Element("SmellData");
							smell.addContent(new Element("ClassName")
									.addContent(clTemp.getClassName()));
							smell.addContent(new Element("State")
									.addContent("0"));
							String codeLine = "#" + clTemp.getClassPath() + "#"
									+ clTemp.getSmellLine(k) + "#";
							smell.addContent(new Element("LinkCode")
									.addContent(codeLine));
							smell.addContent(new Element("MethodName")
									.addContent(clTemp.getMethodName(k)));
							smell.addContent(new Element("SmellType")
									.addContent(clTemp.getSmellType(k).replace(
											"_", " ")));
							smell.addContent(new Element("Line")
									.addContent(String.valueOf(clTemp
											.getSmellLine(k))));
							classList.addContent(smell);
						}
					}
				}
				packages.addContent(classList);
				packages.addContent(new Element("Total").addContent(String
						.valueOf(pkTemp.getAllSmellSize())));
				packageList.addContent(packages);
			}
		}
		root.addContent(packageList);
	}
	
	void closeStream(Closeable io) {
		try {
			if (io != null)
				io.close();
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		}
	}
}
