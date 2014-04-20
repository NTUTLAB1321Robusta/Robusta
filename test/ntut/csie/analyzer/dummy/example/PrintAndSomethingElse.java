package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;

public class PrintAndSomethingElse {

	public void rethrowException() throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			System.out.print("SysOutPrint");
			throw e;
		}
	}

	public void uselessIfStatementBeParent() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			if (true) {
				e.printStackTrace();
			}
		}
	}

	public void uselessIfStatementBeSibling() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			System.err.print(e);
			if (true) {
			}
		}
	}

	public void uselessIntDeclared() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			System.out.println(e);
			int i;
		}
	}
}
