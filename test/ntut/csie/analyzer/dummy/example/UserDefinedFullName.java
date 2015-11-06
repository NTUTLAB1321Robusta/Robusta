package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class UserDefinedFullName {

	/**
	 * testing user defined pattern and testing whether the remove "<>" function of addDummyHandlerSmellInfo is working
	 */
	public void method() {
		ArrayList<Boolean> booleanList = new ArrayList<Boolean>();

		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			// user defined type3 - java.util.ArrayList.add
			booleanList.add(true);
		}
	}
}
