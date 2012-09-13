package ntut.csie.filemaker.exceptionBadSmells.OverLogging;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import ntut.csie.robusta.agile.exception.Tag;
import ntut.csie.robusta.agile.exception.Robustness;


public class OverLoggingJavaLogExample {
	Logger javaLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	/** ---------------------Call Chain In Normal Case----------------------------- */
	/* ----------------------Call Chain In the Same Class-------------------------- */
	
	public void theFirstOrderInTheSameClassWithJavaLog() {
		try {
			theSecondOrderInTheSameClassWithJavaLog();
		} catch(IOException e) {
			// Call chain最上層不會標示OverLogging
			javaLogger.log(Level.WARNING, e.getMessage() + "theFirstOrderInTheSameClassWithJavaLog");
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void theSecondOrderInTheSameClassWithJavaLog() throws IOException {
		try {
			theThirdOrderInTheSameClassWithJavaLog();
		} catch(IOException e) {
			// OverLogging
			javaLogger.log(Level.WARNING, e.getMessage() + "theSecondOrderInTheSameClassWithJavaLog");
			throw e;
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void theThirdOrderInTheSameClassWithJavaLog() throws IOException {
		try {
			theFourthOrderInTheSameClassWithJavaLog();
		} catch(IOException e) {
			e.printStackTrace();
			System.out.println(e);
			// 沒有log動作，但是有往上拋，故需要繼續追蹤
			throw e;
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void theFourthOrderInTheSameClassWithJavaLog() throws IOException {
		try {
			throw new IOException("IOException throws in callee");
		} catch(IOException e) {
			e.printStackTrace();
			// OverLogging
			javaLogger.log(Level.WARNING, e.getMessage() + "theFourthOrderInTheSameClassWithJavaLog");
			throw e;
		}
	}

	/* ----------------------Call Chain In the outer Class------------------------- */ 
	
	public void calleeInOutterClassWithJavaLog() {
		try {
			OverLoggingTheFirstOrderClass outer = new OverLoggingTheFirstOrderClass();
			outer.calleeWithJavaLog();
		} catch(IOException e) {
			// Call chain最上層不會標示OverLogging
			javaLogger.log(Level.WARNING, e.getMessage() + "calleeInOutterClassWithJavaLog");
		}
	}
	
	/** -------Call Chain With Transforming Exception And Changing Exception------- */
	/* -----------------------Call Chain In the Same Class------------------------- */
	
	public void theFirstOrderInTheSameClassWithJavaLogAndSomeConditions() {
		try {
			theSecondOrderInTheSameClassWithJavaLogAndSomeConditions();
			theFifthOrderInTheSameClassWithJavaLogAndSomeConditions();
		} catch (IOException e) {
			// Call chain最上層不會標示OverLogging
			javaLogger.log(Level.WARNING, e.getMessage() + "theFirstOrderInTheSameClassWithJavaLogAndSomeConditions");
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void theSecondOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			theThirdOrderInTheSameClassWithJavaLogAndSomeConditions();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			// OverLogging
			javaLogger.log(Level.WARNING, e.getMessage() + "theFifththOrderInTheSameClassWithJavaLogAndSomeConditions");
			throw e;
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void theThirdOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			theFourthOrderInTheSameClassWithJavaLogAndSomeConditions();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			System.out.println(e);
			// 例外轉型，但是有帶入之前的例外資訊，所以要繼續追蹤
			throw new IOException(e);
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void theFourthOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			throw new FileNotFoundException("FileNotFoundException throws in callee");
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			// OverLogging
			javaLogger.log(Level.WARNING, e.getMessage() + "theFourthOrderInTheSameClassWithJavaLogAndSomeConditions");
			throw e;
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void theFifthOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			theSixthOrderInTheSameClassWithJavaLogAndSomeConditions();
		} catch(FileNotFoundException e) {
			javaLogger.log(Level.WARNING, e.getMessage() + "theSecondOrderInTheSameClassWithJavaLogAndSomeConditions");
			// 拋全新的例外，所以不繼續追蹤
			throw new IOException();
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void theSixthOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			throw new FileNotFoundException("FileNotFoundException throws in callee");
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			// OverLogging
			javaLogger.log(Level.WARNING, e.getMessage() + "theFifththOrderInTheSameClassWithJavaLogAndSomeConditions");
			throw e;
		}
	}
	
	/* -----------------------Call Chain In the outer Class------------------------ */ 
	
	public void calleeInOutterClassWithJavaLogAndSomeConditions() {
		try {
			OverLoggingTheFirstOrderClass outer = new OverLoggingTheFirstOrderClass();
			outer.calleeWithJavaLog();
		} catch(IOException e) {
			// Call chain最上層不會標示OverLogging
			javaLogger.log(Level.WARNING, e.getMessage() + "calleeInOutterClassWithJavaLogAndSomeConditions");
		}
	}
}
