package ntut.csie.csdet.report;

import java.util.ArrayList;
import java.util.List;

/**
 * access report information in package
 * @author Shiau
 */
public class PackageModel {
	private String folderName = "";
	private String packageName = "";
	private List<ClassModel> classModel = new ArrayList<ClassModel>();
	private int totalLine = 0;

	public String getPackageName() {
		//if folder's name is empty, use package's name to replace it
		if (folderName != "")
			return folderName + "/" + packageName;
		else
			return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	public String getFolderName() {
		return folderName;
	}
	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}
	
	public void addClassModel(ClassModel data) {
		classModel.add(data);
	}
	
	public int getClassSize() {
		return classModel.size();
	}
	public ClassModel getClass(int i) {
		if (i >= classModel.size())
			return null;
		else
			return classModel.get(i);
	}
		
	public int getSmellSize(String type) {
		int size = 0;
		for (ClassModel model : classModel) {
			size += model.getSmellSize(type);
		}
		return size;
	}
	
	public int getAllSmellSize() {
		int size = 0;
		for (ClassModel model : classModel) {
			size += model.getSmellSize();
		}
		return size;
	}
	
	public int getTotalLine() {
		return totalLine;
	}
	public void addTotalLine(int countLOC) {
		this.totalLine += countLOC;
	}
}
