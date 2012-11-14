package ntut.csie.jdt.util;

import static org.junit.Assert.*;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;

import org.junit.Test;

public class ClazzTest {

	@Test
	public void testIsImplemented() {
		// FileOutputStream�O�]���~��OutputStream�A��OutputStream����@Closeable
		assertTrue(Clazz.isImplemented(FileOutputStream.class, Closeable.class));

		// InputStream�ۤv�N����@Closeable
		assertTrue(Clazz.isImplemented(InputStream.class, Closeable.class));

		// File�OObject���l���O
		assertFalse(Clazz.isImplemented(File.class, Closeable.class));

		// �S����@Closeable��interface
		assertFalse(Clazz.isImplemented(Serializable.class, Closeable.class));
	}
	
	@Test
	public void testIsUncheckedException() {
		assertTrue(Clazz.isUncheckedException(ArrayIndexOutOfBoundsException.class.getName()));
		assertFalse(Clazz.isUncheckedException(ArrayIndexOutOfBoundsException.class.getSimpleName()));
	}
}
