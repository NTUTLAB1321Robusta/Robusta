package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class CarelessCleanupAdvancedExampleFrom20131113 {

	FileInputStream fileInputStream = null;
	File file = null;
	MethodBeforeCloseExample methodBeforeClose = new MethodBeforeCloseExample();
	
	/**
	 *  This resource is safe, but the close action in try block is still a bad smell .
	 */
	public void CloseInBothTryBlockAndFinallyBlock() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		try {
			methodBeforeClose.declaredCheckedException();
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
			fis.close(); // Unsafe
		} catch (IOException e) {
			System.out.println(e.toString());
		} finally {
			fis.close(); // Safe
		}
	}

	/**
	 *  The close action will be reach even if any exception be thrown.
	 *  But there is maybe a exception before the try statement.
	 */
	public void CloseInFinallyBlockButSomeStatementBetweenCreateAndTryStatement() throws Exception {
		FileInputStream fis = new FileInputStream(file);
		methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
		try {
			methodBeforeClose.declaredCheckedException();
		} finally {
			fis.close(); // Unsafe
		}
	}

	/**
	 * For the concrete closeable below
	 * @author pig
	 */
	class SuperCloseable implements Closeable {
		public void close() {
		}
	}

	/**
	 * This close statement should be detected
	 * @author pig
	 */
	class ConcreteCloseable extends SuperCloseable {
		public void close() {
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
			super.close(); // Unsafe
		}
	}

	/**
	 *  The close action will be reach even if any exception be thrown.
	 *  But there is maybe a exception before the try statement.
	 */
	public void CloseIsTheFirstExecuteSubStatementButStillUnsafe() throws Exception {
		if(1 == fileInputStream.available()) {
			try {
				fileInputStream.close(); // Unsafe
			} finally {
			}
		}
	}

}
