package ntut.csie.analyzer.careless.closingmethod;

import java.io.Closeable;

/**
 * This class implements Closeable，but override the close method to a method who
 * won't throw any exception
 * @author pig
 */

public class ClassImplementCloseableWithoutThrowException implements Closeable {

	@Override
	public void close() {
	}

}
