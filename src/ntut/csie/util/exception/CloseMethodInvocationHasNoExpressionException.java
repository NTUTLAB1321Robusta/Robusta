package ntut.csie.util.exception;

public class CloseMethodInvocationHasNoExpressionException extends ClosingResourceBeginningPositionException {

	private static final long serialVersionUID = -6948127354621083890L;

	public CloseMethodInvocationHasNoExpressionException(){
		super();
	}
	
	public CloseMethodInvocationHasNoExpressionException(Throwable e){
		super(e);
	}
	
	public CloseMethodInvocationHasNoExpressionException(String message){
		super(message);
	}
	
	public CloseMethodInvocationHasNoExpressionException(String message, Throwable e){
		super(message, e);
	}
}
