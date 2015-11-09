package ntut.csie.util;

import static org.junit.Assert.*;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Serializable;

import ntut.csie.util.Clazz;

import org.junit.Test;

public class ClazzTest {

	@Test
	public void testIsImplemented() {
		assertTrue(Clazz.isImplemented(FileOutputStream.class, Closeable.class));

		assertTrue(Clazz.isImplemented(InputStream.class, Closeable.class));

		assertFalse(Clazz.isImplemented(File.class, Closeable.class));

		assertFalse(Clazz.isImplemented(Serializable.class, Closeable.class));
	}
	
	@Test
	public void testIsUncheckedException() {
		assertTrue(Clazz.isUncheckedException(ArrayIndexOutOfBoundsException.class.getName()));
		assertFalse(Clazz.isUncheckedException(ArrayIndexOutOfBoundsException.class.getSimpleName()));
	}
}
