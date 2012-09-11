package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.Closeable;

/**
 * 這個Class雖然繼承Closeable，但是將close的method override成不會拋例外
 * @author pig
 */

public class ClassImplementCloseableWithoutThrowException implements Closeable {

	@Override
	public void close() {
	}

}
