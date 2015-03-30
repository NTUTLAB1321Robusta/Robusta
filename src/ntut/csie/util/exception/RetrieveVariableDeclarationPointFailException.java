package ntut.csie.util.exception;

public class RetrieveVariableDeclarationPointFailException extends ClosingResourceBeginningPositionException {

	private static final long serialVersionUID = -7088586290082126502L;

	public RetrieveVariableDeclarationPointFailException(){
		super();
	}
	
	public RetrieveVariableDeclarationPointFailException(Throwable e){
		super(e);
	}
	
	public RetrieveVariableDeclarationPointFailException(String message){
		super(message);
	}
	
	public RetrieveVariableDeclarationPointFailException(String message, Throwable e){
		super(message, e);
	}
}
