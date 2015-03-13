package ntut.csie.util.exception;

public class ClosingResourceBeginningPositionException extends RuntimeException {

	private static final long serialVersionUID = -2246608676011016932L;

	public ClosingResourceBeginningPositionException(){
		super();
	}
	
	public ClosingResourceBeginningPositionException(Throwable e){
		super(e);
	}
	
	public ClosingResourceBeginningPositionException(String message){
		super(message);
	}
	
	public ClosingResourceBeginningPositionException(String message, Throwable e){
		super(message, e);
	}
}
