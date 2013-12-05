package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.IOException;

public class MethodInvocationBeforeClose {

	public void declaredCheckedException() throws IOException {
		throw new IOException();
	}

	public void declaredUncheckedException() throws RuntimeException {
		throw new RuntimeException();
	}

	public void didNotDeclareAnyExceptionButThrowUnchecked() {
		throw new RuntimeException();
	}

	public void willNotThrowAnyException() {
	}

}
