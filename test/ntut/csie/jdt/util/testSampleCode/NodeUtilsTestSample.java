package ntut.csie.jdt.util.testSampleCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NodeUtilsTestSample {
	// Object���O����k
	public void objectMethod() {
		Object object = new Object();
		// NodeUtils���|��X�L�O��@Closeable��
		object.toString();
	}
	
	// File���O����k
	public void fileMethod() {
		File f = new File("");
		// NodeUtils���|��X�L�O��@Closeable��
		f.toString();
	}
	
	// OutputStream���O����k(�����O����@Closeable)
	public void outputStreamMethod() throws IOException {
		OutputStream os = null;
		os = new OutputStream() {		
			@Override
			public void write(int b) throws IOException {
				throw new IOException();
			}
		};
		// NodeUtils�|��X�L�O��@Closeable��
		os.write(1);
		// NodeUtils�|��X�L�O��@Closeable��
		os.close();
	}
	
	// FileInputStream���O����k(��@Closeable)
	public void fileInputStreamMethod() throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream("");
		// NodeUtils���|��X�L�O��@Closeable��
		fis.toString();
		// NodeUtils�|��X�L�O��@Closeable��
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
}
