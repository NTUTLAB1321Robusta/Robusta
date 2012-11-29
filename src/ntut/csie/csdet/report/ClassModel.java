package ntut.csie.csdet.report;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;

/**
 * �x�sClass��Smell��T
 * @author Shiau
 */
public class ClassModel {
	//�s��Class���W��
	private String className = "";
	//�s��Class�����|
	private String classPath = "";
	//����Smell��T
	private List<MarkerInfo> smellList = new ArrayList<MarkerInfo>();
	//Smell���Method�W��
	private List<String> methodList = new ArrayList<String>();
	//Smell�ƶq
	private int ignoreExSize = 0;
	private int dummySize = 0;
	private int nestedTrySize = 0;
	private int unMainSize = 0;
	private int overLoggingSize = 0;
	private int carelessSize = 0;
	private int overwrittenSize = 0;

	///�s��Class���W��///
	public String getClassName() { 
		return className;
	}
	public void setClassName(String className) {
		if (className != null)
			this.className = className;
		else
			this.className = "";
	}
	
	///�]�w��Class��Smell List///
	public void setIgnoreExList(List<MarkerInfo> ignoreExList, String MethodName) {
		if(ignoreExList != null) {
			ignoreExSize += ignoreExList.size();
			//�NSmell�P��Ҧb��Method�W�٦s�_��
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

	///���oClass����Smell��T///
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

	///���o��Class��Smell�ƶq///
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
	
	///�s��Class�����|///
	public String getClassPath() {
		return classPath;
	}
	public void setClassPath(String classPath) {
		this.classPath = classPath + "/" + className;
	}
}
