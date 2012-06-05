package ntut.csie.filemaker.exceptionBadSmells;

import java.beans.PropertyVetoException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.zip.DataFormatException;

public class NestedTryBlockExample {
	
	/**
	 * 在catch中出現巢狀try-catch
	 * 內外層try-catch的exception type為Exception下面中的同一子系列
	 */
	public void nestedCatch() {
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
	 * 在finally中出現巢狀try-catch
	 * 內外層try-catch的exception type為Exception下面中的同一子系列
	 */
	public void nestedFinally() {
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
	 * 在catch中出現巢狀try-catch
	 * 內外層try-catch的exception type不為Exception下面中的同一子系列
	 */
	public void nestedCatchWithTopException() {
		try {
			throwDataFormatException();
		} catch (DataFormatException e) {
			try {
				throwPropertyVetoException();
			} catch (PropertyVetoException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	/**
	 * 在finally中出現巢狀try-catch
	 * 內外層try-catch的exception type不為Exception下面中的同一子系列
	 */
	public void nestedFinallyWithTopException() {
		try {
			throwDataFormatException();
		} catch (DataFormatException e) {
			e.printStackTrace();
		}
		finally {
			try {
				throwPropertyVetoException();
			} catch (PropertyVetoException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	private void throwSocketTimeoutException() throws SocketTimeoutException {
		throw new SocketTimeoutException();
	}
	
	private void throwInterruptedIOException() throws InterruptedIOException {
		throw new InterruptedIOException();
	}
	
	private void throwDataFormatException() throws DataFormatException {
		throw new DataFormatException();
	}
	
	private void throwPropertyVetoException() throws PropertyVetoException {
		throw new PropertyVetoException(null, null);
	}
}
