package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class CarelessCleanupAdvancedExampleFrom20131113 {

	FileInputStream fileInputStream = null;
	File file = null;
	MethodInvocationBeforeClose methodBeforeClose = new MethodInvocationBeforeClose();
	
	/**
	 *  This resource is safe, but the close action in try block is still a bad smell .
	 */
	public void closeInBothTryBlockAndFinallyBlock() throws Exception {
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
	 * The close action will be reach even if any exception be thrown in that
	 * try statement. But there is maybe a exception before the try statement.
	 */
	public void closeInFinallyButStatementBetweenCreateAndTryStatement()
			throws Exception {
		FileInputStream fis = new FileInputStream(file);
		
		methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
		
		try {
			methodBeforeClose.declaredCheckedException();
		} finally {
			fis.close(); // Unsafe
		}
	}

	/**
	 *  The close action will be reach even if any exception be thrown in that try statement.
	 *  But there is maybe a exception before the try statement.
	 */
	public void closeInFinallyButSomeStatementBetweenCreateAndTryStatement() throws Exception {
		FileInputStream fis = new FileInputStream(file);

		/*
		 * Even if this try statement won't throw any exception in truth, we
		 * still treat it as may throw exception
		 */
		try {
			methodBeforeClose.willNotThrowAnyException();
		} catch (Exception e) {
		}
		
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
	 * The close action will always be reached. But because of the limit of
	 * detected rule, it will be consider an unsafe code.
	 */
	public void closeIsTheFirstExecuteStatementThatAlwaysBeExecute()
			throws Exception {
		if (true) {
			try {
				fileInputStream.close(); // Unsafe
			} finally {
			}
		}
	}

	/**
	 * There is maybe an exception before the try statement on the expression on
	 * if statement.
	 */
	public void closeIsTheFirstExecuteStatementButStillUnsafe()
			throws Exception {
		if (1 == fileInputStream.available()) {
			try {
				fileInputStream.close(); // Unsafe
			} finally {
			}
		}
	}

}
