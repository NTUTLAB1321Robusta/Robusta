package ntut.csie.csdet.data;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.rleht.builder.RLMarkerAttribute;

/**
 * �O��SuppressSmell��������T
 * @author Shiau
 */
public class SSMessage {
	/** ��Suppress��Smell�W�� */
	private List<String> smellList = null;
	/** Annotation�Ҧb��m */
	private int position;
	/** Annotation�Ҧb��� */
	private int lineNumber;
	/** �O�_�bCatch���A�_�h�bMethod�W */
	private boolean inCatch = false;
	/** �bCatch����Index */
	private int catchIdx;
	/** �O�_�����~�W�� */
	public boolean haveFaultName = false;
	/** �ٿ��~��Smell�W�� */
	private String faultName;
	
	/**
	 * �bMethod�W�إߪ�Annotation
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
	 * �bCatch���إߪ�Annotation
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
	 * �[�J��Suppress��Smell�W��
	 * @param smell
	 */
	public void addSmellList(String smell) {
		//�Y�S�����~�W�٫h�����A�Y�����~�W�٫h������
		if (!haveFaultName) {
			boolean isCorrectName = false;
			//�P�_�O�_�����T��Smell�W��
			for (String type : RLMarkerAttribute.CS_TOTAL_TYPE)
				if (smell.equals(type)) {
					isCorrectName = true;
					break;
				}
			//�YSmell�W�٤����T�A�h�O�������~Smell�W��
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
	 * �O�_�����~�W��
	 * @return
	 */
	public boolean isFaultName() {
		return haveFaultName;
	}
	/**
	 * ���o���~��Smell�W��
	 * @return
	 */
	public String getFaultName() {
		return faultName;
	}
	/**
	 * ��Annotation�O�_�bCatch��
	 * @return
	 */
	public boolean isInCatch() {
		return inCatch;
	}
}
