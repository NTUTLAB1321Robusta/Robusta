package ntut.csie.util.exception;

public class NoAssignmentStatementInDetectionRangeException extends ClosingResourceBeginningPositionException {
	
	private static final long serialVersionUID = 2207464930310228432L;

	public NoAssignmentStatementInDetectionRangeException(){
		super();
	}
	
	public NoAssignmentStatementInDetectionRangeException(Throwable e){
		super(e);
	}
	
	public NoAssignmentStatementInDetectionRangeException(String message){
		super(message);
	}
	
	public NoAssignmentStatementInDetectionRangeException(String message, Throwable e){
		super(message, e);
	}

}
