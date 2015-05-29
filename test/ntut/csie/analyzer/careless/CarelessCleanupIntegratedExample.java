package ntut.csie.analyzer.careless;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.DigestInputStream;

import ntut.csie.analyzer.careless.closingmethod.ResourceCloser;
import ntut.csie.analyzer.careless.closingmethod.UserDefinedCarelessCleanupClass;
import ntut.csie.analyzer.careless.closingmethod.UserDefinedCarelessCleanupMethod;

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
		} catch (IOException e) {
			fileOutputStream.close(); // Safe
			throw e;
		} finally {
			fileOutputStream.flush();
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

	public void thrownExceptionInFinallyWith2KindsUserDefinedInstance()
			throws Exception {
		MethodInvocationBeforeClose methodBeforeClose = new MethodInvocationBeforeClose();

		UserDefinedCarelessCleanupMethod udMethod = new UserDefinedCarelessCleanupMethod();
		UserDefinedCarelessCleanupClass udClass = new UserDefinedCarelessCleanupClass();
		try {
			methodBeforeClose.declaredCheckedException();
			udMethod.bark(); // Unsafe when user defined
			udClass.bark(); // Unsafe when user defined
		} finally {
			methodBeforeClose.declaredCheckedException();
			udMethod.bark(); // Unsafe when user defined
			udClass.bite(); // Unsafe when user defined
		}
	}

	public void doTryFinallyTwiceWithUserDefinition(OutputStream os)
			throws IOException {
		InputStream is = null;
		try {
			os.write(is.read());
		} finally {
			ResourceCloser.closeResourceDirectly(is); // Safe
		}
		try {
			os.write(is.read());
		} finally {
			ResourceCloser.closeResourceDirectly(is); // Unsafe when user defined
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

	class Door {
		public void throwException() throws RuntimeException {
			throw new RuntimeException();
		}

		public void close() {
			System.out.println("Door already closed.");
		}
	}

	public void instanceDoNotImpCloseable(OutputStream outputStream) {
		Door door = new Door();
		door.throwException();
		door.close(); // Unsafe only if user define "*.close"
	}

	/**
	 * It is an example of a bug after 2014/04/01. When detecting body of
	 * catch-clause, the parent block will be body of methodDeclaration instead
	 * of try block, so we have to handle this as special case.
	 * 
	 * @author pig
	 */
	public void closeInCatchAndThereIsUncaughtDecalredInTry(
			OutputStream outputStream) throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			throw new RuntimeException("Uncaught decalration");
		} catch (IllegalArgumentException e) {
			fileOutputStream.close(); // Safe
		} catch (NullPointerException e) {
			fileOutputStream.close(); // Safe
		}
	}
}
