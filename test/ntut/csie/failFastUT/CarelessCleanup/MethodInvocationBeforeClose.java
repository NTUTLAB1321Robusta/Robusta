package ntut.csie.failFastUT.CarelessCleanup;

import java.io.IOException;
import java.sql.SQLException;

public class MethodInvocationBeforeClose {

	public void declaredCheckedExceptionOnMethodSignature() throws SQLException {
		throw new SQLException();
	}
	
	public void hasHandledCheckedException(){
		try{
			throw new IOException();
		}catch(IOException e){
			
		}
	}
	
	public void declaredUncheckedExceptionOnMethodSignature() throws RuntimeException {
		throw new RuntimeException();
	}

	public void didNotDeclareAnyExceptionButThrowUnchecked() {
		throw new RuntimeException();
	}

	public void willNotThrowAnyException() {
	}
}
