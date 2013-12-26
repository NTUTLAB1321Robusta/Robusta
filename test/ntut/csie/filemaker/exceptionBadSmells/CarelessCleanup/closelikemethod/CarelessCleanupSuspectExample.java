package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;


/**
 * These examples will be detected as suspects only if user add such user
 * defined rules.
 */
public class CarelessCleanupSuspectExample {

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
	
}
