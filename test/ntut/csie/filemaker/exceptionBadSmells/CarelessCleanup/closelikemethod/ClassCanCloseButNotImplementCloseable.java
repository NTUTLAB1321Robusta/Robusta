package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod;

import java.io.IOException;

/**
 * This class does not implement Closeable. But has a close method that has the
 * same interface with close of Closeable.
 */
public class ClassCanCloseButNotImplementCloseable {

	public void close() throws IOException {
		throw new IOException();
	}

}
