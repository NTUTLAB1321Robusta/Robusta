package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.log4j.lf5.util.Resource;

public class CarelessCleanupAdvancedExample {

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
	 * The close action will be reach if and only if the resource been created.
	 * Because if "declaredCheckedException" throws an exception, the creation
	 * won't be reached.
	 */
	public void closeInFinallyWithStatementOnlyBeforeCreated() throws Exception {
		methodBeforeClose.declaredCheckedException();

		FileInputStream fis = new FileInputStream(file);

		try {
			methodBeforeClose.declaredCheckedException();
		} finally {
			fis.close(); // Safe
		}
	}

	/**
	 * For the concrete closeable below
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
	 * "fileInputStream" will be reassign before it close, and method "reset()"
	 * will throw exception before the assignment. For now, this is a cc example
	 * because we treat the variableDeclaration as the beginning.
	 */
	public void throwExceptionBeforeCreation(FileOutputStream fos)
			throws IOException {
		fileInputStream.reset();
		int a = 10;
		try {
			fileInputStream = new FileInputStream("path");
			fileInputStream.read();
		} finally {
			fileInputStream.close(); // Unsafe
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
	 * Although the try statement before is safe because it catch row type of
	 * Exception and do nothing., but it still a unsafe structure. So it will be
	 * make as careless cleanup.
	 */
	public void closeAfterSafeTryStatement() throws IOException {
		try {
			fileInputStream.available();
		} catch (Exception e) {
			// ignore
		}
		try {
			fileInputStream.close(); // Unsafe
		} finally {
			fileInputStream.close(); // Unsafe
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
	 * About the resource fis. If we treat it's declaration point as beginning, then
	 * it is unsafe. But if we treat it's last assignment as beginning, then it
	 * is safe.
	 * Now we treat it as unsafe when 2013/12/30. 
	 */
	public void variableBeenAssignedTwice(int opcode) throws IOException {
		FileInputStream fis = null;
		if (fis.available() != 1) {
			fis = new FileInputStream(file);
			fis.close(); // Unsafe
		}
	}

	/**
	 * About the resource fis. If we treat it's declaration point as beginning,
	 * then it is unsafe. But if we treat it's first assignment as beginning,
	 * then it is safe.
	 * Now we treat it as unsafe when 2013/12/30.
	 */
	public void variableBeenAssignedAfterDeclared() throws IOException {
		FileInputStream fis;
		file = null;
		fis = new FileInputStream(file);
		fis.close(); // Unsafe
	}

	/**
	 * The instance "new FileInputStream(file)" is safe, but the instance "null"
	 * is unsafe. We should treat it as careless cleanup when it has any unsafe
	 * close.
	 */
	public void variableBeenAssignedTwiceAtDifferentPath(int opcode) throws IOException {
		FileInputStream fis = null;
		if (opcode == 2) {
			fis = new FileInputStream(file);
		}
		fis.close(); // Unsafe
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
			fileInputStream.close(); // Unsafe
		}
	}

	/**
	 * It is an example of a bug on CC rule version 2.
	 * When we detect it, will rise NullPointerException
	 */
	public void arbintest4(Resource resource, OutputStream zOut)
			throws IOException {
		InputStream rIn = resource.getInputStream();
		try {
			rIn.read();
		} finally {
			rIn.close(); // Safe
		}
	}
}
