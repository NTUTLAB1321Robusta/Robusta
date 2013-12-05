package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CarelessCleanupIntegratedExampleFrom20131113 {

	public void thrownExceptionInFinallyWith2KindsInstance(
			File file1, File file2) throws IOException {
		if((file1 == null) || (file2 == null)) {
			throw new IllegalArgumentException("Null 'file' argument.");
		}
		FileInputStream fis = new FileInputStream(file1);
		
		java.io.OutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(file2));
		try {
			while(fis.available() != 0) {
				out.write(fis.read());
			}
		} finally {
			/*
			 * fis.close() is careless cleanup because of
			 * "new FileOutputStream(file2));"
			 */
			fis.close();
			out.close(); // this is careless cleanup because of "fis.close()"
		}
	}
	
}
