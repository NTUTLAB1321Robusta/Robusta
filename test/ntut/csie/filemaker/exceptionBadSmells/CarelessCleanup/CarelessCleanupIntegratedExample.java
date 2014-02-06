package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.DigestInputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.ResourceCloser;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.UserDefinedCarelessCleanupClass;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod.UserDefinedCarelessCleanupMethod;

public class CarelessCleanupIntegratedExample {

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
					fileOutputStream.close(); // Unsafe
				}
			} catch (IOException e) {
				throw e;
			}
		}
	}

	/**
	 * Should define UserDefinedCarelessCleanupClass and method "bark"
	 */
	public void thrownExceptionInFinallyWith2KindsUserDefinedInstance()
			throws Exception {
		UserDefinedCarelessCleanupMethod udMethod = new UserDefinedCarelessCleanupMethod();
		UserDefinedCarelessCleanupClass udClass = new UserDefinedCarelessCleanupClass();
		try {
			udMethod.bark(); // Unsafe
			udClass.bark(); // Unsafe
		} finally {
			udMethod.bark(); // Unsafe
			udClass.bite(); // Unsafe
		}
	}

	public void doTryFinallyTwiceWithUserDefinition(OutputStream zOut)
			throws IOException {
		InputStream is = null;
		try {
			zOut.write(is.read());
		} finally {
			ResourceCloser.closeResourceDirectly(is); // Safe but detected
		}
		try {
			zOut.write(is.read());
		} finally {
			ResourceCloser.closeResourceDirectly(is); // Unsafe
		}
	}

	/**
	 * It is an example of a bug on CC rule version 2.
	 */
	public String closeInNestedTryBlock(File file) {
		String checksum = null;
		try {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
				DigestInputStream dis = new DigestInputStream(fis, null);
				dis.close(); // Safe
				fis.close(); // Unsafe
			} catch (Exception e) {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
		return checksum;
	}

	/**
	 * It is an example of a bug on CC rule version 1.
	 */
	public void closeResourceInTryBlockInCatchBlock(File file)
			throws IOException {
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "rw");
		} catch (IOException e) {
			try {
				raf.close(); // Safe
			} catch (IOException inner) {
			}
		}
	}

	/**
	 * This example will be detected only if user define "*.close"
	 */
	public void instanceDoNotImpCloseable(OutputStream outputStream)
			throws IOException {
		ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
		ios.flush();
		ios.close();
	}
}
