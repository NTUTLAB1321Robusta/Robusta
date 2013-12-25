package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod;

import java.io.Closeable;
import java.io.IOException;

public class ClassImplementCloseable implements Closeable {

	@Override
	public void close() throws IOException {
		throw new IOException();
	}

}
