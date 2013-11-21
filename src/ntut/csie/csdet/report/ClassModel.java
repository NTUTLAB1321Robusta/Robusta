package ntut.csie.csdet.report;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;

/**
 * 儲存Class內Smell資訊
 * @author Shiau
 */
public class ClassModel {
	//存取Class的名稱
	private String className = "";
	//存取Class的路徑
	private String classPath = "";
	//全部Smell資訊
	private List<MarkerInfo> smellList = new ArrayList<MarkerInfo>();

	public int getSmellSize(String type) {
		int size = 0;
		for (MarkerInfo markerInfo : smellList) {
			if(markerInfo.getCodeSmellType().equals(type))
				size++;
		}
		return size;
	}
	
	public void addSmellList(List<MarkerInfo> smellList) {
		if(smellList != null)
			this.smellList.addAll(smellList);
	}

	///存取Class的名稱///
	public String getClassName() { 
		return className;
	}
	
	public void setClassName(String className) {
		if (className != null)
			this.className = className;
		else
			this.className = "";
	}
	///取得Class內的Smell資訊///
	public int getSmellSize() {
		return smellList.size();
	}
	public int getSmellLine(int i) {
		return smellList.get(i).getLineNumber();
	}
	public String getSmellType(int i) {
		return smellList.get(i).getCodeSmellType();
	}
	
	public String getMethodName(int i) {
		return smellList.get(i).getMethodName();
	}

	
	///存取Class的路徑///
	public String getClassPath() {
		return classPath;
	}
	public void setClassPath(String classPath) {
		this.classPath = classPath + "/" + className;
	}
}
