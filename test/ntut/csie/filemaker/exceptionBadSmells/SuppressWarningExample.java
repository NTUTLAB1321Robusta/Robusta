package ntut.csie.filemaker.exceptionBadSmells;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;

import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutTryExample;

import agile.exception.RL;
import agile.exception.Robustness;
import agile.exception.SuppressSmell;

public class SuppressWarningExample {
	/*
	 * 1.unprotected main program
	 * 2.dummy handler
	 * 3.nested try block
	 * 4.ignored exception
	 * 5.careless cleanup
	 * 6.�ݸ�
	 * 7.�ݸ�
	 */
	
	/**
	 * �� suppress warning �� unprotected main program
	 */
	@SuppressSmell("Unprotected_Main_Program")
	public static void main(String[] args) {
		UnprotectedMainProgramWithoutTryExample test = new UnprotectedMainProgramWithoutTryExample();
		test.toString();
	}
	
	/**
	 * �b method �W��  suppress warning �� dummy handler
	 */
	@SuppressSmell("Dummy_Handler")
	public void withSuppressWaringDummyHandlerOnMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
		}
	}
	
	/**
	 * �b catch �W��  suppress warning �� dummy handler
	 */
	public void withSuppressWaringDummyHandlerOnCatch() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (@SuppressSmell("Dummy_Handler") IOException e) {
			e.printStackTrace();	//	DummyHandler
		}
	}

	/**
	 * �b catch �W��  suppress warning �� nested try block
	 */
	@SuppressSmell({ "Nested_Try_Block", "Dummy_Handler" })
	public void withSuppressWaringNestedTryBlockOnCatch() {
		try {
			throwSocketTimeoutException();
		} catch (@SuppressSmell("Dummy_Handler") SocketTimeoutException e) {
			try {
				throwInterruptedIOException();
			} catch (@SuppressSmell("Dummy_Handler") InterruptedIOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	/**
	 * �b finally �W��  suppress warning �� nested try block
	 */
	@SuppressSmell({ "Nested_Try_Block", "Dummy_Handler" })
	public void withSuppressWaringNestedTryBlockOnFinally() {
		try {
			throwSocketTimeoutException();
		} catch (@SuppressSmell("Dummy_Handler") SocketTimeoutException e) {
			e.printStackTrace();
		}
		finally {
			try {
				throwInterruptedIOException();
			} catch (@SuppressSmell("Dummy_Handler") InterruptedIOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * �b method �W�� suppress warning �� ignored exception
	 */
	@SuppressSmell("Ignore_Checked_Exception")
	public void withSuppressWaringIgnoredExceptionOnMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {	// IgnoreException
			
		}
	}
	
	/**
	 * �b catch �W�� suppress warning �� ignored exception
	 */
	public void withSuppressWaringIgnoredExceptionOnCatch() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (@SuppressSmell("Ignore_Checked_Exception") IOException e) {	// IgnoreException
			
		}
	}

	/**
	 * �� suppress waring �� careless cleanup
	 */
	@SuppressSmell("Careless_CleanUp")
	@Robustness(value = { @RL(level = 1, exception = java.lang.RuntimeException.class) })
	public void withSuppressWaringCarelessCleanup(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * �� suppress waring �� careless cleanup �b try �i�� close ���ʧ@
	 * @param context
	 * @param outputFile
	 */
	@SuppressSmell("Careless_CleanUp")
	@Robustness(value = { @RL(level = 1, exception = java.lang.RuntimeException.class) })
	public void withSuppressWaringCarelessCleanupCloseInTry(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * �� suppress waring �� careless cleanup �[�W finally block
	 */
	@SuppressSmell("Careless_CleanUp")
	@Robustness(value = { @RL(level = 1, exception = java.lang.RuntimeException.class) })
	public void withSuppressWaringCarelessCleanupCloseInTryAddFinlly(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
	}
	
	private void throwSocketTimeoutException() throws SocketTimeoutException {
		throw new SocketTimeoutException();
	}
	
	private void throwInterruptedIOException() throws InterruptedIOException {
		throw new InterruptedIOException();
	}
}