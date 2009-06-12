package ntut.csie.csdet.report;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportModel {
	private List<PackageModel> smellList = new ArrayList<PackageModel>();
	//專案名稱
	private String projectName = "";
	//儲存路徑
	private String projectPath = "";
	//Smell總數
	private int ignoreTotalSize = 0;
	private int dummyTotalSize = 0;
	private int unMainTotalSize = 0;
	private int nestedTryTotalSize = 0;

	/**
	 * 取得建造時間
	 */
	public String getDateTime()
	{
		Locale lo = Locale.TAIWAN;
        Calendar cl= Calendar.getInstance();
        Date d = cl.getTime();
        DateFormat df1 = DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,lo);
        
        return df1.format(d).toString();
	}
	
	//增加Smell的總數
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
	
	//取得Smell的總數
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
	public int getTotalSmellCount() {
		return getIgnoreTotalSize() + getDummyTotalSize() + getUnMainTotalSize() + getNestedTryTotalSize();
	}

	//設定或取得Project的名稱
	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public String getProjectName() {
		return projectName;
	}

	//設定或取得Project的路徑
	public String getProjectPath() {
		return projectPath;
	}
	public void setProjectPath(String projectPath) {
		this.projectPath = projectPath + "/" + getProjectName() + "_Report";
	}

	public PackageModel getPackage(int i) {
		if (i >= smellList.size())
			return null;
		else
			return smellList.get(i);
	}
	public int getPackagesSize() {
		return smellList.size();
	}

	/**
	 * 加入新的Package
	 * @param packageName
	 * @return
	 */
	public PackageModel addSmellList(String packageName) {
//		for (PackageModel pm : smellList) {
//			if (pm.getPackageName().equals(packageName)) {
//				System.out.println("[PackageName]===>"+pm.getPackageName());
//				return pm;
//			}
//		}
		PackageModel newPackageModel = new PackageModel();
		newPackageModel.setPackageName(packageName);
		smellList.add(newPackageModel);
		return newPackageModel;
	}
}
