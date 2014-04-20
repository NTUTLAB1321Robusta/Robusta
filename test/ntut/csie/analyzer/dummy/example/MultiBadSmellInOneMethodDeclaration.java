package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;

public class MultiBadSmellInOneMethodDeclaration {

	public void method() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
		} finally {
			try {
				fis.close();
			} catch (IOException e) { // DummyHandler
				System.err.println("?");
			}
			
			try {
				fis.close();
			} catch (IOException e) { // DummyHandler
				e.printStackTrace();
			}
		}
	}

}
