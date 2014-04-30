package ntut.csie.csdet.report;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;

public class ClassModel {
	private String className = "";
	private String classPath = "";
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

	public String getClassName() { 
		return className;
	}
	
	public void setClassName(String className) {
		if (className != null)
			this.className = className;
		else
			this.className = "";
	}
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
	
	public String getClassPath() {
		return classPath;
	}
	public void setClassPath(String classPath) {
		this.classPath = classPath + "/" + className;
	}
}
