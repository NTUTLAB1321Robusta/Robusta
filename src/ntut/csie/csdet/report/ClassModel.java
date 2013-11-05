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
	private int emptyCatchSize = 0;
	private int dummySize = 0;
	private int nestedTrySize = 0;
	private int unMainSize = 0;
	private int overLoggingSize = 0;
	private int carelessSize = 0;
	private int throwsInFinallySize = 0;

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
	public void setEmptyCatchList(List<MarkerInfo> emptyCatchList, String MethodName) {
		if(emptyCatchList != null) {
			emptyCatchSize += emptyCatchList.size();
			//將Smell與其所在的Method名稱存起來
			smellList.addAll(emptyCatchList);
			for (int i=0; i<emptyCatchList.size(); i++)
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
	/**
	 * 將 nestedTryList 加在 ClassModel 收集的 smellList 中
	 * TODO 刪除 methodList 時直接刪除整個 for statement 即可
	 * TODO 其他 list 參考此 Method，將 MethodName 參數拿掉
	 * @param nestedTryList
	 */
	public void addNestedTryList(List<MarkerInfo> nestedTryList) {
		if (nestedTryList != null) {
			nestedTrySize += nestedTryList.size();
			smellList.addAll(nestedTryList);
			for (MarkerInfo markerInfo : nestedTryList) {
				methodList.add(markerInfo.getMethodName());
			}
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
	public void setCarelessCleanup(List<MarkerInfo> carelessList, String MethodName) {
		if (carelessList != null) {
			carelessSize += carelessList.size();
			smellList.addAll(carelessList);
			for (int i=0; i<carelessList.size(); i++)
				methodList.add(MethodName);
		}
	}
	public void setThrowsInFinally(List<MarkerInfo> throwsInFinallyList, String MethodName) {
		if (throwsInFinallyList != null) {
			throwsInFinallySize += throwsInFinallyList.size();
			smellList.addAll(throwsInFinallyList);
			for (int i=0; i<throwsInFinallyList.size(); i++)
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
	/*
	 * TODO MethodName整合到MarkerInfo內，之後可刪除methodList
	 * by pig
	 */
	public String getMethodName(int i) {
		return methodList.get(i);
	}

	///取得此Class的Smell數量///
	public int getEmptySize() {
		return emptyCatchSize;
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
	public int getCarelessCleanupSize() {
		return carelessSize;
	}
	public int getOverLoggingSize() {
		return overLoggingSize;
	}
	public int getThrowsInFinallySize() {
		return throwsInFinallySize;
	}
	public int getTotalSmell() {
		return getEmptySize() + getDummySize() + getUnMainSize() + getNestedTrySize() +
			   getCarelessCleanupSize() + getOverLoggingSize() + getThrowsInFinallySize();
	}
	
	///存取Class的路徑///
	public String getClassPath() {
		return classPath;
	}
	public void setClassPath(String classPath) {
		this.classPath = classPath + "/" + className;
	}
}
