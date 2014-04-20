package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

public class MultiPrintAndLogInOneCatch {

	Logger log4j = null;

	public void method() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			e.printStackTrace();
			System.out.println("Second");
			log4j.info("Third");
		}
	}

}
