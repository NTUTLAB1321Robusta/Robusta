package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.security.DigestInputStream;
import java.security.MessageDigest;

/**
 * ��²�� ����������Ī����p�U�A�۩w�q�����[�J close �ҵo�{���Ҥl
 */
public class CarelessCleanupAntExample {
	/**
	 * �O�a���D���Ҥl CCII ��o�� CCI �S��쪺�Ҥl
	 * 
	 * @throws IOException
	 */
	private void addResourceWithUserDefinition(OutputStream zOut)
			throws IOException {
		InputStream is = null;
		try {
			zipFile(is, zOut);
		} finally {
			CarelessCleanupExampleForAntDefinition.close(is);// �o�Ӥ��O!
		}
		try {
			zipFile(is, zOut);
		} finally {
			CarelessCleanupExampleForAntDefinition.close(is);// CCII �{���O�a���D
		}
	}

	/**
	 * �O�a���D���Ҥl CCII ��o�� CCI �S��쪺�Ҥl
	 * 
	 * @throws IOException
	 */
	private void addResource(OutputStream zOut) throws IOException {
		InputStream is = null;
		try {
			zipFile(is, zOut);
		} finally {
			is.close();// �o�Ӥ��O!
		}
		try {
			zipFile(is, zOut);
		} finally {
			is.close();// CCII �{���O�a���D
		}
	}

	/**
	 * Example ���ݭn�Ψ쪺 method �|�ߥX IOException �i�H���κ�
	 * 
	 * @throws IOException
	 */
	private void zipFile(InputStream in, OutputStream zOut) throws IOException {
		byte[] buffer = new byte[BUFFER_SIZE];
		int count = 0;
		do {
			zOut.write(buffer, 0, count);
			count = in.read(buffer, 0, buffer.length);
		} while (count != -1);
	}

	private static final int BUFFER_SIZE = 1024;

	/**
	 * �O�a���D���Ҥl CCII ��o�� CCI �S��쪺�Ҥl
	 * 
	 * @throws IOException
	 */
	private void fetchFile(File localFile, OutputStream out) throws IOException {
		FileOutputStream fos = new FileOutputStream(localFile);
		try {
		} finally {
			fos.flush();
			fos.close();// CCII �{���O�a���D
		}
	}

	/**
	 * [v] Also detct...... �������䦳�Ҥl�I��쪺����N�귽�ᵹ��L method �ҨϥΡA���S���n�N�귽�����A�� CCI
	 * ���M��X�ӡC
	 */
	private BufferedReader reader;

	/**
	 * ���O�a���D���Ҥl CCI ��o�� CCII �S��쪺�Ҥl
	 * 
	 * @throws IOException
	 */
	public void closeCCIINotCatch() throws IOException {
		if (reader != null) {
			
			reader.close();// CCI �{���O�a���D
		}
	}

	private abstract class Handler {
		private PrintStream ps;

		/**
		 * ���O�a���D���Ҥl CCI ��o�� CCII �S��쪺�Ҥl
		 */
		void completeWithUserDefinition() {
			CarelessCleanupExampleForAntDefinition.close(ps);// CCI �{���O�a���D
		}
	}

	/**
	 * ���O�a���D���Ҥl CCI ��o�� CCII �S��쪺�Ҥl
	 * 
	 * @throws IOException
	 */
	public void setOutput(OutputStream out) throws IOException {
		if (out != System.out) {
			out.close();// CCI �{���O�a���D
		}
	}

	private URLConnection conn;

	/**
	 * ���O�a���D���Ҥl CCI ��o�� CCII �S��쪺�Ҥl
	 */
	private synchronized void close() {
		try {
			CarelessCleanupExampleForAntDefinition.close(conn);// CCI �{���O�a���D
		} finally {
			conn = null;
		}
	}

	private MessageDigest messageDigest = null;

	/**
	 * �O�a���D���Ҥl CCI ��o�� CCII �S��쪺�Ҥl
	 */
	public String getValue(File file) {
		String checksum = null;
		try {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
				DigestInputStream dis = new DigestInputStream(fis,
						messageDigest);
				dis.close();// CCI �{���O�a���D
				fis.close();// CCI �{���O�a���D
			} catch (Exception e) {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
		return checksum;
	}

	private RandomAccessFile raf = null;

	/**
	 * �O�a���D���Ҥl CCI
	 * ��o�� CCII �S��쪺�Ҥl
	 */
	public CarelessCleanupAntExample(File file) throws IOException {
		try {
			raf = new RandomAccessFile(file, "rw");
		} catch (IOException e) {
			try {
				raf.close();// CCI �{���O�a���D
			} catch (IOException inner) {
			}
		}
	}
}
