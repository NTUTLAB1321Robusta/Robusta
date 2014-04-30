package ntut.csie.csdet.report;

import java.io.File;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ntut.csie.csdet.preference.RobustaSettings;

public class ReportModel {
	private List<PackageModel> packageModelList = new ArrayList<PackageModel>();
	// Filter條綿是否為全偵測
	private boolean detectAllproject;

	// Filter條件
	private List<String> filterRuleList = new ArrayList<String>();
	// 專案名稱
	private String projectName = "";
	// 取得code counter
	private int tryCounter = 0;
	private int catchCounter = 0;
	private int finallyCounter = 0;


	public void setProjectName(String projectName) {
		this.projectName = projectName;
	}
	public String getProjectName() {
		return projectName;
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
		return detectAllproject;
	}
	public void setDerectAllproject(boolean derectAllproject) {
		this.detectAllproject = derectAllproject;
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
