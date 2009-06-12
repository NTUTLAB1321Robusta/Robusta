package ntut.csie.csdet.report;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;

public class ClassModel {	
	//�s��Class���W��
	private String className = "";
	//�s��Smell��T
	private List<CSMessage> ignoreExList;
	private List<CSMessage> dummyList;
	private List<CSMessage> nestedTryList;
	private List<CSMessage> unprotectedMain;

	///�s��Class���W��///
	public String getClassName() {
		return className;
	}
	public void setClassName(String className) {
		this.className = className;
	}
	
	///�]�w��Class��Smell List///
	public void setIgnoreExList(List<CSMessage> ignoreExList) {
		this.ignoreExList = ignoreExList;
	}
	public void setDummyList(List<CSMessage> dummyList) {
		this.dummyList = dummyList;
	}
	public void setNestedTryList(List<CSMessage> nestedTryList) {
		this.nestedTryList = nestedTryList;
	}
	public void setUnprotectedMain(List<CSMessage> unprotectedMain) {
		this.unprotectedMain = unprotectedMain;
	}
	
	///���o��Class��Smell�ƶq///
	public int getIgnoreSize() {
		if (ignoreExList != null)
			return ignoreExList.size();
		else
			return 0;
	}
	public int getDummySize() {
		if (dummyList != null)
			return dummyList.size();
		else
			return 0;
	}
	public int getUnMainSize() {
		if (unprotectedMain != null)
			return unprotectedMain.size();
		else
			return 0;
	}
	public int getNestedTrySize() {
		if (nestedTryList != null)
			return nestedTryList.size();
		else
			return 0;
	}
	public int getTotalSmell() {
		return getIgnoreSize() + getDummySize() + getUnMainSize() + getNestedTrySize();
	}
}
