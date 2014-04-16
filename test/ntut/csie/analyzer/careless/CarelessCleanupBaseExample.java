package ntut.csie.analyzer.careless;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Example after We change the definition of Careless Cleanup at
 * 2013/11 So pig write these new example to make it more understandable
 * 
 * @author pig
 */
public class CarelessCleanupBaseExample {

	FileInputStream fileInputStream = null;
	File file = null;
	MethodInvocationBeforeClose methodBeforeClose = new MethodInvocationBeforeClose();

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
		fileInputStream.close(); // Safe
	}

	/**
	 * It's the same with example above
	 */
	public void closeAfterNoDeclaredException2() throws Exception {
		methodBeforeClose.willNotThrowAnyException();
		fileInputStream.close(); // Safe
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

	public void closeInTryBlockWhichIsFirstStatement() throws Exception {
		try {
			fileInputStream.close(); // Safe
		} catch (IOException e) {
		}
	}

	public void closeAfterUncheckedExceptionInTryBlock() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.declaredUncheckedException();
			fis.close(); // Unsafe
		} catch (IOException e) {
		}
	}

	public void closeInCatchBlock() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.declaredCheckedException();
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
		} catch (IOException e) {
			fis.close(); // Safe
		}
	}

	public void closeInFinallyBlock() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.declaredCheckedException();
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
		} finally {
			fis.close(); // Safe
		}
	}

	public void closeInFinallyBlockButAfterUncheckedException()
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
}
