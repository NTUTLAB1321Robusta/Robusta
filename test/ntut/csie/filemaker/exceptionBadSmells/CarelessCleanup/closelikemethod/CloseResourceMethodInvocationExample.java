package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class CloseResourceMethodInvocationExample {

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

	/**
	 * These examples will be marked only if user add such user defined rules.
	 */
	public void userDefinedClass() throws Exception {
		UserDefinedCarelessCleanupClass clazz = new UserDefinedCarelessCleanupClass();
		clazz.bark();
		clazz.bark();
		clazz.bite();
		
		UserDefinedCarelessCleanupMethod methods = new UserDefinedCarelessCleanupMethod();
		methods.Shine();
		methods.rain();
		methods.bark();
	}
	
	/**
	 * These examples will be marked only if user add such user defined rules.
	 */
	public void userDefinedMethod() throws Exception {
		UserDefinedCarelessCleanupClass clazz = new UserDefinedCarelessCleanupClass();
		clazz.bark();
		clazz.bark();
		clazz.bite();
		
		UserDefinedCarelessCleanupMethod methods = new UserDefinedCarelessCleanupMethod();
		methods.Shine();
		methods.rain();
		methods.bark();
	}

	public void userDefinedMethodWithArguments() throws Exception {
		UserDefinedCarelessCleanupMethod methods = new UserDefinedCarelessCleanupMethod();
		methods.Shine(5);
		methods.Shine();
		methods.Shine(10);
	}

	/**
	 * Never try to close fis, so it is ignored cleanup but not careless cleanup
	 */
	public void thrownExceptionButNeverClose(File file1, File file2)
			throws IOException {
		if ((file1 == null) || (file2 == null)) {
			throw new IllegalArgumentException("Null 'file' argument.");
		}
		FileInputStream fis = new FileInputStream(file1);

		java.io.OutputStream out = new java.io.BufferedOutputStream(
				new FileOutputStream(file2));
		try {
			out.write(fis.read());
		} finally {
			System.out.println(fis.available());
		}
	}

	/**
	 * For the concrete closeable below
	 */
	class SuperCloseable implements Closeable {
		public void close() throws IOException {
		}
		public void doSomething() throws RuntimeException {
			throw new RuntimeException();
		}
	}

	/**
	 * This close statement will not be detected.
	 * (SuperMethodInvocation is not a MethodInvocation)
	 */
	class ConcreteCloseable extends SuperCloseable {
		public void close() throws IOException {
			doSomething();
			super.close(); // Unsafe but not included
		}
	}

}
