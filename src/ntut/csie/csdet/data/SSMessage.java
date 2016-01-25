package ntut.csie.csdet.data;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.rleht.builder.RLMarkerAttribute;

/**
 * record SuppressSmell information
 * @author Shiau
 */
public class SSMessage {
	/** the smell list which will be suppressed*/
	private List<String> smellList = null;
	/** annotation position */
	private int position;
	/** annotation line number */
	private int lineNumber;
	private boolean isInCatchStatement = false;
	/** Index position inside Catch statement */
	private int catchIdx;

	private boolean isFaultName = false;

	private String isFaultSmellName;
	
	/**
	 * set annotation on method signature
	 * @param pos
	 * @param lineNumber
	 */
	public SSMessage(int pos, int lineNumber) {
		this.smellList = new ArrayList<String>();
		this.position = pos;
		this.lineNumber = lineNumber;
		
		this.isInCatchStatement = false;
	}

	/**
	 * set annotation in catch statement
	 * @param pos
	 * @param lineNumber
	 * @param catchIdx
	 */
	public SSMessage(int pos, int lineNumber, int catchIdx) {
		this.smellList = new ArrayList<String>();
		this.position = pos;
		this.lineNumber = lineNumber;
		this.catchIdx = catchIdx;
		
		this.isInCatchStatement = true;
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
	 * add smell name which will be suppressed
	 * @param smell
	 */
	public void addSmellList(String smell) {
		if (!isFaultName) {
			boolean isCorrectSmellName = false;
			for (String type : RLMarkerAttribute.CS_TOTAL_TYPE)
				if (smell.equals(type)) {
					isCorrectSmellName = true;
					break;
				}
			if (!isCorrectSmellName) {
				isFaultSmellName = smell;
				isFaultName = true;
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
	 * check whether name is correct or not
	 * @return
	 */
	public boolean isFaultName() {
		return isFaultName;
	}

	public String getFaultName() {
		return isFaultSmellName;
	}
	
	public boolean isInsideCatchStatement() {
		return isInCatchStatement;
	}
}
