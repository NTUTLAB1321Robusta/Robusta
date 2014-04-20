package ntut.csie.analyzer.dummy.example;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

public class DummyInNestedTryAndOtherStructure {

	Logger log4j = null;
	
	public void inTryBlock() throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			if (true) {
				try {
					fos.write(10);
				} catch (IOException e) { // Dummy handler
					System.out.println(e.getMessage());
					log4j.info("message");
				}
			}
		} finally {
			if (fos != null)
				fos.close();
		}
	}

	public void inCatckClause() throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch (FileNotFoundException e) {
			try {
				fos = new FileOutputStream("");
				fos.write(10);
			} catch (FileNotFoundException e1) { // Dummy handler
				e1.printStackTrace();
			}
		} finally {
			if (fos != null)
				fos.close();
		}
	}

	public void inFinally() {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) { // Dummy handler
					e.printStackTrace();
				}
			}
		}
	}
}
