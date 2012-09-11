package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.Closeable;
import java.io.IOException;
import agile.exception.Robustness;
import agile.exception.RL;

public class ClassImplementCloseable implements Closeable {

	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	@Override
	public void close() throws IOException {
		throw new IOException();
	}

}
