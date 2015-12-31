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
		methodBeforeClose.declaredCheckedExceptionOnMethodSignature();
		fileInputStream.close(); // Unsafe
	}

	public void closeAfterUncheckedException() throws Exception {
		methodBeforeClose.declaredUncheckedExceptionOnMethodSignature();
		fileInputStream.close(); // Unsafe
	}

	public void closeAfterNoDeclaredException() throws Exception {
		methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
		fileInputStream.close(); // safe
	}
	
	public void closeAfterHasHandledCheckedException() throws Exception {
		methodBeforeClose.hasHandledCheckedException();
		fileInputStream.close(); // safe
	}
	
	public void closeAfterWillNotThrowAnyException() throws Exception{
		methodBeforeClose.willNotThrowAnyException();
		fileInputStream.close(); // safe
	}

	/**
	 * It's the same with example above
	 */
	public void closeAfterNoDeclaredException2() throws Exception {
		methodBeforeClose.willNotThrowAnyException();
		fileInputStream.close(); // safe
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
		methodBeforeClose.declaredCheckedExceptionOnMethodSignature();
		fis.close(); // Unsafe
	}

	public void createAndCloseAfterUncheckedException() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		methodBeforeClose.declaredUncheckedExceptionOnMethodSignature();
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
			methodBeforeClose.declaredUncheckedExceptionOnMethodSignature();
			fis.close(); // Unsafe
		} catch (IOException e) {
		}
	}

	public void closeInCatchBlock() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.declaredCheckedExceptionOnMethodSignature();
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();//according to extra rule of test case, "didNotDeclareAnyExceptionButThrowUnchecked" will takwe as close() method. so this line is unsafe 
		} catch (IOException e) {
			fis.close(); // Safe
		}
	}

	public void closeInFinallyBlock() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.declaredCheckedExceptionOnMethodSignature();
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();//according to extra rule of test case, "didNotDeclareAnyExceptionButThrowUnchecked" will takwe as close() method 
		} finally {
			fis.close(); // Safe
		}
	}

	public void closeInFinallyBlockButAfterUncheckedException()
			throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.declaredCheckedExceptionOnMethodSignature();
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();//according to extra rule of test case, "didNotDeclareAnyExceptionButThrowUnchecked" will takwe as close() method 
		} finally {
			methodBeforeClose.declaredUncheckedExceptionOnMethodSignature();
			fis.close(); // Unsafe
		}
	}
}
