package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CarelessCleanupAdvancedExampleFrom20131113 {

	FileInputStream fileInputStream = null;
	File file = null;
	MethodInvocationBeforeClose methodBeforeClose = new MethodInvocationBeforeClose();

	/**
	 * This resource is safe, but the close action in try block is still a bad
	 * smell .
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
	 * The close action will be reach even if any exception be thrown in that
	 * try statement. But there is maybe a exception before the try statement.
	 */
	public void closeInFinallyButSomeStatementBetweenCreateAndTryStatement()
			throws Exception {
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
	 * 
	 * @author pig
	 */
	class SuperCloseable implements Closeable {
		public void close() {
		}
	}

	/**
	 * This close statement should be detected
	 * 
	 * @author pig
	 */
	class ConcreteCloseable extends SuperCloseable {
		public void close() {
			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
			super.close(); // Unsafe
		}
	}

	/**
	 * Method "reset()" will throw exception before the created action, so
	 * close() action will always not careless cleanup
	 */
	public void throwExceptionBeforeCreation(FileOutputStream fos)
			throws IOException {
		fileInputStream.reset();
		int a = 10;
		try {
			fileInputStream = new FileInputStream("path");
			fileInputStream.read();
		} finally {
			fileInputStream.close();
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
	public void closeIsTheFirstExecuteStatementButStillUnsafeWithIfTryStatement()
			throws Exception {
		if (1 == fileInputStream.available()) {
			try {
				fileInputStream.close(); // Unsafe
			} finally {
			}
		}
	}

	public void closeIsTheFirstExecuteStatementButStillUnsafeWithIfStatement()
			throws IOException {
		if (1 == fileInputStream.available()) {
			fileInputStream.close(); // Unsafe
		}
	}

	public void closeIsTheFirstExecuteStatementButStillUnsafeWithForStatement()
			throws IOException {
		for (int i = 0; i < fileInputStream.available(); i++) {
			fileInputStream.close(); // Unsafe
		}
	}

	public void closeIsTheFirstExecuteStatementButStillUnsafeWithDoWhileStatement()
			throws IOException {
		do {
			fileInputStream.close(); // Unsafe
		} while (1 == fileInputStream.available());
	}

	public void doTryFinallyTwice(OutputStream zOut) throws IOException {
		InputStream is = null;
		try {
			zOut.write(is.read());
		} finally {
			is.close(); // Safe
		}
		try {
			zOut.write(is.read());
		} finally {
			is.close(); // Unsafe
		}
	}

	/**
	 * To make sure that the fis won't get the wrong declaration.
	 */
	public void twoVariablesWithSameName(int opcode) throws IOException {
		if (opcode == 1) {
			FileInputStream fis = null;
			fis.read();
		}
		if (opcode == 2) {
			FileInputStream fis = null;
			fis.close(); // Safe
		}
	}

	/**
	 * We shouldn't treat "fileInputStream = new FileInputStream(file);" as the
	 * nearest assignment, because it may not be executed.
	 */
	public void getSuitableAssignment(int opcode) throws IOException {
		if (opcode == 1) {
			fileInputStream = new FileInputStream(file);
			fileInputStream.read();
		}
		try {
			fileInputStream.read();
		} catch (IOException e) {
			System.out.println(e.toString());
		} finally {
			fileInputStream.close(); // Safe
		}
	}

}
