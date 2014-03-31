package ntut.csie.csdet.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * 記錄每一種code smell的相關資訊
 * @author chewei
 */

public class MarkerInfo {
	
	private ITypeBinding typeBinding;
	private String statement;
	private String cstype;
	private int position;
	private int lineNumber;
	private String exceptionType;
	private List<String> specialProperty = null;
	private boolean inTry = false;
	private ITypeBinding[] exceptionMethodThrown;
	private String methodName;
	private int methodIndex = -1;
	private int badSmellIndex = 0;
	private boolean supportRefactoring = false;
	
	public boolean isSupportRefactoring() {
		return supportRefactoring;
	}

	public void setSupportRefactoring(boolean supportRefactoring) {
		this.supportRefactoring = supportRefactoring;
	}

	public int getBadSmellIndex() {
		return badSmellIndex;
	}

	public void setBadSmellIndex(int badSmellIndex) {
		this.badSmellIndex = badSmellIndex;
	}

	/*
	 * 除了NT以外目前都用這個，未來應改為與NT相同
	 */
	public MarkerInfo(String type, ITypeBinding typeBinding, String statement, int pos, int lineNumber, String exceptionType) {
		// Method Name 未定義，先給空字串
		this(type, typeBinding, statement, pos, lineNumber, exceptionType, "");
	}

	/*
	 * NT 使用，附帶 Method Name 資訊給 Report 用
	 */
	public MarkerInfo(String type, ITypeBinding typeBinding, String statement,
			int pos, int lineNumber, String exceptionType, String methodName) {
		this.cstype = type;
		this.typeBinding = typeBinding;
		this.statement = statement;
		this.position = pos;
		this.lineNumber = lineNumber;
		this.exceptionType = exceptionType;
		this.methodName = methodName;
	}
	
	public void addSpecialProperty(String str) {
		specialProperty.add(str);
	}
	
	public String getCodeSmellType(){
		return cstype;
	}
	
	public int getLineNumber() {
		return lineNumber;
	}

	public String getExceptionType() {
		return exceptionType;
	}
	/**
	 * @return the statement
	 */
	public String getStatement() {
		return statement;
	}

	public ITypeBinding getTypeBinding() {
		return typeBinding;
	}
	
	/**
	 * @return the position
	 */
	public int getPosition() {
		return position;
	}
	
	public ITypeBinding[] getMethodThrownExceptions() {
		return exceptionMethodThrown;
	}
	
	/**
	 * 此bad smell是否在try statement中
	 * false表示不在try裡面， true反之
	 * @return 
	 */
	public boolean getIsInTry() {
		return inTry;
	}
	
	public void setIsInTry(boolean in) {
		this.inTry = in;
	}
	
	public void setCodeSmellType(String type){
		this.cstype = type;
	}
	
	public void setLineNumber(int lineNumber) {
		this.lineNumber = lineNumber;
	}
	
	public void setExceptionsMethodThrown(ITypeBinding[] exceptions) {
		this.exceptionMethodThrown = exceptions;
	}

	/**
	 * @param statement
	 *            the statement to set
	 */
	public void setStatement(String statement) {
		this.statement = statement;
	}

	/**
	 * @param position
	 *            the position to set
	 */
	public void setPosition(int position) {
		this.position = position;
	}
		
	public void setTypeBinding(ITypeBinding typeBinding) {
		this.typeBinding = typeBinding;
	}

	/**
	 * 使用者設定的條件可能會造成同一行程式碼造成的bad smell被記錄兩次。
	 * 此副程式旨在處理這個問題。
	 * @param nestedTryStatementList
	 * @return
	 */
	public static List<MarkerInfo> RemoveDuplicatedMarkerInfo(List<MarkerInfo> nestedTryStatementList) {
		List<MarkerInfo> rearrangedMarkerInfo = new ArrayList<MarkerInfo>();
		Map<Integer, MarkerInfo> nestedTryStatementHashMap = new HashMap<Integer, MarkerInfo>();
		for (MarkerInfo marker : nestedTryStatementList) {
			if(!nestedTryStatementHashMap.containsKey(marker.getLineNumber())) {
				nestedTryStatementHashMap.put(marker.getLineNumber(), marker);
			}
		}
		
        Iterator<MarkerInfo> iterator = nestedTryStatementHashMap.values().iterator();
        while(iterator.hasNext()) {
            rearrangedMarkerInfo.add((MarkerInfo)iterator.next());
        }
		
		return rearrangedMarkerInfo;
	}
	
	public String getMethodName() {
		return methodName;
	}
	
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public int getMethodIndex() {
		return methodIndex;
	}

	public void setMethodIndex(int methodIndex) {
		this.methodIndex = methodIndex;
	}
	
}
