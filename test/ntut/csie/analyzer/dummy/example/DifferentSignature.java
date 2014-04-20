package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;

public class DifferentSignature {

	public void publicdMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
		}
	}

	protected void protectedMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			System.out.print("SysOutPrint");
		}
	}

	private void privateMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			System.out.print("SysOutPrint");
		}
	}

	private static void staticPrivateMethod() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
		}
	}
}
