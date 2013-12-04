package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.FileInputStream;
import java.io.IOException;

public class MethodBeforeCloseExample {

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

	public void closerWithoutException(FileInputStream fileInputStream) {
		try {
			fileInputStream.close();
		} catch (Exception e) {
		}
	}
}
