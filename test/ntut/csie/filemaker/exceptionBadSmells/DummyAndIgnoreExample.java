package ntut.csie.filemaker.exceptionBadSmells;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import org.apache.log4j.Logger;


public class DummyAndIgnoreExample {
	Logger log4j = null;
	java.util.logging.Logger javaLog = null;
	
	public DummyAndIgnoreExample() {
		log4j = Logger.getLogger(this.getClass());
		javaLog = java.util.logging.Logger.getLogger("");
	}
	
	public void true_printStackTrace_public() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
		}
	}
	
	public void test() {
		log4j.getClass();
		Logger.getRootLogger();
	}
	
	protected void true_printStackTrace_protected() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
		}
	}
	
	public void true_printStackTrace_private() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
		}
	}
	
	public void true_systemTrace() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			System.out.println("DummyHandlerExample.true_systemErrPrint()");	//	DummyHandler
		}
	}
	
	public void true_systemErrPrint() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			System.err.println(e);	//	DummyHandler
		}
	}
	
	public void true_systemOutPrint() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			System.out.print(e);	//	DummyHandler
		}
	}
	
	/**
	 * 同時測試若在catch內出現的expression statement內含的不是method invocation時，是否能正常運行
	 */
	public void true_systemOutPrintlnWithE() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			System.out.println(e);	//	DummyHandler
			String stringToChar = "1001";
			stringToChar.toString().toCharArray();
		}
	}
	
	public void true_systemOutPrintlnWithoutE() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			System.out.println("I am Dummy.");	//	DummyHandler
			// 使用者自訂type2 - *.toString時 - true
			e.toString();
			// 使用者自訂type2 - *.toString時測*.toCharArray - false
			e.toString().toCharArray();
		}
	}

	/**
	 * 測試使用者自訂type3
	 * 同時測試addDummyHandlerSmellInfo的去<>功能是否能正常運行
	 */
	public void true_systemOutAndPrintStack() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
			System.out.println(e);	//	DummyHandler
			// 使用者自訂type3 - java.util.ArrayList.add時 - true
			ArrayList<Boolean> booleanList = new ArrayList<Boolean>();
			booleanList.add(true);
		}
	}
	
	public void true_Log4J() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			log4j.info("message");	//	DummyHandler
		}
	}
	
	public void true_javaLogInfo() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			javaLog.info("");	//	DummyHandler
		}
	}
	
	public void true_javaLogDotLog() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			javaLog.log(Level.INFO, "Just log it.");	//	DummyHandler
		}
	}
	
	public void true_DummyHandlerFinallyNestedTry() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();	//	DummyHandler
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();	//	DummyHandler
			}
		}
	}
	
	public void true_IgnoredException() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {	// IgnoreException
			
		}
	}
	
	public void false_throwAndPrint() throws IOException {
		try {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public void false_throwAndSystemOut() throws IOException {
		try {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();
		} catch (IOException e) {
			System.out.println(e);
			throw e;
		}
	}
	
	public void false_rethrowRuntimeException() {
		try {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void false_systemOut() {
		System.out.println("I am not Dummy Handler.");
	}
	
	public void false_systemOutNotInTryStatement() throws IOException {
		try {
			FileInputStream fis = new FileInputStream("");
			fis.close();
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
		System.out.println("I am not Dummy Handler.");
	}
	
	public void true_DummyHandlerTryNestedTry() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();	//	DummyHandler
			}
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
		}
	}
	
	public void true_DummyHandlerCatchNestedTry() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
			try {
				fis.close();
			} catch (IOException e1) {
				e1.printStackTrace();	//	DummyHandler
			}
		}
	}
	
	public void false_TryStatementWithoutCatch() {
		try {
		} finally {
		}
	}

	/**
	 * 在catch內使用outer class來測試使用者自訂的pattern
	 */
	public void false_userPatternType1WhitOuterClass() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			UserDefineDummyHandlerFish userDefineDummyHandlerFish = new UserDefineDummyHandlerFish();
			// 使用者自訂type1 - [javaFilePath].UserDefineDummyHandlerFish.*時 - true
			userDefineDummyHandlerFish.eat();
			/*
			 * 使用者自訂type1 - [javaFilePath].UserDefineDummyHandlerFish.*時 - true
			 * swim()有去呼叫System.out.println，但不會被「*.toString」偵測到
			 */
			userDefineDummyHandlerFish.swim();
			/*
			 * 使用者自訂type1 - [javaFilePath].UserDefineDummyHandlerFish.*時 - false
			 * 雖然使用userDefineDummyHandlerFish，但此method是繼承Object來的，故不被記入
			 * 若讓userDefineDummyHandlerFish override此method，就會被記入
			 */
			userDefineDummyHandlerFish.toString();
		}
	}
}