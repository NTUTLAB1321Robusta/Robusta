package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.Closeable;
import java.io.IOException;

import ntut.csie.robusta.agile.exception.Tag;
import ntut.csie.robusta.agile.exception.Robustness;

public class ClassImplementCloseable implements Closeable {

	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	@Override
	public void close() throws IOException {
		throw new IOException();
	}

}
