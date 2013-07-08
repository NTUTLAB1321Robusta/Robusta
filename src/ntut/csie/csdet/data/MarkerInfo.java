package ntut.csie.csdet.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * �O���C�@��code smell��������T
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
	private ITypeBinding[] methodThrownExceptions;
	private String methodName;
	
	/*
	 * ���FNT�H�~�ثe���γo�ӡA�������אּ�PNT�ۦP
	 */
	public MarkerInfo(String type, ITypeBinding typeBinding, String statement, int pos, int lineNumber, String exceptionType) {
		this.cstype = type;
		this.typeBinding = typeBinding;
		this.statement = statement;
		this.position = pos;
		this.lineNumber = lineNumber;
		this.exceptionType = exceptionType;
		// Method Name ���w�q�A�����Ŧr��
		methodName = "";
	}

	/*
	 * NT �ϥΡA���a Method Name ��T�� Report ��
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
		return methodThrownExceptions;
	}
	
	/**
	 * ��bad smell�O�_�btry statement��
	 * false��ܤ��btry�̭��A true�Ϥ�
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
	
	public void setMethodThrownExceptions(ITypeBinding[] exceptions) {
		this.methodThrownExceptions = exceptions;
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
	 * �ϥΪ̳]�w������i��|�y���P�@��{���X�y����bad smell�Q�O���⦸�C
	 * ���Ƶ{�����b�B�z�o�Ӱ��D�C
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
}
