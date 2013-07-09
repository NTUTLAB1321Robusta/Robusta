package ntut.csie.filemaker.exceptionBadSmells;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.nio.channels.FileLockInterruptionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;
import ntut.csie.robusta.agile.exception.SuppressSmell;


public class SuppressWarningExampleForAnalyzer {
	/*
	 * 1.unprotected main program
	 * 2.dummy handler
	 * 3.nested try block
	 * 4.ignored exception
	 * 5.careless cleanup
	 * 6.over logging
	 * 7.�ݸ�
	 */
	
	/* ---------------------- With Suppress warning & Tag Example --------------------- */
	/* ------------------------------------ Start ------------------------------------ */
	
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
	 * �b method �W�� suppress warning �� nested try block
	 */
	@SuppressSmell({ "Nested_Try_Block", "Dummy_Handler" })
	public void withSuppressWaringNestedTryBlockOnMethod() {
		try {
			throwSocketTimeoutException();
		} catch (@SuppressSmell("Dummy_Handler") SocketTimeoutException e) {
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
	 * �b method �W�� suppress warning �� ignored exception
	 */
	@SuppressSmell("Empty_Catch_Block")
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
		} catch (@SuppressSmell("Empty_Catch_Block") IOException e) {	// IgnoreException
			
		}
	}

	/**
	 * �� suppress waring �� careless cleanup
	 */
	@SuppressSmell("Careless_CleanUp")
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void withSuppressWaringCarelessCleanup(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			new FileOutputStream("");
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
	 * �� suppress waring �� careless cleanup �b try �i�� close ���ʧ@
	 * @param context
	 * @param outputFile
	 */
	@SuppressSmell("Careless_CleanUp")
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
	
	/* ---------------------OverLogging And Nested Try Example--------------------- */
	/* -----------------------Call Chain In the Same Class------------------------- */
	
	Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	public void theFirstOrderInTheSameClass() {
		try {
			theSecondOrderInTheSameClass();
		} catch(@SuppressSmell("Dummy_Handler") IOException e) {
			logger.log(Level.WARNING, e.getMessage());
		}
	}
	
	/**
	 * @SuppressSmell("Over_Logging")�b method �W
	 */
	@SuppressSmell("Over_Logging")
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void theSecondOrderInTheSameClass() throws IOException {
		try {
			theThirdOrderInTheSameClass();
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage());
			throw e;
		}
	}
	
	/**
	 * @SuppressSmell("Over_Logging")�b catch �W
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void theThirdOrderInTheSameClass() throws IOException {
		try {
			theFourthOrderInTheSameClass();
		} catch(@SuppressSmell("Over_Logging") IOException e) {
			logger.log(Level.WARNING, e.getMessage());
			throw e;
		}
	}
	
	/**
	 * �b�_�� try-catch �n�b catch �W suppress bad smell ��
	 * ���[�b method �W suppress bad smell �ɥi�H���T���Q suppress
	 */
	@SuppressSmell({ "Careless_CleanUp", "Nested_Try_Block" })
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void theFourthOrderInTheSameClass() throws IOException {
		FileOutputStream fileOutputStream = null;
		FileInputStream fileInputStream = null;
		FileInputStream fis = null;
		try {
			new FileOutputStream("");
			fileOutputStream = new FileOutputStream("");
			fileOutputStream.close();
			throw new IOException("IOException throws in callee");
		} catch(@SuppressSmell({ "Nested_Try_Block" , "Over_Logging" }) FileNotFoundException e) {
			logger.log(Level.WARNING, e.getMessage());
			throw e;
		} catch(@SuppressSmell({ "Nested_Try_Block" , "Over_Logging" , "Empty_Catch_Block"}) FileLockInterruptionException e) {
			
		} catch(@SuppressSmell("Dummy_Handler") IOException e) {
			logger.log(Level.WARNING, e.getMessage());
		} catch (Exception e) {
			try {
				fis = new FileInputStream("");
				fis.read();
				//�T�h
				try {
					fileInputStream = new FileInputStream("");
					fileInputStream.read();
				} catch (IOException e2) {
					throw e2;
				}
			} catch (@SuppressSmell( "Dummy_Handler" ) FileNotFoundException e1) {
				e.printStackTrace();
			} catch (@SuppressSmell({ "Dummy_Handler", "Dummy_Handler" }) IOException e1) {
				e.printStackTrace();
			} catch (@SuppressSmell( "Empty_Catch_Block" ) ArithmeticException e1) {
				// TODO: handle exception
			} catch (@SuppressSmell( { "Empty_Catch_Block", "Empty_Catch_Block" } ) ArrayStoreException  e1) {
				// TODO: handle exception
			} catch (ArrayIndexOutOfBoundsException e1) {
				fileOutputStream.close();
			}
		}
	}
	
	/* ---------------------- With Suppress warning & Tag Example --------------------- */
	/* --------------------------------------- End ----------------------------------- */
	
	/* -------------------- Without Suppress warning & Tag Example -------------------- */
	/* ------------------------------------- Start ----------------------------------- */

	public void withoutSuppressWaringDummyHandlerOnMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
		}
	}

	public void withoutSuppressWaringDummyHandlerOnCatch() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
		}
	}

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

	public void withoutSuppressWaringIgnoredExceptionOnMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {	// IgnoreException
			
		}
	}

	public void withoutSuppressWaringIgnoredExceptionOnCatch() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {	// IgnoreException
			
		}
	}

	public void withoutSuppressWaringCarelessCleanup(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			new FileOutputStream("");
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

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
	
	/* ---------------------OverLogging And Nested Try Example--------------------- */
	/* -----------------------Call Chain In the Same Class------------------------- */
	
	public void withoutSuppressWaringTheFirstOrderInTheSameClass() {
		try {
			withoutSuppressWaringTheSecondOrderInTheSameClass();
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage());
		}
	}

	public void withoutSuppressWaringTheSecondOrderInTheSameClass() throws IOException {
		try {
			withoutSuppressWaringTheThirdOrderInTheSameClass();
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage());
			throw e;
		}
	}

	public void withoutSuppressWaringTheThirdOrderInTheSameClass() throws IOException {
		try {
			withoutSuppressWaringTheFourthOrderInTheSameClass();
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage());
			throw e;
		}
	}
	
	/**
	 * �b�_�� try-catch �n�b catch �W suppress bad smell ��
	 * ���[�b method �W suppress bad smell �ɥi�H���T���Q suppress
	 */
	public void withoutSuppressWaringTheFourthOrderInTheSameClass() throws IOException {
		FileOutputStream fileOutputStream = null;
		FileInputStream fileInputStream = null;
		FileInputStream fis = null;
		try {
			new FileOutputStream("");
			fileOutputStream = new FileOutputStream("");
			fileOutputStream.close();
			throw new IOException("IOException throws in callee");
		} catch(FileNotFoundException e) {
			logger.log(Level.WARNING, e.getMessage());
			throw e;
		} catch(FileLockInterruptionException e) {
			
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage());
		} catch (Exception e) {
			try {
				fis = new FileInputStream("");
				fis.read();
				//�T�h
				try {
					fileInputStream = new FileInputStream("");
					fileInputStream.read();
				} catch (IOException e2) {
					throw e2;
				}
			} catch (FileNotFoundException e1) {
				e.printStackTrace();
			} catch (IOException e1) {
				e.printStackTrace();
			} catch (ArithmeticException e1) {
				// TODO: handle exception
			} catch (ArrayStoreException  e1) {
				// TODO: handle exception
			} catch (ArrayIndexOutOfBoundsException e1) {
				fileOutputStream.close();
			}
		}
	}
	
	/* -------------------- Without Suppress warning & Tag Example -------------------- */
	/* ------------------------------------- End ------------------------------------- */
	
	private void throwSocketTimeoutException() throws SocketTimeoutException{
		throw new SocketTimeoutException();
	}
	
	private void throwInterruptedIOException() throws InterruptedIOException {
		throw new InterruptedIOException();
	}
	
	public void twoExceptionForMethodGetExceptionList()
			throws SocketTimeoutException, InterruptedIOException {
		throw new SocketTimeoutException();
	}
	
	public void multiExceptionForMethodGetExceptionList()
			throws InterruptedIOException, ArithmeticException, Exception {
		throw new InterruptedIOException();
	}

	static int testInt;
	static double testDouble;
	static char testChar;
	static String testString;
	static SuppressWarningExampleForAnalyzer testExample;
	
	public SuppressWarningExampleForAnalyzer(int testInt, double testDouble,
			char testChar, String testString, SuppressWarningExampleForAnalyzer testExample) throws IOException {
		throw new IOException();
	}
	
	public SuppressWarningExampleForAnalyzer(int testInt, double testDouble,
			char testChar, String testString) throws IOException {
		this(testInt, testDouble, testChar, testString, testExample);
	}
	
	public SuppressWarningExampleForAnalyzer(int testInt, double testDouble,
			char testChar) throws IOException {
		this(testInt, testDouble, testChar, testString, testExample);
	}
	
	public SuppressWarningExampleForAnalyzer(int testInt, double testDouble) throws IOException {
		this(testInt, testDouble, testChar, testString, testExample);
	}
	
	public SuppressWarningExampleForAnalyzer(int a) throws IOException {
		this(testInt, testDouble, testChar, testString, testExample);
	}
	
	public SuppressWarningExampleForAnalyzer() throws IOException {
		this(testInt, testDouble, testChar, testString, testExample);
	}
}