package ntut.csie.analyzer;

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

import ntut.csie.analyzer.unprotected.UnprotectedMainProgramWithoutTryExample;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;
import ntut.csie.robusta.agile.exception.SuppressSmell;


public class SuppressWarningExampleForAnalyzer {
	/*
	 * 1.unprotected main program
	 * 2.dummy handler
	 * 3.nested try statement
	 * 4.empty catch block
	 * 5.careless cleanup
	 * 6.over logging
	 * 7.to be continued...
	 */
	
	/* ---------------------- With Suppress warning & Tag Example --------------------- */
	/* ------------------------------------ Start ------------------------------------ */
	
	/**
	 * a unprotected main program with suppress warning
	 */
	@SuppressSmell("Unprotected_Main_Program")
	public static void main(String[] args) {
		UnprotectedMainProgramWithoutTryExample test = new UnprotectedMainProgramWithoutTryExample();
		test.toString();
	}
	
	@SuppressSmell("Dummy_Handler")
	public void withSuppressWaringDummyHandlerOnMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // Suppressed DummyHandler
			e.printStackTrace();
		}
	}

	public void withSuppressWaringDummyHandlerOnCatch() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (@SuppressSmell("Dummy_Handler") IOException e) { // Suppressed DummyHandler
			e.printStackTrace();
		}
	}

	@SuppressSmell({ "Nested_Try_Statement", "Dummy_Handler" })
	public void withSuppressWaringNestedTryStatementOnCatch() {
		try {
			throwSocketTimeoutException();
		} catch (@SuppressSmell("Dummy_Handler") SocketTimeoutException e) {
			try {
				throwInterruptedIOException();
			} catch (@SuppressSmell("Dummy_Handler") InterruptedIOException e1) { // Suppressed DummyHandler
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
	
	@SuppressSmell({ "Nested_Try_Statement", "Dummy_Handler" })
	public void withSuppressWaringNestedTryStatementOnMethod() {
		try {
			throwSocketTimeoutException();
		} catch (@SuppressSmell("Dummy_Handler") SocketTimeoutException e) { // Suppressed DummyHandler
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

	@SuppressSmell("Empty_Catch_Block")
	public void withSuppressWaringEmptyCatchBlcokionOnMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // Suppressed EmptyCatchBlock

		}
	}

	public void withSuppressWaringEmptyCatchBlockOnCatch() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (@SuppressSmell("Empty_Catch_Block") IOException e) {	// EmptyCatchBlock
			
		}
	}

	@SuppressSmell("Careless_Cleanup")
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
	
	@SuppressSmell("Careless_Cleanup")
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
	
	@SuppressSmell("Careless_Cleanup")
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
		} catch(@SuppressSmell("Dummy_Handler") IOException e) { // Suppressed DummyHandler
			logger.log(Level.WARNING, e.getMessage());
		}
	}
	
	/**
	 * @SuppressSmell("Over_Logging") on method signature
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
	 * @SuppressSmell("Over_Logging")on catch block
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
	
	@SuppressSmell({ "Careless_Cleanup", "Nested_Try_Statement" })
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
		} catch(@SuppressSmell({ "Nested_Try_Statement" , "Over_Logging" }) FileNotFoundException e) {
			logger.log(Level.WARNING, e.getMessage());
			throw e;
		} catch(@SuppressSmell({ "Nested_Try_Statement" , "Over_Logging" , "Empty_Catch_Block"}) FileLockInterruptionException e) {
			
		} catch(@SuppressSmell("Dummy_Handler") IOException e) { // Suppressed DummyHandler
			logger.log(Level.WARNING, e.getMessage());
		} catch (Exception e) {
			try {
				fis = new FileInputStream("");
				fis.read();
				//three level deep
				try {
					fileInputStream = new FileInputStream("");
					fileInputStream.read();
				} catch (IOException e2) {
					throw e2;
				}
			} catch (@SuppressSmell("Dummy_Handler") FileNotFoundException e1) { // Suppressed DummyHandler
				e.printStackTrace();
			} catch (@SuppressSmell({"Dummy_Handler", "Dummy_Handler"}) IOException e1) { // Suppressed DummyHandler
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
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
		}
	}

	public void withoutSuppressWaringDummyHandlerOnCatch() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
		}
	}

	public void withoutSuppressWaringNestedTryStatementOnCatch() {
		try {
			throwSocketTimeoutException();
		} catch (SocketTimeoutException e) {
			try {
				throwInterruptedIOException();
			} catch (InterruptedIOException e1) { // DummyHandler
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}

	public void withoutSuppressWaringNestedTryStatementOnFinally() {
		try {
			throwSocketTimeoutException();
		} catch (SocketTimeoutException e) { // DummyHandler
			e.printStackTrace();
		}
		finally {
			try {
				throwInterruptedIOException();
			} catch (InterruptedIOException e) { // DummyHandler
				e.printStackTrace();
			}
		}
	}

	public void withoutSuppressWaringEmptyCatchBlockOnMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {	// EmptyCatchBlock
			
		}
	}

	public void withoutSuppressWaringEmptyCatchBlockOnCatch() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {	// EmptyCatchBlock
			
		}
	}

	public void withoutSuppressWaringCarelessCleanup(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			new FileOutputStream("");
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();  // CC #1
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
			fileOutputStream.close();  // CC #2
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
			fileOutputStream.close();  // CC #3
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
		} catch(IOException e) { // DummyHandler
			logger.log(Level.WARNING, e.getMessage());
		}
	}

	public void withoutSuppressWaringTheSecondOrderInTheSameClass() throws IOException {
		try {
			withoutSuppressWaringTheThirdOrderInTheSameClass();
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage()); // Over logging
			throw e;
		}
	}

	public void withoutSuppressWaringTheThirdOrderInTheSameClass() throws IOException {
		try {
			withoutSuppressWaringTheFourthOrderInTheSameClass();
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage()); // Over logging
			throw e;
		}
	}
	
	public void withoutSuppressWaringTheFourthOrderInTheSameClass() throws IOException {
		FileOutputStream fileOutputStream = null;
		FileInputStream fileInputStream = null;
		FileInputStream fis = null;
		try {
			new FileOutputStream("");
			fileOutputStream = new FileOutputStream("");
			fileOutputStream.close();  // CC #4
			throw new IOException("IOException throws in callee");
//		} catch(FileNotFoundException e) {
//			logger.log(Level.WARNING, e.getMessage());  // Over logging, if our system can detect FileNotFoundException is a sub class of IOException
//			throw e;
		} catch(FileLockInterruptionException e) {
			
		} catch(IOException e) { // DummyHandler
			logger.log(Level.WARNING, e.getMessage());
		} catch (Exception e) {
			try {
				fis = new FileInputStream("");
				fis.read();
				//three level deep
				try {
					fileInputStream = new FileInputStream("");
					fileInputStream.read();
				} catch (IOException e2) {
					throw e2;
				}
			} catch (FileNotFoundException e1) { // DummyHandler
				e.printStackTrace();
			} catch (IOException e1) { // DummyHandler
				e.printStackTrace();
			} catch (ArithmeticException e1) {
				// TODO: handle exception
			} catch (ArrayStoreException  e1) {
				// TODO: handle exception
			} catch (ArrayIndexOutOfBoundsException e1) {
				fileOutputStream.close();  // CC #5
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