package ntut.csie.csdet.data;

import java.util.List;

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
	
	private List<String> specialProperty;
	
	private boolean inTry;
	
	public MarkerInfo(String type, ITypeBinding typeBinding, String statement, int pos, int lineNumber, String exceptionType){
		this.cstype = type;
		this.typeBinding = typeBinding;
		this.statement = statement;
		this.position = pos;
		this.lineNumber = lineNumber;
		this.exceptionType = exceptionType;
		this.specialProperty = null;
		this.inTry = false;
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

}
