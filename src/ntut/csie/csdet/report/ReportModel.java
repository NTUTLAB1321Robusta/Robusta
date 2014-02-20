package ntut.csie.csdet.report;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ntut.csie.csdet.preference.RobustaSettings;

/**
 * Report的相關資料
 * @author Shiau
 */
public class ReportModel {
	// Smell資訊
	private List<PackageModel> packageModelList = new ArrayList<PackageModel>();
	// Filter條綿是否為全偵測
	private boolean derectAllproject;

	private Date buildTime;
	// Filter條件
	private List<String> filterRuleList = new ArrayList<String>();
	// 專案名稱
	private String projectName = "";
	// 儲存路徑
	private String projectReportFolderPath = "";
	// 取得code counter
	private int tryCounter = 0;
	private int catchCounter = 0;
	private int finallyCounter = 0;

	/**
	 * 設定、取得建造時間
	 */
	public void setBuildTime() {
		Calendar calendar= Calendar.getInstance();
		buildTime = calendar.getTime();
	}
	public String getBuildTime() {
		// 設定格式 顯示秒數
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.LONG);

        return df.format(buildTime);
	}
	

	// 設定或取得Project的名稱
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public String getProjectName() {
		return projectName;
	}

	// 設定或取得Project的路徑
	public String getProjectPath() {
		return projectReportFolderPath;
	}

	public void setProjectPath(String workspacePath) {
		this.projectReportFolderPath = workspacePath + "/" + RobustaSettings.SETTING_REPORTFOLDERNAME;
		File metadataPath = new File(projectReportFolderPath);
		// 若沒有路徑就建立路徑
		if (!metadataPath.exists())
			metadataPath.mkdir();
		File htmlPath = new File(projectReportFolderPath + "/" + buildTime.getTime());
		htmlPath.mkdir();
	}
	
	/**
	 * 取得File的直實位置(有無加時間區隔)
	 * @param fileName	(File的名稱)
	 * @param isAddTime (是否有加時間)
	 * @return
	 */
	public String getFilePath(String fileName, boolean isAddTime) {
		if (isAddTime)
			return (projectReportFolderPath + "/" + buildTime.getTime() + "/"
					+ buildTime.getTime() + "_" + fileName);
		else
			return (projectReportFolderPath + "/" + fileName);
	}

	public String getRelativeFilePathWithProjectReportPath(String fileName,
			boolean isAddTime) {
		String absolutePath = getFilePath(fileName, isAddTime);
		String relativePath;
		try {
			String projectReportPath = projectReportFolderPath
					+ (isAddTime ? "/" + buildTime.getTime() : "");
			relativePath = (new URI(projectReportPath.replace(" ", "%20"))).relativize(
					new URI(absolutePath.replace(" ", "%20"))).toString();
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage());
		}
		return relativePath;
	}

	/**
	 * 取得PackageModel
	 * @param i PackageIndex
	 * @return
	 */
	public PackageModel getPackage(int i) {
		if (i >= packageModelList.size())
			return null;
		else
			return packageModelList.get(i);
	}
	/**
	 * 取得Package總數
	 * @return
	 */
	public int getPackagesSize() {
		return packageModelList.size();
	}
	
	///存取Filter條件是否為全偵測///
	public boolean isDerectAllproject() {
		return derectAllproject;
	}
	public void setDerectAllproject(boolean derectAllproject) {
		this.derectAllproject = derectAllproject;
	}
	
	///存取Filter條件///
	public List<String> getFilterList() {
		return filterRuleList;
	}
	public void setFilterList(List<String> ruleList) {
		for (String temp: ruleList) {
			temp = temp.replace("EH_STAR", "*");
			temp = temp.replace("EH_LEFT", "");
			temp = temp.replace("EH_RIGHT", "");
			filterRuleList.add(temp);
		}
	}
	
	// 取得全部的行數 //
	public int getTotalLine() {
		int total = 0;
		for (PackageModel pm : packageModelList)
			total += pm.getTotalLine();
		return total;
	}
	
	// 存取Code的資訊 //
	public int getTryCounter() {
		return tryCounter;
	}
	public void addTryCounter(int tryCounter) {
		this.tryCounter += tryCounter;
	}
	public int getCatchCounter() {
		return catchCounter;
	}
	public void addCatchCounter(int catchCounter) {
		this.catchCounter += catchCounter;
	}
	public int getFinallyCounter() {
		return finallyCounter;
	}	
	public void addFinallyCounter(int finallyCounter) {
		this.finallyCounter += finallyCounter;
	}
	public void addPackageModel(PackageModel newPackageModel) {
		packageModelList.add(newPackageModel);
	}
	
	public int getSmellSize(String type) {
		int size = 0;
		for (PackageModel packageModel : packageModelList) {
			size += packageModel.getSmellSize(type);
		}
		return size;
	}
	
	public int getAllSmellSize() {
		int size = 0;
		for (PackageModel packageModel : packageModelList) {
			size += packageModel.getAllSmellSize();
		}
		return size;
	}
}
