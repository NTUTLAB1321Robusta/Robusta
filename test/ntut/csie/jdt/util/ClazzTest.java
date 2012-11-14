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
		// FileOutputStream是因為繼承OutputStream，而OutputStream有實作Closeable
		assertTrue(Clazz.isImplemented(FileOutputStream.class, Closeable.class));

		// InputStream自己就有實作Closeable
		assertTrue(Clazz.isImplemented(InputStream.class, Closeable.class));

		// File是Object的子類別
		assertFalse(Clazz.isImplemented(File.class, Closeable.class));

		// 沒有實作Closeable的interface
		assertFalse(Clazz.isImplemented(Serializable.class, Closeable.class));
	}
	
	@Test
	public void testIsUncheckedException() {
		assertTrue(Clazz.isUncheckedException(ArrayIndexOutOfBoundsException.class.getName()));
		assertFalse(Clazz.isUncheckedException(ArrayIndexOutOfBoundsException.class.getSimpleName()));
	}
}
