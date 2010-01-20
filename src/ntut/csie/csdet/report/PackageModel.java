package ntut.csie.csdet.report;

import java.util.ArrayList;
import java.util.List;

/**
 * �s��Package����Report������T
 * @author Shiau
 */
public class PackageModel {
	//�s��Package�W�@�h����Ƨ��W��(ex:src�Btest)
	private String folderName = "";
	//�s��Package���W��
	private String packageName = "";
	//�s��ClassModel
	private List<ClassModel> classModel = new ArrayList<ClassModel>();
	//�s��LOC�ƥ�
	private int totalLine = 0;

	///�s��Package���W��///
	public String getPackageName() {
		//�Y�S����Ƨ��W�١A�h�������Package�W��
		if (folderName != "")
			return folderName + "/" + packageName;
		else
			return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}
	///�s��Folder���W��///
	public String getFolderName() {
		return folderName;
	}
	public void setFolderName(String folderName) {
		this.folderName = folderName;
	}
	
	///�s�WClass���///
	public void addClassModel(ClassModel data) {
		classModel.add(data);
	}
	
	///���oPackage����ClassMethod///
	public int getClassSize() {
		return classModel.size();
	}
	public ClassModel getClass(int i) {
		if (i >= classModel.size())
			return null;
		else
			return classModel.get(i);
	}
		
	///���o��Package��Smell�ƶq///
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
	public int getTotalSmellSize() {
		return getIgnoreSize() + getDummySize() + getNestedTrySize() + getUnMainSize() +
			   getCarelessCleanUpSize() + getOverLoggingSize();
	}
	
	//�s���{����LOC
	public int getTotalLine() {
		return totalLine;
	}
	public void addTotalLine(int countLOC) {
		this.totalLine += countLOC;
	}
}
