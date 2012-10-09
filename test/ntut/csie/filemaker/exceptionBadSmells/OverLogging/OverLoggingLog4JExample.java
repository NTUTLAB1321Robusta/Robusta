package ntut.csie.filemaker.exceptionBadSmells.OverLogging;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.log4j.Logger;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

public class OverLoggingLog4JExample {
	Logger log4jLogger = Logger.getLogger(this.getClass());

	/** ---------------------Call Chain In Normal Case------------------------------ */
	/* ----------------------Call Chain In the Same Class--------------------------- */
	
	public void theFirstOrderInTheSameClassWithLog4J() {
		try {
			theSecondOrderInTheSameClassWithLog4J();
		} catch(IOException e) {
			// Call chain最上層不會標示OverLogging
			log4jLogger.error(e.getMessage() + "theFirstOrderInTheSameClassWithLog4J");
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void theSecondOrderInTheSameClassWithLog4J() throws IOException {
		try {
			theThirdOrderInTheSameClassWithLog4J();
		} catch(IOException e) {
			// OverLogging
			log4jLogger.error(e.getMessage() + "theSecondOrderInTheSameClassWithLog4J");
			throw e;
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void theThirdOrderInTheSameClassWithLog4J() throws IOException {
		try {
			theFourthOrderInTheSameClassWithLog4J();
		} catch(IOException e) {
			e.printStackTrace();
			System.out.println(e);
			// 沒有log動作，但是有往上拋，故需要繼續追蹤
			throw e;
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void theFourthOrderInTheSameClassWithLog4J() throws IOException {
		try {
			throw new IOException("IOException throws in callee");
		} catch(IOException e) {
			e.printStackTrace();
			// OverLogging
			log4jLogger.error(e.getMessage() + "theFourthOrderInTheSameClassWithLog4J");
			throw e;
		}
	}
	
	/* ----------------------Call Chain In the outer Class------------------------- */ 
	
	public void calleeInOutterClassWithLog4J() {
		try {
			OverLoggingTheFirstOrderClass outer = new OverLoggingTheFirstOrderClass();
			outer.calleeWithLog4J();
		} catch(IOException e) {
			// Call chain最上層不會標示OverLogging
			log4jLogger.error(e.getMessage() + "calleeInOutterClassWithLog4J");
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
			log4jLogger.error(e.getMessage() + "theFirstOrderInTheSameClassWithJavaLogAndSomeConditions");
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void theSecondOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			theThirdOrderInTheSameClassWithJavaLogAndSomeConditions();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			// OverLogging
			log4jLogger.error(e.getMessage() + "theFifththOrderInTheSameClassWithJavaLogAndSomeConditions");
			throw e;
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void theFourthOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			throw new FileNotFoundException("FileNotFoundException throws in callee");
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			// OverLogging
			log4jLogger.error(e.getMessage() + "theFourthOrderInTheSameClassWithJavaLogAndSomeConditions");
			throw e;
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void theFifthOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			theSixthOrderInTheSameClassWithJavaLogAndSomeConditions();
		} catch(FileNotFoundException e) {
			log4jLogger.error(e.getMessage() + "theSecondOrderInTheSameClassWithJavaLogAndSomeConditions");
			// 拋全新的例外，所以不繼續追蹤
			throw new IOException();
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void theSixthOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			throw new FileNotFoundException("FileNotFoundException throws in callee");
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			// OverLogging
			log4jLogger.error(e.getMessage() + "theFifththOrderInTheSameClassWithJavaLogAndSomeConditions");
			throw e;
		}
	}
	
	/* -----------------------Call Chain In the outer Class------------------------ */ 
	
	public void calleeInOutterClassWithLog4JAndSomeConditions() {
		try {
			OverLoggingTheFirstOrderClass outer = new OverLoggingTheFirstOrderClass();
			outer.calleeWithLog4J();
		} catch(IOException e) {
			// Call chain最上層不會標示OverLogging
			log4jLogger.error(e.getMessage() + "calleeInOutterClassWithLog4JAndSomeConditions");
		}
	}
	
	/* -----------------------------------Others------------------------------------ */
	
	public void topMethod() {
		try {
			bottomMethod();
		} catch (IOException e) {
			log4jLogger.error(e.getMessage() + "topMethod");
		}
	}
	
	public void bottomMethod() throws IOException {
		try {
			throw new FileNotFoundException("bottomMethod");
		} catch (IOException e) {
			throw e;
		}
	}
}
