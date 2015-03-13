package ntut.csie.util.exception;

public class CloseMethodArgumentNotAllInSimpleNameFormException extends ClosingResourceBeginningPositionException {

	private static final long serialVersionUID = 1L;

	public CloseMethodArgumentNotAllInSimpleNameFormException(){
		super();
	}
	
	public CloseMethodArgumentNotAllInSimpleNameFormException(Throwable e){
		super(e);
	}
	
	public CloseMethodArgumentNotAllInSimpleNameFormException(String message){
		super(message);
	}
	
	public CloseMethodArgumentNotAllInSimpleNameFormException(String message, Throwable e){
		super(message, e);
	}

}
