package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.Closeable;

/**
 * �o��Class���M�~��Closeable�A���O�Nclose��method override�����|�ߨҥ~
 * @author pig
 */

public class ClassImplementCloseableWithoutThrowException implements Closeable {

	@Override
	public void close(){
	}

}
