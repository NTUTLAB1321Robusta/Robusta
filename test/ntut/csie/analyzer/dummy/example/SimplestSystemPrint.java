package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;

public class SimplestSystemPrint {

	public void systemOutPrint() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			System.out.print("SysOutPrint");
		}
	}

	public void systemOutPrintln() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			System.out.println("SysOutPrintln");
		}
	}

	public void systemErrPrint() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			System.err.print(e);
		}
	}

	public void systemErrPrintln() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			System.err.println(e);
		}
	}
}
