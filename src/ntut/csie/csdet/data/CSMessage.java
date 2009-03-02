package ntut.csie.csdet.data;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * 記錄每一種code smell的相關資訊
 * @author chewei
 */

public class CSMessage {
	
	private ITypeBinding typeBinding;

	private String statement;
	
	private String cstype;

	private int position;

	private int lineNumber;
	
	private String exceptionType;
	
	public CSMessage(String type,ITypeBinding typeBinding, String statement, int pos, int lineNumber,String exceptionType){
		this.cstype = type;
		this.typeBinding = typeBinding;
		this.statement = statement;
		this.position = pos;
		this.lineNumber = lineNumber;
		this.exceptionType = exceptionType;
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
