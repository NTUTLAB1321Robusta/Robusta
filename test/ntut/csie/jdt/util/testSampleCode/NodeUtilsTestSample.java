package ntut.csie.jdt.util.testSampleCode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

public class NodeUtilsTestSample {
	// Object類別的方法
	public void objectMethod() {
		Object object = new Object();
		// NodeUtils不會抓出他是實作Closeable的
		object.toString();
	}
	
	// File類別的方法
	public void fileMethod() {
		File f = new File("");
		// NodeUtils不會抓出他是實作Closeable的
		f.toString();
	}
	
	// OutputStream類別的方法(此類別有實作Closeable)
	public void outputStreamMethod() throws IOException {
		OutputStream os = null;
		os = new OutputStream() {		
			@Override
			public void write(int b) throws IOException {
				throw new IOException();
			}
		};
		// NodeUtils會抓出他是實作Closeable的
		os.write(1);
		// NodeUtils會抓出他是實作Closeable的
		os.close();
	}
	
	// FileInputStream類別的方法(實作Closeable)
	public void fileInputStreamMethod() throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream("");
		// NodeUtils不會抓出他是實作Closeable的
		fis.toString();
		// NodeUtils會抓出他是實作Closeable的
		fis.close()
		;
	}
}
