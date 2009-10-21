package ntut.csie.csdet.data;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.rleht.builder.RLMarkerAttribute;

/**
 * 記錄SuppressSmell的相關資訊
 * @author Shiau
 */
public class SSMessage {
	/** 欲Suppress的Smell名稱 */
	private List<String> smellList = null;
	/** Annotation所在位置 */
	private int position;
	/** Annotation所在行數 */
	private int lineNumber;
	/** 是否在Catch內，否則在Method上 */
	private boolean inCatch = false;
	/** 在Catch內的Index */
	private int catchIdx;
	/** 是否有錯誤名稱 */
	public boolean haveFaultName = false;
	/** 稱錯誤的Smell名稱 */
	private String faultName;
	
	/**
	 * 在Method上建立的Annotation
	 * @param pos
	 * @param lineNumber
	 */
	public SSMessage(int pos, int lineNumber) {
		this.smellList = new ArrayList<String>();
		this.position = pos;
		this.lineNumber = lineNumber;
		
		this.inCatch = false;
	}

	/**
	 * 在Catch內建立的Annotation
	 * @param pos
	 * @param lineNumber
	 * @param catchIdx
	 */
	public SSMessage(int pos, int lineNumber, int catchIdx) {
		this.smellList = new ArrayList<String>();
		this.position = pos;
		this.lineNumber = lineNumber;
		this.catchIdx = catchIdx;
		
		this.inCatch = true;
	}
	/// catch index ///
	public int getCatchIdx() {
		return catchIdx;
	}
	public void setCatchIdx(int catchIdx) {
		this.catchIdx = catchIdx;
	}
	/// Smell List ///
	public List<String> getSmellList() {
		return smellList;
	}
	/**
	 * 加入欲Suppress的Smell名稱
	 * @param smell
	 */
	public void addSmellList(String smell) {
		//若沒有錯誤名稱則偵測，若有錯誤名稱則不偵測
		if (!haveFaultName) {
			boolean isCorrectName = false;
			//判斷是否為正確的Smell名稱
			for (String type : RLMarkerAttribute.CS_TOTAL_TYPE)
				if (smell.equals(type)) {
					isCorrectName = true;
					break;
				}
			//若Smell名稱不正確，則記錄此錯誤Smell名稱
			if (!isCorrectName) {
				faultName = smell;
				haveFaultName = true;
			}
		}
		this.smellList.add(smell);
	}
	/// Annotation Line ///
	public int getLineNumber() {
		return lineNumber;
	}
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	/// Annotation Position ///
	public int getPosition() {
		return position;
	}
	public void setPosition(int position) {
		this.position = position;
	}
	/**
	 * 是否有錯誤名稱
	 * @return
	 */
	public boolean isFaultName() {
		return haveFaultName;
	}
	/**
	 * 取得錯誤的Smell名稱
	 * @return
	 */
	public String getFaultName() {
		return faultName;
	}
	/**
	 * 此Annotation是否在Catch內
	 * @return
	 */
	public boolean isInCatch() {
		return inCatch;
	}
}
