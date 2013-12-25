package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.ResourceCloser;

import org.apache.log4j.lf5.util.Resource;

public class TempExampleForNextGoal {

	FileInputStream fileInputStream = null;
	File file = null;
	MethodInvocationBeforeClose methodBeforeClose = new MethodInvocationBeforeClose();

//	public void closeAfterCloser() throws Exception {
//		methodBeforeClose.closerWithoutException(fileInputStream);
//		/*
//		 * Unsafe until other new story be finish
//		 * @author pig
//		 */
//		fileInputStream.close();
//	}
//
//	public void createAndCloseAfterCloser() throws Exception {
//		FileInputStream fis = new FileInputStream(file);
//		methodBeforeClose.closerWithoutException(fis);
//		/*
//		 * Unsafe until other new story be finish
//		 * @author pig
//		 */
//		fis.close();
//	}
//
//	public void closeInFinallyBlockButAfterCloser() throws Exception {
//		FileInputStream fis = new FileInputStream(file);
//		try {
//			methodBeforeClose.declaredCheckedException();
//			methodBeforeClose.didNotDeclareAnyExceptionButThrowUnchecked();
//		} finally {
//			methodBeforeClose.closerWithoutException(fis);
//			/*
//			 * Unsafe until other new story be finish
//			 * @author pig
//			 */
//			fis.close(); // Unsafe
//		}
//	}
	
	// FileUtils.closeFile(fileinputStream);
	
	//FileUtils.closeFileWithCondition(fileinputStream, true);
	
	// closeable.closeResource(resource)
	
	/*
	 * Other Example, haven't check if need
	 */

	/**
	 * 這例子會讓我們工具拋出 NullPointerException
	 */
	public void arbintest4(Resource resource, OutputStream zOut)
			throws IOException {
		InputStream rIn = resource.getInputStream();
		try {
			rIn.read();
		} finally {
			rIn.close(); // it isn't CC
		}
	}

//	/**
//	 * 這例子會讓我們工具拋出 NullPointerException
//	 */
//	public void arbintest5(Resource resource, OutputStream zOut)
//			throws IOException {
//		InputStream in = null;
//		try {
//			in = resource.getInputStream();
//		} finally {
//			FileUtils.close(in); // it isn't CC
//		}
//	}
	
	/*
	 * Example of resource is a field
	 */

	private Closeable conn;

	/**
	 * 不是壞味道的例子 CCI 抓得到 CCII 沒抓到的例子
	 */
	private synchronized void close() {
		try {
			ResourceCloser.closeResourceWithoutExceptionWithDoNothing(conn);
		} finally {
			conn = null;
		}
	}

	/*
	 * Example of inner class
	 */

	private abstract class Handler {
		private PrintStream ps;

		/**
		 * 不是壞味道的例子 CCI 抓得到 CCII 沒抓到的例子
		 */
		void completeWithUserDefinition() {
			ResourceCloser.closeResourceWithoutExceptionWithDoNothing(ps);// CCI 認為是壞味道
		}
	}

	/**
	 * 如果是專門用來放在Finally關閉串流的method，將不做careless cleanup的檢查
	 * @param fileOutputStream
	 */
	public void closeStreamWithoutThrowingExceptionNestedIfTry(FileOutputStream fileOutputStream) {
		if (fileOutputStream != null)
			try {
				if(fileOutputStream != null) {
					fileOutputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
}
