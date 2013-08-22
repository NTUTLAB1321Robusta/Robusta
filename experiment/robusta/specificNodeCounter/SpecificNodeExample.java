package robusta.specificNodeCounter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class SpecificNodeExample {
	
	public void oneTryStatement() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
		}
	}

	public void nestedTryStatement() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
		} catch (Exception e2) {
		}finally {
			try {
				fis.close();
			} catch(IOException e2) {
			}
		}
	}

	public void tryWithoutCatchClause() throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} finally {
			fis.close();
		}
	}
}
