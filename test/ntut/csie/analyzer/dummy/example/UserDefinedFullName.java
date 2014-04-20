package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

public class UserDefinedFullName {

	/**
	 * 測試使用者自訂type3 同時測試addDummyHandlerSmellInfo的去<>功能是否能正常運行
	 */
	public void method() {
		ArrayList<Boolean> booleanList = new ArrayList<Boolean>();

		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			// 使用者自訂type3 - java.util.ArrayList.add
			booleanList.add(true);
		}
	}
}
