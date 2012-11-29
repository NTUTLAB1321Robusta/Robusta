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
	//Smell位於的Method名稱
	private List<String> methodList = new ArrayList<String>();
	//Smell數量
	private int ignoreExSize = 0;
	private int dummySize = 0;
	private int nestedTrySize = 0;
	private int unMainSize = 0;
	private int overLoggingSize = 0;
	private int carelessSize = 0;
	private int overwrittenSize = 0;

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
	
	///設定此Class的Smell List///
	public void setIgnoreExList(List<MarkerInfo> ignoreExList, String MethodName) {
		if(ignoreExList != null) {
			ignoreExSize += ignoreExList.size();
			//將Smell與其所在的Method名稱存起來
			smellList.addAll(ignoreExList);
			for (int i=0; i<ignoreExList.size(); i++)
				methodList.add(MethodName);
		}
	}
	public void setDummyList(List<MarkerInfo> dummyList, String MethodName) {
		if (dummyList != null) {
			dummySize += dummyList.size();
			smellList.addAll(dummyList);
			for (int i=0; i<dummyList.size(); i++)
				methodList.add(MethodName);
		}
	}
	public void setNestedTryList(List<MarkerInfo> nestedTryList, String MethodName) {
		if (nestedTryList != null) {
			nestedTrySize += nestedTryList.size();
			smellList.addAll(nestedTryList);
			for (int i=0; i<nestedTryList.size(); i++)
				methodList.add(MethodName);
		}
	}
	public void setUnprotectedMain(List<MarkerInfo> unProtectedMain, String MethodName) {
		if (unProtectedMain != null) {
			unMainSize += unProtectedMain.size();
			smellList.addAll(unProtectedMain);
			for (int i=0; i<unProtectedMain.size(); i++)
				methodList.add(MethodName);
		}
	}
	public void setOverLogging(List<MarkerInfo> overLoggingList, String MethodName) {
		if (overLoggingList != null) {
			overLoggingSize += overLoggingList.size();
			smellList.addAll(overLoggingList);
			for (int i=0; i<overLoggingList.size(); i++)
				methodList.add(MethodName);
		}
	}
	public void setCarelessCleanUp(List<MarkerInfo> carelessList, String MethodName) {
		if (carelessList != null) {
			carelessSize += carelessList.size();
			smellList.addAll(carelessList);
			for (int i=0; i<carelessList.size(); i++)
				methodList.add(MethodName);
		}
	}
	public void setOverwrittenLead(List<MarkerInfo> overwrittenList, String MethodName) {
		if (overwrittenList != null) {
			overwrittenSize += overwrittenList.size();
			smellList.addAll(overwrittenList);
			for (int i=0; i<overwrittenList.size(); i++)
				methodList.add(MethodName);
		}
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
		return methodList.get(i);
	}

	///取得此Class的Smell數量///
	public int getIgnoreSize() {
		return ignoreExSize;
	}
	public int getDummySize() {
		return dummySize;
	}
	public int getUnMainSize() {
		return unMainSize;
	}
	public int getNestedTrySize() {
		return nestedTrySize;
	}
	public int getCarelessCleanUpSize() {
		return carelessSize;
	}
	public int getOverLoggingSize() {
		return overLoggingSize;
	}
	public int getOverwrittenSize() {
		return overwrittenSize;
	}
	public int getTotalSmell() {
		return getIgnoreSize() + getDummySize() + getUnMainSize() + getNestedTrySize() +
			   getCarelessCleanUpSize() + getOverLoggingSize() + getOverwrittenSize();
	}
	
	///存取Class的路徑///
	public String getClassPath() {
		return classPath;
	}
	public void setClassPath(String classPath) {
		this.classPath = classPath + "/" + className;
	}
}
