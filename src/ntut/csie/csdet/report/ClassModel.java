package ntut.csie.csdet.report;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.CSMessage;

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
	private List<CSMessage> smellList = new ArrayList<CSMessage>();
	//Smell���Method�W��
	private List<String> methodList = new ArrayList<String>();
	//Smell�ƶq
	private int ignoreExSize = 0;
	private int dummySize = 0;
	private int nestedTrySize = 0;
	private int unMainSize = 0;

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
	public void setIgnoreExList(List<CSMessage> ignoreExList, String MethodName) {
		if(ignoreExList != null) {
			ignoreExSize += ignoreExList.size();
			smellList.addAll(ignoreExList);
			for (int i=0; i<ignoreExList.size(); i++)
				methodList.add(MethodName);
		}
	}
	public void setDummyList(List<CSMessage> dummyList, String MethodName) {
		if (dummyList != null) {
			dummySize += dummyList.size();
			smellList.addAll(dummyList);
			for (int i=0; i<dummyList.size(); i++)
				methodList.add(MethodName);
		}
	}
	public void setNestedTryList(List<CSMessage> nestedTryList, String MethodName) {
		if (nestedTryList != null) {
			nestedTrySize += nestedTryList.size();
			smellList.addAll(nestedTryList);
			for (int i=0; i<nestedTryList.size(); i++)
				methodList.add(MethodName);
		}
	}
	public void setUnprotectedMain(List<CSMessage> unProtectedMain, String MethodName) {
		if (unProtectedMain != null) {
			unMainSize += unProtectedMain.size();
			smellList.addAll(unProtectedMain);
			for (int i=0; i<unProtectedMain.size(); i++)
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
	public int getTotalSmell() {
		return getIgnoreSize() + getDummySize() + getUnMainSize() + getNestedTrySize();
	}
	
	///�s��Class�����|///
	public String getClassPath() {
		return classPath;
	}
	public void setClassPath(String classPath) {
		this.classPath = classPath + "/" + className;
	}
}
