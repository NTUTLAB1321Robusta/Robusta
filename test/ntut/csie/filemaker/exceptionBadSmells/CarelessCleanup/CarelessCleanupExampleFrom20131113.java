package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * fileInputStream Example We change the definition of Careless Cleanup at
 * 2013/11 So pig write these new example to make it more understandable
 * 
 * @author pig
 */
public class CarelessCleanupExampleFrom20131113 {

	FileInputStream fileInputStream = null;
	File file = null;
	MethodBeforeCloseExample methodBeforeClose = new MethodBeforeCloseExample();

	public void closeDirectly() throws Exception {
		fileInputStream.close(); // Safe
	}

	public void closeAfterCheckedException() throws Exception {
		methodBeforeClose.declaredCheckedException();
		fileInputStream.close(); // Unsafe
	}

	public void closeAfterUncheckedException() throws Exception {
		methodBeforeClose.declaredUncheckedException();
		fileInputStream.close(); // Unsafe
	}

	public void closeAfterNoDeclaredException() throws Exception {
		methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
		fileInputStream.close(); // Unsafe
	}

	public void closeAfterNoDeclaredException2() throws Exception {
		methodBeforeClose.willNotThrowAnyException();
		fileInputStream.close(); // Unsafe
	}

	public void closeAfterCloser() throws Exception {
		methodBeforeClose.closerWithoutException(fileInputStream);
		/*
		 * Unsafe until other new story be finish
		 * @author pig
		 */
		fileInputStream.close();
	}

	public void createAndCloseDirectlyWithCreatedFile() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		fis.close(); // Safe
	}

	public void createAndCloseDirectlyWithNewFile() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		fis.close(); // Safe
	}

	public void createAndCloseAfterCheckedException() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		methodBeforeClose.declaredCheckedException();
		fis.close(); // Unsafe
	}

	public void createAndCloseAfterUncheckedException() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		methodBeforeClose.declaredUncheckedException();
		fis.close(); // Unsafe
	}

	public void createAndCloseAfterCloser() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		methodBeforeClose.closerWithoutException(fis);
		/*
		 * Unsafe until other new story be finish
		 * @author pig
		 */
		fis.close();
	}

	public void CloseInTryBlockWhichIsFirstStatement() throws Exception {
		try {
			fileInputStream.close(); // Safe
		} catch (IOException e) {
		}
	}

	public void CloseAfterUncheckedExceptionInTryBlock() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
			fis.close(); // Unsafe
		} catch (IOException e) {
		}
	}

	public void CloseInCatchBlock() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.declaredCheckedException();
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
		} catch (IOException e) {
			fis.close(); // Unsafe
		}
	}

	public void CloseInFinallyBlock() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.declaredCheckedException();
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
		} finally {
			fis.close(); // Safe
		}
	}

	public void CloseInFinallyBlockButAfterUncheckedException()
			throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.declaredCheckedException();
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
		} finally {
			methodBeforeClose.declaredUncheckedException();
			fis.close(); // Unsafe
		}
	}

	public void CloseInFinallyBlockButAfterCloser() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.declaredCheckedException();
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
		} finally {
			methodBeforeClose.closerWithoutException(fis);
			/*
			 * Unsafe until other new story be finish
			 * @author pig
			 */
			fis.close(); // Unsafe
		}
	}
}
