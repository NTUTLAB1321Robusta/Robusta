package ntut.csie.csdet.report;

import java.util.ArrayList;
import java.util.List;

/**
 * 存取Package內的Report相關資訊
 * @author Shiau
 */
public class PackageModel {
	//存取Package的名稱
	private String packageName = "";
	//存取ClassModel
	private List<ClassModel> classModel = new ArrayList<ClassModel>();

	///存取Package的名稱///
	public String getPackageName() {
		return packageName;
	}
	public void setPackageName(String packageName) {
		this.packageName = packageName;
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
	public int getTotalSmellSize() {
		return getIgnoreSize() + getDummySize() + getNestedTrySize() + getUnMainSize();
	}
}
