package ntut.csie.csdet.data;

import java.util.List;

import ntut.csie.robusta.marker.AnnotationInfo;

import org.eclipse.jdt.core.dom.ITypeBinding;

/**
 * record each bad smell information
 * @author chewei
 */

public class MarkerInfo {
	
	private ITypeBinding typeBinding;
	private String className;
	private String statement;
	private String cstype;
	private int position;
	private int lineNumber;
	private String exceptionType;
	private boolean inTry = false;
	private ITypeBinding[] exceptionMethodThrown;
	private String methodName;
	private int methodIndex = -1;
	private int badSmellIndex = 0;
	private boolean supportRefactoring = false;
	private List<AnnotationInfo> annotationInfoList = null;
	
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
	 * we always use this constructor to create MarkerInfo for all bad smells except nested try bad smell  
	 */
	public MarkerInfo(String type, ITypeBinding typeBinding, String statement, int pos, int lineNumber, String exceptionType) {
		this(type, typeBinding, "", statement, pos, lineNumber, exceptionType, "", null);
	}
	
	public MarkerInfo(String type, ITypeBinding typeBinding, String className, String statement, int pos, int lineNumber, String exceptionType, List<AnnotationInfo> annotationPosList) {
		this(type, typeBinding, className, statement, pos, lineNumber, exceptionType, "", annotationPosList);
	}
	
	/*
	 * this constructor is used to create MarkerInfo for nested try bad smell 
	 */
	public MarkerInfo(String type, ITypeBinding typeBinding, String statement,
			int pos, int lineNumber, String exceptionType, String methodName, List<AnnotationInfo> annotationPosList) {
		this.cstype = type;
		this.typeBinding = typeBinding;
		this.statement = statement;
		this.position = pos;
		this.lineNumber = lineNumber;
		this.exceptionType = exceptionType;
		this.methodName = methodName;
		this.annotationInfoList = annotationPosList;
	}
	
	public MarkerInfo(String type, ITypeBinding typeBinding, String className, String statement,
			int pos, int lineNumber, String exceptionType, String methodName, List<AnnotationInfo> annotationPosList) {
		this.cstype = type;
		this.typeBinding = typeBinding;
		this.className = className;
		this.statement = statement;
		this.position = pos;
		this.lineNumber = lineNumber;
		this.exceptionType = exceptionType;
		this.methodName = methodName;
		this.annotationInfoList = annotationPosList;
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
	 * to show whether the bad smell is in the try block 
	 * false means it's in try block
	 * true means  it isn't in try block
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

	public void setPosition(int position) {
		this.position = position;
	}
		
	public void setTypeBinding(ITypeBinding typeBinding) {
		this.typeBinding = typeBinding;
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
	
	public List<AnnotationInfo> getAnnotationList() {
		return this.annotationInfoList;
	}
	
	public String getClassName() {
		return className;
	}
}
