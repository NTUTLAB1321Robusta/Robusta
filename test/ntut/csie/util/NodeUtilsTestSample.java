package ntut.csie.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NodeUtilsTestSample {
	// Object's method 
	public void objectMethod() {
		Object object = new Object();
		object.toString();
	}
	
	public void fileMethod() {
		File f = new File("");
		f.toString();
	}
	
	public void outputStreamMethod() throws IOException {
		OutputStream os = null;
		os = new OutputStream() {		
			@Override
			public void write(int b) throws IOException {
				throw new IOException();
			}
		};
		os.write(1);
		os.close();
	}
	
	public void fileInputStreamMethod() throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream("");
		fis.toString();
		fis.close();
	}
	
	public void readFile() throws Exception {
		FileInputStream fis = null;
		StringBuilder sb = new StringBuilder();
		try {
			fis = new FileInputStream("C:\\123.txt");
			sb.append(fis.read());
		} finally {
			fis.close();
		}
		
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("C:\\456.txt");
			fos.write(sb.toString().length());
		} finally {
			fos.close();
		}
	}

	public void useRuntimeException() {
		RuntimeException runtimeException = new RuntimeException();
		runtimeException.notify();
	}

	public void catchJavaDefinedException() {
		FileInputStream fis = null;
		try {
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void catchUserSelfDefinedException() {
		try {
			System.out.println("May throw RuntimeException");
		} catch (UserSelfDefinedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * For method {@link #catchUserSelfDefinedException()}
	 */
	private class UserSelfDefinedException extends RuntimeException {
	}
}
