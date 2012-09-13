package ntut.csie.rleht.views;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ntut.csie.rleht.common.ASTHandler;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.dom.ITypeBinding;

public class RLMessage {



	// -------------------------------------------------------------------------
	private String key = "";

	private List<String> keyList = null;

	private boolean edited = false;

	// -------------------------------------------------------------------------

	private RLData rlData=new RLData();

	private ITypeBinding typeBinding;

	private String statement;

	private int position;

	private int lineNumber;

	// 是否被處理
	private boolean handling = false;

	// 是否被降級(Tag Level)
	private boolean reduction = false;

	// 是否為Checked Exception
	private boolean checkedException = false;

	// 是否被Catch
	private boolean handleByCatch = false;

	// 註記的Exception清冊位置
	private Map<String, String> handleExMap;

	public RLMessage(int level, ITypeBinding typeBinding, int pos, int lineNumber) {
		this(level, typeBinding, "", pos, lineNumber);
	}

	public RLMessage(int level, ITypeBinding typeBinding, String statement, int pos, int lineNumber) {

		this.statement = statement;
		this.position = pos;
		this.typeBinding = typeBinding;
		this.keyList = new ArrayList<String>();
		this.checkedException = ASTHandler.isCheckedException(typeBinding);
		this.lineNumber = lineNumber;
		this.handleExMap = new HashMap<String, String>();

		rlData.setLevel(level);
		if (typeBinding != null) {
			rlData.setExceptionType(typeBinding.getQualifiedName());
		}

	}

	public ITypeBinding getTypeBinding() {
		return typeBinding;
	}

	public void setTypeBinding(ITypeBinding typeBinding) {
		this.typeBinding = typeBinding;
		if (typeBinding != null) {
			this.rlData.setExceptionType(typeBinding.getQualifiedName());
		}
	}

	/**
	 * @return the statement
	 */
	public String getStatement() {
		return statement;
	}

	/**
	 * @param statement
	 *            the statement to set
	 */
	public void setStatement(String statement) {
		this.statement = statement;
	}

	/**
	 * @return the position
	 */
	public int getPosition() {
		return position;
	}

	/**
	 * @param position
	 *            the position to set
	 */
	public void setPosition(int position) {
		this.position = position;
	}

	/**
	 * @return
	 */
	public boolean isHandling() {
		return handling;
	}

	public boolean isReduction() {
		return reduction;
	}

	public void setReduction(boolean reduction) {
		this.reduction = reduction;
	}

	/**
	 * @param handling
	 */
	public void setHandling(boolean handling) {
		this.handling = handling;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public List<String> getKeyList() {
		return this.keyList;
	}

	public boolean isCheckedException() {
		return checkedException;
	}

	public void setHandleByCatch(boolean handleByCatch) {
		this.handleByCatch = handleByCatch;
	}

	public boolean isHandleByCatch() {
		return handleByCatch;
	}

	public String getKeyString(int pos) {
		if (pos < 0 || pos >= this.keyList.size()) {
			return "";
		}
		StringBuffer newkey = new StringBuffer();

		for (int i = 0; i <= pos; i++) {
			newkey.append(this.keyList.get(i) + "-");
		}
		return newkey.toString();
	}

	public void setKeyList(List<String> keyList) {
		if (keyList == null) {
			return;
		}

		this.keyList = keyList;

		int size = keyList.size();
		if (size > 1 && this.keyList.get(size - 1).equals("0.0")) {
			this.keyList.remove(size - 1);
		}
	}

	/**
	 * 刪除Key Array的最後一個元素
	 * 
	 */
	public void decreaseKeyList() {
		int size = this.keyList.size();
		if (size <= 1) {
			return;
		}

		this.keyList.remove(size - 1);
	}

	public int getKeySize() {

		return this.keyList.size();
	}

	public boolean equalClassType(String superClassName) {
		if (this.typeBinding.getQualifiedName().equals(superClassName)) {
			return true;
		}

		ITypeBinding superTB = this.typeBinding.getSuperclass();
		while (!superTB.getQualifiedName().equals(Object.class.getName())) {
			if (superTB.getQualifiedName().equals(superClassName)) {
				return true;
			}
			superTB = superTB.getSuperclass();
		}

		return false;

	}

	public int getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}

	public boolean isInHandleExMap(String exListPos) {
		return this.handleExMap.get(exListPos) != null;
	}

	public void addHandleExMap(String exListPos) {
		this.handleExMap.put(exListPos, "Y");
	}

	public boolean isEdited() {
		return edited;
	}

	public void setEdited(boolean edited) {
		this.edited = edited;
	}
	
	

	public RLData getRLData() {
		return rlData;
	}

	public void setRLData(RLData rlData) {
		this.rlData = rlData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return this.toControlString() + " ==> LEVEL=[" + this.getRLData().getLevel() + "] EXCEPTION=[" + this.getRLData().getExceptionType()
				+ "] STATEMENT=[" + StringUtils.replace(this.statement, "\n", "") + "] StartPosition=[" + this.position
				+ "] HANDLING=[" + this.handling + "]";
	}

	public String toControlString() {
		return "＠KEY=[" + this.key + "] ＠KEY_SIZE=[" + this.getKeySize() + "] ＠KEY_ARRAY="
				+ this.getKeyString(this.getKeySize() - 1) + "]";
	}


}
