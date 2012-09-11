package ntut.csie.filemaker.exceptionBadSmells.OverLogging;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agile.exception.RL;
import agile.exception.Robustness;

public class OverLoggingSelf4JExample {
	Logger self4jLogger = LoggerFactory.getLogger(this.getClass());
	
	/** --------------------------------------------------------------------------- *
	 * 								Self4J��@�ϥΪ̦ۭq								*
	 * ---------------------------------------------------------------------------- */
	/** ---------------------Call Chain In Normal Case----------------------------- */
	/* ----------------------Call Chain In the Same Class-------------------------- */
	
	public void theFirstOrderInTheSameClassWithSelf4J() {
		try {
			theSecondOrderInTheSameClassWithSelf4J();
		} catch(IOException e) {
			// Call chain�̤W�h���|�Х�OverLogging
			self4jLogger.error(e.getMessage() + "theFirstOrderInTheSameClassWithSelf4J");
		}
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void theSecondOrderInTheSameClassWithSelf4J() throws IOException {
		try {
			theThirdOrderInTheSameClassWithSelf4J();
		} catch(IOException e) {
			// OverLogging
			self4jLogger.error(e.getMessage() + "theSecondOrderInTheSameClassWithSelf4J");
			throw e;
		}
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void theThirdOrderInTheSameClassWithSelf4J() throws IOException {
		try {
			theFourthOrderInTheSameClassWithSelf4J();
		} catch(IOException e) {
			e.printStackTrace();
			System.out.println(e);
			// �S��log�ʧ@�A���O�����W�ߡA�G�ݭn�~��l��
			throw e;
		}
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void theFourthOrderInTheSameClassWithSelf4J() throws IOException {
		try {
			throw new IOException("IOException throws in callee");
		} catch(IOException e) {
			e.printStackTrace();
			// OverLogging
			self4jLogger.error(e.getMessage() + "theFourthOrderInTheSameClassWithSelf4J");
			throw e;
		}
	}
	/* ----------------------Call Chain In the outer Class------------------------- */ 
	
	public void calleeInOutterClassWithSelf4J() {
		try {
			OverLoggingTheFirstOrderClass outer = new OverLoggingTheFirstOrderClass();
			outer.calleeWithSelf4J();
		} catch(IOException e) {
			// Call chain�̤W�h���|�Х�OverLogging
			self4jLogger.error(e.getMessage() + "calleeInOutterClassWithSelf4J");
		}
	}
	
	/** -------Call Chain With Transforming Exception And Changing Exception------- */
	/* -----------------------Call Chain In the Same Class------------------------- */
	
	public void theFirstOrderInTheSameClassWithJavaLogAndSomeConditions() {
		try {
			theSecondOrderInTheSameClassWithJavaLogAndSomeConditions();
			theFifthOrderInTheSameClassWithJavaLogAndSomeConditions();
		} catch (IOException e) {
			// Call chain�̤W�h���|�Х�OverLogging
			self4jLogger.error(e.getMessage() + "theFirstOrderInTheSameClassWithJavaLogAndSomeConditions");
		}
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void theSecondOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			theThirdOrderInTheSameClassWithJavaLogAndSomeConditions();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			// OverLogging
			self4jLogger.error(e.getMessage() + "theFifththOrderInTheSameClassWithJavaLogAndSomeConditions");
			throw e;
		}
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void theThirdOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			theFourthOrderInTheSameClassWithJavaLogAndSomeConditions();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			System.out.println(e);
			// �ҥ~�૬�A���O���a�J���e���ҥ~��T�A�ҥH�n�~��l��
			throw new IOException(e);
		}
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void theFourthOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			throw new FileNotFoundException("FileNotFoundException throws in callee");
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			// OverLogging
			self4jLogger.error(e.getMessage() + "theFourthOrderInTheSameClassWithJavaLogAndSomeConditions");
			throw e;
		}
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void theFifthOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			theSixthOrderInTheSameClassWithJavaLogAndSomeConditions();
		} catch(FileNotFoundException e) {
			self4jLogger.error(e.getMessage() + "theSecondOrderInTheSameClassWithJavaLogAndSomeConditions");
			// �ߥ��s���ҥ~�A�ҥH���~��l��
			throw new IOException();
		}
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void theSixthOrderInTheSameClassWithJavaLogAndSomeConditions() throws IOException {
		try {
			throw new FileNotFoundException("FileNotFoundException throws in callee");
		} catch(FileNotFoundException e) {
			e.printStackTrace();
			// OverLogging
			self4jLogger.error(e.getMessage() + "theFifththOrderInTheSameClassWithJavaLogAndSomeConditions");
			throw e;
		}
	}
	
	/* -----------------------Call Chain In the outer Class------------------------ */ 
	
	public void calleeInOutterClassWithSelf4JAndSomeConditions() {
		try {
			OverLoggingTheFirstOrderClass outer = new OverLoggingTheFirstOrderClass();
			outer.calleeWithSelf4J();
		} catch(IOException e) {
			// Call chain�̤W�h���|�Х�OverLogging
			self4jLogger.error(e.getMessage() + "calleeInOutterClassWithSelf4JAndSomeConditions");
		}
	}
}
