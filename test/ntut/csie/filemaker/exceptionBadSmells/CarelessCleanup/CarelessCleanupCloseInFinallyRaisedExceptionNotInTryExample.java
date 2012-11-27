package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CarelessCleanupCloseInFinallyRaisedExceptionNotInTryExample {

	/**
	 * 會發生Careless Cleanup的情況
	 * @throws IOException
	 */
	public void true_readFile() throws IOException {
		FileInputStream fis = new FileInputStream("C:\\12312.txt");
		if(fis.available() == 0) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		try {
			while(fis.available() == 0) {
				sb.append(fis.read());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			fis.close();
		}
	}
	
	/**
	 * 不會被認定Careless Cleanup
	 * @throws IOException
	 */
	public void readFile2() throws IOException {
		FileInputStream fis = new FileInputStream("C:\\12312.txt");
		StringBuilder sb = new StringBuilder();
		try {
			while(fis.available() == 0) {
				sb.append(fis.read());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			fis.close();
		}
	}
	
	/**
	 * 因為FileInputStream fis = new FileInputStream(file1);執行完畢以後，
	 * java.io.OutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(file2));可能發生例外，
	 * 所以fis.close();會變成careless cleanup。
	 * <br /><br />
	 * 因為fis.close()可能會發生例外，
	 * 所以out.close()會變成careless cleanup。
	 * <br /><br />
	 * 但是CarelessCleanupCloseInFinallyRaisedExceptionNotInTryCaused只會偵測到fis.close()。
	 * @param file1
	 * @param file2
	 * @throws IOException
	 */
	public void true_thrownExceptionOnMethodDeclarationWithTryStatementWith2KindsInstance(
			File file1, File file2) throws IOException {
		if((file1 == null) || (file2 == null)) {
			throw new IllegalArgumentException("Null 'file' argument.");
		}
		FileInputStream fis = new FileInputStream(file1);
		
		// 如果out發生例外，則fis依然會有 careless cleanup 的壞味道
		java.io.OutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(file2));
		try {
			while(fis.available() != 0) {
				out.write(fis.read());
			}
		} finally {
			fis.close();	// 因為new FileOutputStream會拋出例外，造成fis.close()變成careless cleanup
			out.close();	// 因為fis.close()會拋出例外，造成out.close()變成careless cleanup
		}
	}
	
	/**
	 * StringBuilder在建立的時候不會發生例外，所以這整個例子不是careless cleanup
	 * @throws IOException
	 */
	public void create2InstanceWithoutThrowingExceptions() throws IOException {
		FileInputStream fis = new FileInputStream("C:\\123123123");
		StringBuilder sb = new StringBuilder();
		try{
			sb.append("1111");
			while (fis.available() != 0) {
				fis.read();
			}
		} finally {
			fis.close();
		}
	}
}
