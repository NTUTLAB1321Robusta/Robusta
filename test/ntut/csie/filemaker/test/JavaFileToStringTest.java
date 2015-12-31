package ntut.csie.filemaker.test;

import static org.junit.Assert.*;

import java.io.File;

import ntut.csie.filemaker.JavaFileToString;
import ntut.csie.filemaker.JavaProjectMaker;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author pig
 */
public class JavaFileToStringTest {
	JavaFileToString jFileToString;

	@Before
	public void setUp() throws Exception {
		jFileToString = new JavaFileToString();
		jFileToString.read(JavaFileToStringExample.class, JavaProjectMaker.FOLDERNAME_TEST);
	}

	@After
	public void tearDown() throws Exception {
		jFileToString = null;
	}

	/**
	 * confirm the loading file content is correct 
	 * Test method for {@link ntut.csie.filemaker.JavaFileToString#read(java.lang.Class, java.lang.String)}.
	 */
	@Test
	public void testReadClassOfQString() {
		String example = null;
		String lineSeparator = System.getProperty("line.separator");
		example = lineSeparator +
		"/**" + lineSeparator + 
		" * 供 JavaFileToStringTest 使用的範例" + lineSeparator + 
		" * @author pig" + lineSeparator + 
		" */" + lineSeparator + 
		"public class JavaFileToStringExample {" + lineSeparator + 
		"	private int memberInt;" + lineSeparator + 
		"	private String memberString;" + lineSeparator + 
		"	" + lineSeparator + 
		"	public void sayHello() {" + lineSeparator + 
		"	}" + lineSeparator + 
		"" + lineSeparator + 
		"	protected void doSayHello() {" + lineSeparator + 
		"	}" + lineSeparator + 
		"	" + lineSeparator + 
		"	public int plusOne(int beforePlus) {" + lineSeparator + 
		"		return returnWithPlusOne(beforePlus);" + lineSeparator + 
		"	}" + lineSeparator + 
		"" + lineSeparator + 
		"	private int returnWithPlusOne(int beforePlus) {" + lineSeparator + 
		"		return beforePlus + 1;" + lineSeparator + 
		"	}" + lineSeparator + 
		"}" + lineSeparator;
		assertEquals(example, jFileToString.getFileContent());
	}

	/**
	 * confirm the clear feature is stable
	 * Test method for {@link ntut.csie.filemaker.JavaFileToString#clear()}.
	 */
	@Test
	public void testClear() {
		jFileToString.clear();
		assertEquals("", jFileToString.getFileContent());
	}

}
