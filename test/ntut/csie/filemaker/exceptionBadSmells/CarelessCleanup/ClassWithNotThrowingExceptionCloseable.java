package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.IOException;
import agile.exception.Robustness;
import agile.exception.RL;

/**
 * 這個Class沒有繼承Closeable，但是有close的method，
 * 而且close的method不會拋出例外。
 * @author Charles
 *
 */
public class ClassWithNotThrowingExceptionCloseable {
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public ClassWithNotThrowingExceptionCloseable() throws IOException {
		throw new IOException();
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void open() throws IOException {
		throw new IOException();
	}
	
	public void close() {
		
	}
}
