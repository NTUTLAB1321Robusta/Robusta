package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Level;

public class ResourceCloser {

	private static java.util.logging.Logger javaLog = java.util.logging.Logger
			.getLogger("");;

	public static void closeResourceDirectly(Closeable resource)
			throws IOException {
		resource.close();
	}

	public static void closeResourceWithIOException(Closeable resource)
			throws IOException {
		try {
			resource.close();
		} catch (IOException e) {
			throw e;
		}
	}

	public static void closeResourceWithUndeclaredRuntimeException(
			Closeable resource) {
		try {
			resource.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * We are not sure if it will throw any runtime exception
	 */
	public static void closeResourceWithLogDirectly(Closeable resource) {
		try {
			resource.close();
		} catch (IOException e) {
			javaLog.log(Level.INFO, e.toString());
		}
	}

	public static void closeResourceWithoutExceptionWithDoNothing(
			Closeable resource) {
		try {
			resource.close();
		} catch (Exception e) {
			// Do nothing
		}
	}

	public static void closeResourceWithoutExceptionWithSafeLog(
			Closeable resource) {
		try {
			resource.close();
		} catch (IOException e) {
			logWithoutExceptionAndDoNothing(e);
		}
	}

	public static void logWithoutExceptionAndDoNothing(IOException ioException) {
		try {
			javaLog.log(Level.INFO, ioException.toString());
		} catch (Exception e) {
			// Do nothing
		}
	}

	public static void closeResourceInTryBlockWithPreventFromNullOutside(
			Closeable resource) {
		if (null != resource) {
			try {
				resource.close();
			} catch (IOException e) {
				// Do nothing
			}
		}
	}

	public static void closeResourceInTryBlockWithPreventFromNullInside(
			Closeable resource) {
		try {
			if (null != resource) {
				resource.close();
			}
		} catch (IOException e) {
			// Do nothing
		}
	}

}
