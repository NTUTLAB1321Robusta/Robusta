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
 * 精簡版 偵測條件全勾的情況下，自定義部分加入 close 所發現之例子
 */
public class CarelessCleanupAntExample {
	/**
	 * 是壞味道的例子 CCII 抓得到 CCI 沒抓到的例子
	 * 
	 * @throws IOException
	 */
	private void addResourceWithUserDefinition(OutputStream zOut)
			throws IOException {
		InputStream is = null;
		try {
			zipFile(is, zOut);
		} finally {
			CarelessCleanupExampleForAntDefinition.close(is);// 這個不是!
		}
		try {
			zipFile(is, zOut);
		} finally {
			CarelessCleanupExampleForAntDefinition.close(is);// CCII 認為是壞味道
		}
	}

	/**
	 * 是壞味道的例子 CCII 抓得到 CCI 沒抓到的例子
	 * 
	 * @throws IOException
	 */
	private void addResource(OutputStream zOut) throws IOException {
		InputStream is = null;
		try {
			zipFile(is, zOut);
		} finally {
			is.close();// 這個不是!
		}
		try {
			zipFile(is, zOut);
		} finally {
			is.close();// CCII 認為是壞味道
		}
	}

	/**
	 * Example 中需要用到的 method 會拋出 IOException 可以不用管
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
	 * 是壞味道的例子 CCII 抓得到 CCI 沒抓到的例子
	 * 
	 * @throws IOException
	 */
	private void fetchFile(File localFile, OutputStream out) throws IOException {
		FileOutputStream fos = new FileOutputStream(localFile);
		try {
		} finally {
			fos.flush();
			fos.close();// CCII 認為是壞味道
		}
	}

	/**
	 * [v] Also detct...... 風順那邊有例子！抓到的那行將資源丟給其他 method 所使用，但沒有要將資源關閉，但 CCI
	 * 仍然抓出來。
	 */
	private BufferedReader reader;

	/**
	 * 不是壞味道的例子 CCI 抓得到 CCII 沒抓到的例子
	 * 
	 * @throws IOException
	 */
	public void closeCCIINotCatch() throws IOException {
		if (reader != null) {
			
			reader.close();// CCI 認為是壞味道
		}
	}

	private abstract class Handler {
		private PrintStream ps;

		/**
		 * 不是壞味道的例子 CCI 抓得到 CCII 沒抓到的例子
		 */
		void completeWithUserDefinition() {
			CarelessCleanupExampleForAntDefinition.close(ps);// CCI 認為是壞味道
		}
	}

	/**
	 * 不是壞味道的例子 CCI 抓得到 CCII 沒抓到的例子
	 * 
	 * @throws IOException
	 */
	public void setOutput(OutputStream out) throws IOException {
		if (out != System.out) {
			out.close();// CCI 認為是壞味道
		}
	}

	private URLConnection conn;

	/**
	 * 不是壞味道的例子 CCI 抓得到 CCII 沒抓到的例子
	 */
	private synchronized void close() {
		try {
			CarelessCleanupExampleForAntDefinition.close(conn);// CCI 認為是壞味道
		} finally {
			conn = null;
		}
	}

	private MessageDigest messageDigest = null;

	/**
	 * 是壞味道的例子 CCI 抓得到 CCII 沒抓到的例子
	 */
	public String getValue(File file) {
		String checksum = null;
		try {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(file);
				DigestInputStream dis = new DigestInputStream(fis,
						messageDigest);
				dis.close();// CCI 認為是壞味道
				fis.close();// CCI 認為是壞味道
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
	 * 是壞味道的例子 CCI
	 * 抓得到 CCII 沒抓到的例子
	 */
	public CarelessCleanupAntExample(File file) throws IOException {
		try {
			raf = new RandomAccessFile(file, "rw");
		} catch (IOException e) {
			try {
				raf.close();// CCI 認為是壞味道
			} catch (IOException inner) {
			}
		}
	}
}
