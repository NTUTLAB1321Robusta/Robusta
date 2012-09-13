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
	 * �P�ɴ��խY�bcatch���X�{��expression statement���t�����Omethod invocation�ɡA�O�_�ॿ�`�B��
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
			// �ϥΪ̦ۭqtype2 - *.toString�� - true
			e.toString();
			// �ϥΪ̦ۭqtype2 - *.toString�ɴ�*.toCharArray - false
			e.toString().toCharArray();
		}
	}

	/**
	 * ���ըϥΪ̦ۭqtype3
	 * �P�ɴ���addDummyHandlerSmellInfo���h<>�\��O�_�ॿ�`�B��
	 */
	public void true_systemOutAndPrintStack() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			e.printStackTrace();	//	DummyHandler
			System.out.println(e);	//	DummyHandler
			// �ϥΪ̦ۭqtype3 - java.util.ArrayList.add�� - true
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
				e.printStackTrace();	//	�o�Ӥ����ӳQ���@DummyHandler
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
				e.printStackTrace();	//	�o�Ӥ����ӳQ���@DummyHandler
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
				e1.printStackTrace();	//	�o�Ӥ����ӳQ���@DummyHandler
			}
		}
	}
	
	public void false_TryStatementWithoutCatch() {
		try {
		} finally {
		}
	}

	/**
	 * �bcatch���ϥ�outer class�Ӵ��ըϥΪ̦ۭq��pattern
	 */
	public void false_userPatternType1WhitOuterClass() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			UserDefineDummyHandlerFish userDefineDummyHandlerFish = new UserDefineDummyHandlerFish();
			// �ϥΪ̦ۭqtype1 - [javaFilePath].UserDefineDummyHandlerFish.*�� - true
			userDefineDummyHandlerFish.eat();
			/*
			 * �ϥΪ̦ۭqtype1 - [javaFilePath].UserDefineDummyHandlerFish.*�� - true
			 * swim()���h�I�sSystem.out.println�A�����|�Q�u*.toString�v������
			 */
			userDefineDummyHandlerFish.swim();
			/*
			 * �ϥΪ̦ۭqtype1 - [javaFilePath].UserDefineDummyHandlerFish.*�� - false
			 * ���M�ϥ�userDefineDummyHandlerFish�A����method�O�~��Object�Ӫ��A�G���Q�O�J
			 * �Y��userDefineDummyHandlerFish override��method�A�N�|�Q�O�J
			 */
			userDefineDummyHandlerFish.toString();
		}
	}
}