package ntut.csie.util.exception;

public class NoAssignmentStatementOrDeclarationInDetectionRangeException extends ClosingResourceBeginningPositionException {

	private static final long serialVersionUID = -3773165747282796763L;

	public NoAssignmentStatementOrDeclarationInDetectionRangeException(){
		super();
	}
	
	public NoAssignmentStatementOrDeclarationInDetectionRangeException(Throwable e){
		super(e);
	}
	
	public NoAssignmentStatementOrDeclarationInDetectionRangeException(String message){
		super(message);
	}
	
	public NoAssignmentStatementOrDeclarationInDetectionRangeException(String message, Throwable e){
		super(message, e);
	}
}
