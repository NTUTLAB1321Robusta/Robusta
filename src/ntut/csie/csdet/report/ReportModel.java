package ntut.csie.csdet.report;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Report的相關資料
 * @author Shiau
 */
public class ReportModel {
	//Smell資訊
	private List<PackageModel> smellList = new ArrayList<PackageModel>();
	//Filter條綿是否為全偵測
	private boolean derectAllproject;
	
	private Date buildTime;
	//Filter條件
	private List<String> filterRuleList = new ArrayList<String>();
	//專案名稱
	private String projectName = "";
	//儲存路徑
	private String projectPath = "";
	//Smell總數
	private int ignoreTotalSize = 0;
	private int dummyTotalSize = 0;
	private int unMainTotalSize = 0;
	private int nestedTryTotalSize = 0;
	private int carelessCleanUpSize = 0;
	private int overLoggingSize = 0;
	private int overwrittenSize = 0;
	//取得code counter
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
		//設定格式 顯示秒數
        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.LONG);

        return df.format(buildTime);
	}
	
	///增加Smell的總數///
	public void addIgnoreTotalSize(int ignoreSize) {
		this.ignoreTotalSize += ignoreSize;
	}
	public void addDummyTotalSize(int dummySize) {
		this.dummyTotalSize += dummySize;
	}
	public void addUnMainTotalSize(int unMainSize) {
		this.unMainTotalSize += unMainSize;
	}
	public void addNestedTotalTrySize(int nestedTrySize) {
		this.nestedTryTotalSize += nestedTrySize;
	}
	public void addOverLoggingSize(int overLoggingSize) {
		this.overLoggingSize += overLoggingSize;
	}
	public void addCarelessCleanUpSize(int carelessCleanUpSize) {
		this.carelessCleanUpSize += carelessCleanUpSize;
	}
	public void addOverwrittenSize(int overwrittenSize) {
		this.overwrittenSize += overwrittenSize;
	}
	
	///取得Smell的總數///
	public int getIgnoreTotalSize() {
			return ignoreTotalSize;
	}
	public int getDummyTotalSize() {
			return dummyTotalSize;
	}
	public int getUnMainTotalSize() {
			return unMainTotalSize;
	}
	public int getNestedTryTotalSize() {
			return nestedTryTotalSize;
	}
	public int getOverLoggingTotalSize() {
		return overLoggingSize;
	}
	public int getCarelessCleanUpTotalSize() {
		return carelessCleanUpSize;
	}
	public int getOverwrittenTotalSize() {
		return overwrittenSize;
	}
	public int getTotalSmellCount() {
		return getIgnoreTotalSize() + getDummyTotalSize() + getUnMainTotalSize() + getNestedTryTotalSize()
				+ getCarelessCleanUpTotalSize() + getOverLoggingTotalSize() + getOverwrittenTotalSize();
	}

	///設定或取得Project的名稱///
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public String getProjectName() {
		return projectName;
	}

	///設定或取得Project的路徑///
	public String getProjectPath() {
		return projectPath;
	}
	public void setProjectPath(String workspacePath) {
		this.projectPath = workspacePath + "/" + getProjectName() + "_Report";
		File metadataPath = new File(projectPath);
		//若沒有路徑就建立路徑
		if(!metadataPath.exists())
			metadataPath.mkdir();
		File htmlPath = new File(projectPath + "/" + buildTime.getTime());
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
			return (projectPath + "/" + buildTime.getTime() + "/" + buildTime.getTime() + "_" + fileName);
		else
			return (projectPath + "/" + fileName);
	}

	/**
	 * 取得PackageModel
	 * @param i PackageIndex
	 * @return
	 */
	public PackageModel getPackage(int i) {
		if (i >= smellList.size())
			return null;
		else
			return smellList.get(i);
	}
	/**
	 * 取得Package總數
	 * @return
	 */
	public int getPackagesSize() {
		return smellList.size();
	}

	/**
	 * 加入新的Package
	 * @param packageName
	 * @return
	 */
	public PackageModel addSmellList(String packageName) {
		PackageModel newPackageModel = new PackageModel();
		//設置Package名稱
		newPackageModel.setPackageName(packageName);
		smellList.add(newPackageModel);

		return newPackageModel;
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
	
	///取得全部的行數///
	public int getTotalLine() {
		int total = 0;
		for (PackageModel pm : smellList)
			total += pm.getTotalLine();
		return total;
	}
	
	///存取Code的資訊///
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
}
