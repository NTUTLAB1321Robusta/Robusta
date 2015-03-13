package ntut.csie.util.exception;

public class ExpressionIsNotInSimpleNameFormException extends ClosingResourceBeginningPositionException {

	private static final long serialVersionUID = 1339138576307234807L;

	public ExpressionIsNotInSimpleNameFormException(){
		super();
	}
	
	public ExpressionIsNotInSimpleNameFormException(Throwable e){
		super(e);
	}
	
	public ExpressionIsNotInSimpleNameFormException(String message){
		super(message);
	}
	
	public ExpressionIsNotInSimpleNameFormException(String message, Throwable e){
		super(message, e);
	}
}
