package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;

public class UserDefinedMethod {

	public void mathodToString() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			// DummyHandler when user defined toString
			e.toString();
		}
	}

	public void mathodToCharArray() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			// DummyHandler when user defined toCharArray
			e.toString().toCharArray();
		}
	}

}
