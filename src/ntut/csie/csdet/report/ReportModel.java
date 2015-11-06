package ntut.csie.csdet.report;

import java.util.ArrayList;
import java.util.List;

public class ReportModel {
	private List<PackageModel> packageModelList = new ArrayList<PackageModel>();
	private String projectName = "";
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
	 * get PackageModel by index
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
	 * get package size
	 * @return
	 */
	public int getPackagesSize() {
		return packageModelList.size();
	}

	public int getTotalLine() {
		int total = 0;
		for (PackageModel pm : packageModelList)
			total += pm.getTotalLine();
		return total;
	}
	
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
