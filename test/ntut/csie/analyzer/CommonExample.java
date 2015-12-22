package ntut.csie.analyzer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.log4j.Logger;

public class CommonExample {
	Logger log4j = null;
	java.util.logging.Logger javaLog = null;
	
	public CommonExample() {
		log4j = Logger.getLogger(this.getClass());
		javaLog = java.util.logging.Logger.getLogger("");
	}
	
	public void true_printStackTrace_public() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
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
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
		}
	}
	
	public void true_printStackTrace_private() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
		}
	}

	public void true_systemTrace() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			System.out.println("DummyHandlerExample.true_systemErrPrint()");
		}
	}
	
	public void true_systemErrPrint() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			System.err.println(e);
		}
	}
	
	public void true_systemOutPrint() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			System.out.print(e);
		}
	}
	
	// Dummy handler in initializer
	{
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
		}
	}
	
	/**
	 * to test if there is an expression statement, not a method invocation, exists in catch clause and whether the detection can work 
	 */
	public void true_systemOutPrintlnWithE() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			System.out.println(e);
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
			System.out.println("I am Dummy.");
			// user defined type2 - when meet a template like "*.toString" - true
			e.toString();
		}
	}

	public void true_Log4J() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			log4j.info("message");
		}
	}
	
	public void true_javaLogInfo() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			javaLog.info("");
		}
	}
	
	public void true_javaLogDotLog() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			javaLog.log(Level.INFO, "Just log it.");
		}
	}
	
	public void true_DummyHandlerFinallyNestedTry() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
		} finally {
			try {
				fis.close();//none cc
			} catch (IOException e) { // DummyHandler
				e.printStackTrace();
			}
		}
	}
	
	public void true_IgnoredException() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {	// EmptyCatchBlock
			
		}
	}
	
	public void false_throwAndPrint() throws IOException {
		try {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();//cc
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public void false_throwAndSystemOut() throws IOException {
		try {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();//cc
		} catch (IOException e) {
			System.out.println(e);
			throw e;
		}
	}
	
	public void false_rethrowRuntimeException() {
		try {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();//cc
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
			fis.close();//none cc
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
				fis.close();//cc
			} catch (IOException e) { // DummyHandler
				e.printStackTrace();
			}
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
		}
	}
	
	public void true_DummyHandlerCatchNestedTry() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();
			try {
				fis.close();//none cc
 			} catch (IOException e1) { // DummyHandler
				e1.printStackTrace();
			}
		}
	}
	
	public void false_TryStatementWithoutCatch() {
		try {
		} finally {
		}
	}

	/**
	 * use outer class in catch clause to test user defined pattern 
	 */
	public void false_userPatternType1WhitOuterClass() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			UserDefineDummyHandlerFish userDefineDummyHandlerFish = new UserDefineDummyHandlerFish();
			//user defined type1 - when meet a template like "*[javaFilePath].UserDefineDummyHandlerFish.*" - true
			userDefineDummyHandlerFish.eat();
			/*
			 * user defined type1 - when meet a template like "*[javaFilePath].UserDefineDummyHandlerFish.*" - true
			 * there is a System.out.println() invocation in swim(), but swim() would not be detected as "*.toString()"
			 */
			userDefineDummyHandlerFish.swim();
			/*
			 * user defined type1 - when meet a template like "*[javaFilePath].UserDefineDummyHandlerFish.*" - false
		 	 * this "userDefineDummyHandlerFish.toString()" will not be detected, due to its' .toString() is inherited from Object class.
		 	 * if "userDefineDummyHandlerFish.toString()"'s .toString() is a override method, userDefineDummyHandlerFish.toString() will be detected as "*.toString()".
			 */
			userDefineDummyHandlerFish.toString();
		}
	}
}