package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;

public class PrintWithCommentOnly {

	public void method() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			// TODO Auto-generated catch block
			e.printStackTrace();
			/**
			 * Java doc
			 * 
			 */
			
			/*
			 * Comments
			 */
		}
	}
}
