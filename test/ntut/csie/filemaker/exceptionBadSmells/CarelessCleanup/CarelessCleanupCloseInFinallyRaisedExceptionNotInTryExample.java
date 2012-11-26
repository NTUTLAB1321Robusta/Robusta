package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.FileInputStream;
import java.io.IOException;

public class CarelessCleanupCloseInFinallyRaisedExceptionNotInTryExample {

	/**
	 * 會發生Careless Cleanup的情況
	 * @throws IOException
	 */
	public void y_readFile() throws IOException {
		FileInputStream fis = new FileInputStream("C:\\12312.txt");
		if(fis.available() == 0) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		try {
			while(fis.available() == 0) {
				sb.append(fis.read());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			fis.close();
		}
	}
	
	/**
	 * 不會被認定Careless Cleanup
	 * @throws IOException
	 */
	public void readFile2() throws IOException {
		FileInputStream fis = new FileInputStream("C:\\12312.txt");
		StringBuilder sb = new StringBuilder();
		try {
			while(fis.available() == 0) {
				sb.append(fis.read());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			fis.close();
		}
	}
}
