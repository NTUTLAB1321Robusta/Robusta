package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CarelessCleanupCloseInFinallyRaisedExceptionNotInTryExample {

	/**
	 * �|�o��Careless Cleanup�����p
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
	 * ���|�Q�{�wCareless Cleanup
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
	 * �]��FileInputStream fis = new FileInputStream(file1);���槹���H��A
	 * java.io.OutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(file2));�i��o�ͨҥ~�A
	 * �ҥHfis.close();�|�ܦ�careless cleanup�C
	 * <br /><br />
	 * �]��fis.close()�i��|�o�ͨҥ~�A
	 * �ҥHout.close()�|�ܦ�careless cleanup�C
	 * <br /><br />
	 * ���OCarelessCleanupCloseInFinallyRaisedExceptionNotInTryCaused�u�|������fis.close()�C
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
		
		// �p�Gout�o�ͨҥ~�A�hfis�̵M�|�� careless cleanup ���a���D
		java.io.OutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(file2));
		try {
			while(fis.available() != 0) {
				out.write(fis.read());
			}
		} finally {
			fis.close();	// �]��new FileOutputStream�|�ߥX�ҥ~�A�y��fis.close()�ܦ�careless cleanup
			out.close();	// �]��fis.close()�|�ߥX�ҥ~�A�y��out.close()�ܦ�careless cleanup
		}
	}
	
	/**
	 * StringBuilder�b�إߪ��ɭԤ��|�o�ͨҥ~�A�ҥH�o��ӨҤl���Ocareless cleanup
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
