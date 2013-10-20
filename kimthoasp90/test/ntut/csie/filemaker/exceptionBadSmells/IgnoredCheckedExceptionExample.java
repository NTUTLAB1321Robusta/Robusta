package ntut.csie.filemaker.exceptionBadSmells;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class IgnoredCheckedExceptionExample {
	public void true_IgnoredCheckedException() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {	// IgnoreCheckedException
			
		}
	}
	
	public void true_IgnoreCheckedExceptionWithNestedTryStatement() {
		FileOutputStream fos = null;
		FileInputStream fis = null;
		try {
			fos = new FileOutputStream("D:\\abc.txt");
			fos.write(10);
			try{
				fis = new FileInputStream("C:\\456.txt");
				fis.read();
				while(fis.available() != 0) {
					fos.write(fis.read());
				}
			} catch (IOException e) {	// IgnoreCheckedException
			} finally {
				fis.close();	//OverwrittenLeadException
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				fos.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void true_IgnoredCheckedExceptionInNestedTryStatement() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("C:\\456.txt");
			fis.read();
		} catch (IOException e) {
			try {
				fis = new FileInputStream("C:\\123.txt");
			} catch (FileNotFoundException e1) {	// IgnoreCheckedException
			}
		}
	}
	
	public void true_IgnoredCheckedExceptionWithOverwrittenLeadException() throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("C:\\456.txt");
			fis.read();
		} catch (IOException e) {
			try {
				fis = new FileInputStream("C:\\123.txt");
			} catch (FileNotFoundException e1) {	// IgnoreCheckedException
			}
			throw e;
		} finally {
			fis.close();
		}
	}
	
	public void true_IgnoredCheckedExceptionWithDummyHandler() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("C:\\456.txt");
			fis.read();
		} catch (IOException e) {
			try {
				fis = new FileInputStream("C:\\123.txt");
				fis.read();
			} catch (FileNotFoundException e1) {	// IgnoreCheckedException
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}
	}
	
	public void true_IgnoredCheckedExceptionWithCarelessCleanup() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
			fis.close();
		} catch (IOException e) {	// IgnoreCheckedException
		} finally {
			System.out.println("done");
		}
	}
	
	public void true_IgnoredCheckedExceptionWithCloseStream() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				fis.close();
			} catch (IOException e) {	// IgnoreCheckedException
			}
		}
	}
}
