package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.File;
import java.io.FileInputStream;

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
	
}
