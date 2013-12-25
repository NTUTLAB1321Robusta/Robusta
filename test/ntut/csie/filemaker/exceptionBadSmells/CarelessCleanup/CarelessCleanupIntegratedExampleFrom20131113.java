package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

public class CarelessCleanupIntegratedExampleFrom20131113 {

	public void thrownExceptionInFinallyWith2KindsInstance(File file1,
			File file2) throws IOException {
		if ((file1 == null) || (file2 == null)) {
			throw new IllegalArgumentException("Null 'file' argument.");
		}
		FileInputStream fis = new FileInputStream(file1);

		java.io.OutputStream out = new java.io.BufferedOutputStream(
				new FileOutputStream(file2));
		try {
			while (fis.available() != 0) {
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

	public void sameResourceCloseManyTimes(byte[] context, File outputFile)
			throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close(); // Unsafe
		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException.");
			fileOutputStream.close(); // Unsafe
			throw e;
		} catch (IOException e) {
			fileOutputStream.close(); // Safe
			throw e;
		} finally {
			System.out.println("Close nothing at all.");
			fileOutputStream.close(); // Unsafe
		}
	}

	public void closeIsTheFirstExecuteStatementButStillUnsafeWithSomeStatements()
			throws IOException {
		FileOutputStream fileOutputStream = null;
		for (int a = 0; a < 10; a++) {
			try {
				if (a == 5) {
					fileOutputStream.close();
				}
			} catch (IOException e) {
				throw e;
			}
		}
	}

}
