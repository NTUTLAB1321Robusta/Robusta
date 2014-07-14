package ntut.csie.analyzer.careless;

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
	 * try statement. But there is a exception may-been-thrown before this try statement.
	 */
	public void safeCloseInTryButSimpleThrowBeforeTry()
			throws Exception {
		FileInputStream fis = new FileInputStream(file);

		methodBeforeClose.declaredCheckedException();

		try {
			methodBeforeClose.declaredCheckedException();
		} finally {
			fis.close(); // Unsafe
		}
	}

	/**
	 * The close action will be reach even if any exception be thrown in that
	 * try statement. But there is a exception may-been-thrown before this try statement.
	 */
	public void safeCloseInTryButThrowInOtherStatementBeforeTry()
			throws Exception {
		FileInputStream fis = new FileInputStream(file);

		if (true) {
			methodBeforeClose.declaredCheckedException();
		}

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
	public void safeCloseInTryAndThrowInOtherStatementBeenCaught()
			throws Exception {
		FileInputStream fis = new FileInputStream(file);

		/*
		 * The thrown exception already been caught,
		 * but maybe there is other exception to interrupt the close action below.
		 */
		try {
			methodBeforeClose.declaredCheckedException();
		} catch (IOException e) {
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
	public void safeCloseInTryWithDidNotThrowBetweenCreationAndClosed() throws Exception {
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
			methodBeforeClose.declaredUncheckedException();
			super.close(); // Safe for Robusta (Not for human) 
		}
	}

	/**
	 * For Robusta, this is a CC bad smell(Because we start from the
	 * declaration), but it is not for humans.
	 */
	public void variableBeenAssignedAfterDeclared() throws IOException {
		FileInputStream fis;
		fis = new FileInputStream(file);
		fis.close(); // Unsafe for Robusta (Not for human) 
	}

	/**
	 * For Robusta, this is a CC bad smell, but not for humans.
	 */
	public void throwExceptionBeforeAssignment(FileOutputStream fos)
			throws IOException {
		methodBeforeClose.declaredCheckedException();
		
		try {
			fileInputStream = new FileInputStream("path");
			fileInputStream.read();
		} finally {
			fileInputStream.close(); // Unsafe for Robusta (Not for human) 
		}
	}

	/**
	 * There isn't any may-throw-exception before the close action.
	 */
	public void closeIsTheFirstExecuteStatement()
			throws Exception {
		if (fileInputStream != null) {
			try {
				fileInputStream.close(); // Safe
			} finally {
			}
		}
	}

	/**
	 * There is a declaration before the close action on the expression of if
	 * statement.
	 */
	public void closeIsTheFirstExecuteStatementButStillUnsafeWithIfStatement()
			throws IOException {
		if (0 == fileInputStream.available()) {
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
			fileInputStream.close(); // Unsafe after first loop
			fileInputStream = new FileInputStream(file);
		} while (0 == fileInputStream.available());
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

	class ClassWithGetResource implements Closeable {
		public java.nio.channels.Channel getResourceWithInterface() {
			return null;
		}
		public FileOutputStream getResourceWithImp() {
			return null;
		}
		public void close() {
		}
		
		public void closeResourceByInvokeMyClose() throws Exception {
			methodBeforeClose.declaredCheckedException();
			close(); // Unsafe
		}
	}

	public void closeResourceFromGetResourceWithImp() throws Exception {
		ClassWithGetResource resourceManager = new ClassWithGetResource();
		methodBeforeClose.declaredUncheckedException();
		resourceManager.getResourceWithImp().close(); // Unsafe
	}
	
	public void closeResourceFromGetResourceWithInterface() throws Exception {
		ClassWithGetResource resourceManager = new ClassWithGetResource();
		methodBeforeClose.declaredCheckedException();
		resourceManager.getResourceWithInterface().close(); // Unsafe
	}
}
