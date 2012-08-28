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

public class WithoutSuppressWarningExample {
	/*
	 * 1.unprotected main program
	 * 2.dummy handler
	 * 3.nested try block
	 * 4.ignored exception
	 * 5.careless cleanup
	 * 6.待補
	 * 7.待補
	 */
	
	/**
	 * 沒有 suppress warning 的 unprotected main program
	 */
	public static void main(String[] args) {
		UnprotectedMainProgramWithoutTryExample test = new UnprotectedMainProgramWithoutTryExample();
		test.toString();
	}
	
	/**在 method 上沒有有  suppress warning 的 dummy handler
	 * 
	 */
	public void withoutSuppressWaringDummyHandlerOnMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
		}
	}
	/**
	 * 在 catch 上沒有  suppress warning 的 dummy handler
	 */
	public void withoutSuppressWaringDummyHandlerOnCatch() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
		}
	}

	/**
	 * 在 catch 上沒有  suppress warning 的 nested try block
	 */
	public void withoutSuppressWaringNestedTryBlockOnCatch() {
		try {
			throwSocketTimeoutException();
		} catch (SocketTimeoutException e) {
			try {
				throwInterruptedIOException();
			} catch (InterruptedIOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	/**
	 * 在 finally 上沒有  suppress warning 的 nested try block
	 */
	public void withoutSuppressWaringNestedTryBlockOnFinally() {
		try {
			throwSocketTimeoutException();
		} catch (SocketTimeoutException e) {
			e.printStackTrace();
		}
		finally {
			try {
				throwInterruptedIOException();
			} catch (InterruptedIOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 在 method 上沒有 suppress warning 的 ignored exception
	 */
	public void withoutSuppressWaringIgnoredExceptionOnMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {	// IgnoreException
			
		}
	}
	
	/**
	 * 在 catch 上沒有 suppress warning 的 ignored exception
	 */
	public void withoutSuppressWaringIgnoredExceptionOnCatch() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {	// IgnoreException
			
		}
	}

	/**
	 * 沒有 suppress waring 的 careless cleanup
	 */
	@Robustness(value = { @RL(level = 1, exception = java.lang.RuntimeException.class) })
	public void withoutSuppressWaringCarelessCleanup(byte[] context, File outputFile) {
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
	 * 沒有 suppress waring 的 careless cleanup 在 try 進行 close 的動作
	 */
	@Robustness(value = { @RL(level = 1, exception = java.lang.RuntimeException.class) })
	public void withoutSuppressWaringCarelessCleanupCloseInTry(byte[] context, File outputFile) {
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
	 * 沒有 suppress waring 的 careless cleanup 加上 finally block
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @RL(level = 1, exception = java.lang.RuntimeException.class) })
	public void withoutSuppressWaringCarelessCleanupCloseInTryAddFinlly(byte[] context, File outputFile) {
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