package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.IOException;
import agile.exception.Robustness;
import agile.exception.RL;

/**
 * �o��Class�S���~��Closeable�A���O��close��method�A
 * �ӥBclose��method���|�ߥX�ҥ~�C
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
