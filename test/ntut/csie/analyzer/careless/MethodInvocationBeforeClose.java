package ntut.csie.analyzer.careless;

import java.io.IOException;

public class MethodInvocationBeforeClose {

	public void declaredCheckedExceptionOnMethodSignature() throws IOException {
		throw new IOException();
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
