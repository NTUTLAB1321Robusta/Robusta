package ntut.csie.filemaker.exceptionBadSmells;

import java.beans.PropertyVetoException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.zip.DataFormatException;

public class NestedTryBlockExample {
	
	/**
	 * �bcatch���X�{�_��try-catch
	 * ���~�htry-catch��exception type��Exception�U�������P�@�l�t�C
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
	 * �bfinally���X�{�_��try-catch
	 * ���~�htry-catch��exception type��Exception�U�������P�@�l�t�C
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
	 * �bcatch���X�{�_��try-catch
	 * ���~�htry-catch��exception type����Exception�U�������P�@�l�t�C
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
	 * �bfinally���X�{�_��try-catch
	 * ���~�htry-catch��exception type����Exception�U�������P�@�l�t�C
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
