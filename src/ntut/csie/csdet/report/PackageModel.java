package ntut.csie.csdet.report;

import java.util.ArrayList;
import java.util.List;

/**
 * 存取Package內的Report相關資訊
 * @author Shiau
 */
public class PackageModel {
	//存取Package上一層的資料夾名稱(ex:src、test)
	private String folderName = "";
	//存取Package的名稱
	private String packageName = "";
	//存取ClassModel
	private List<ClassModel> classModel = new ArrayList<ClassModel>();
	//存取LOC數目
	private int totalLine = 0;

	///存取Package的名稱///
	public String getPackageName() {
		//若沒有資料夾名稱，則直接顯示Package名稱
		if (folderName != "")
			return folderName + "/" + packageName;
		else
			return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	///存取Folder的名稱///
	public String getFolderName() {
		return folderName;
	}
	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}
	
	///新增Class資料///
	public void addClassModel(ClassModel data) {
		classModel.add(data);
	}
	
	///取得Package內的ClassMethod///
	public int getClassSize() {
		return classModel.size();
	}
	public ClassModel getClass(int i) {
		if (i >= classModel.size())
			return null;
		else
			return classModel.get(i);
	}
		
	///取得此Package的Smell數量///
	public int getIgnoreSize() {
		int ignoreSize = 0;
		for (ClassModel cm : classModel)
			ignoreSize += cm.getIgnoreSize();

		return ignoreSize;
	}
	public int getDummySize() {
		int dummySize = 0;
		for (ClassModel cm : classModel)
			dummySize += cm.getDummySize();

		return dummySize;
	}
	public int getUnMainSize() {
		int unMainSize = 0;
		for (ClassModel cm : classModel)
			unMainSize += cm.getUnMainSize();

		return unMainSize;
	}
	public int getNestedTrySize() {
		int nestedTrySize = 0;
		for (ClassModel cm : classModel)
			nestedTrySize += cm.getNestedTrySize();

		return nestedTrySize;
	}
	public int getCarelessCleanUpSize() {
		int carelessCleanUpSize = 0;
		for (ClassModel cm : classModel)
			carelessCleanUpSize += cm.getCarelessCleanUpSize();

		return carelessCleanUpSize;
	}
	public int getOverLoggingSize() {
		int overLoggingSize = 0;
		for (ClassModel cm : classModel)
			overLoggingSize += cm.getOverLoggingSize();

		return overLoggingSize;
	}
	public int getOverwrittenSize() {
		int overwrittenSize = 0;
		for (ClassModel cm : classModel)
			overwrittenSize += cm.getOverwrittenSize();

		return overwrittenSize;
	}
	public int getTotalSmellSize() {
		return getIgnoreSize() + getDummySize() + getNestedTrySize() + getUnMainSize() +
			   getCarelessCleanUpSize() + getOverLoggingSize() + getOverwrittenSize();
	}
	
	//存取程式的LOC
	public int getTotalLine() {
		return totalLine;
	}
	public void addTotalLine(int countLOC) {
		this.totalLine += countLOC;
	}
}
