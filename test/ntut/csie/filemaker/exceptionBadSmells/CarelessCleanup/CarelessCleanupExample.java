package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import ntut.csie.robusta.agile.exception.Tag;
import ntut.csie.robusta.agile.exception.Robustness;



/**
 * Careless Cleanup的錯誤範例。
 * Careless Cleanup：當使用者嘗試關閉串流時，關閉的動作並沒有在finally裡面執行，便是屬於這類壞味道。
 * 這些範例都可以被我們的CarelessCleanupVisitor偵測到。
 * @author Charles
 *
 */
public class CarelessCleanupExample {
	
	/**
	 * 不會被CarelessCleanupVisitor加上mark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void withoutCloseStream(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fileOutputStream.close();加上mark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y_closeStreamInTryBlock(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fileOutputStream.close();加上mark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y2_close2StreamInTryBlock(byte[] context, File inputFile, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		FileInputStream  fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(inputFile);
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileInputStream.close();
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fileOutputStream.close();加上mark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y_closeStreamInTryBlockWithEmptyFinally(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fileOutputStream.close();加上mark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y_closeStreamInTryBlockWithBlankFinally(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			
		}
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fileOutputStream.close();加上mark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y_closeStreamInTryBlockWithNonblankFinally(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			System.out.println("Close nothing at all.");
		}
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fileOutputStream.close();加上mark(兩處)
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = {
			@Tag(level = 1, exception = java.io.FileNotFoundException.class),
			@Tag(level = 1, exception = java.io.IOException.class) })
	public void y2_closeStreamInCatchClause(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			fileOutputStream.close();
			throw e;
		} catch (IOException e) {
			fileOutputStream.close();
			throw e;
		} finally {
			System.out.println("Close nothing at all.");
		}
	}
	
	/**
	 * 不會被CarelessCleanupVisitor在anInstance.close();加上mark
	 * 因為ClassWithNotThrowingExceptionCloseable不是implements Closeable介面
	 * 所以不會被視為是關閉資源的動作
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void closeNonClosableInstance() {
		ClassWithNotThrowingExceptionCloseable anInstance = null;
		try {
			anInstance = new ClassWithNotThrowingExceptionCloseable();
			anInstance.open();
			anInstance.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y_closeUserImplementingClosableInstance() {
		ClassImplementCloseable anInstance = null;
		try {
			anInstance = new ClassImplementCloseable();
			anInstance.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 看似專門用來放在Finally做關閉串流的程式碼，但是他會拋出例外，所以還是要被Careless Cleanup檢查。
	 * @param fileOutputStream
	 * @throws IOException 
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	protected void y_closeStreamThrowingExceptionDeclaring(FileOutputStream fileOutputStream) throws IOException {
		try {
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		throw new IOException();
	}
	
	/**
	 * 看似專門用來放在Finally做關閉串流的程式碼，但是他會拋出例外，所以還是要被Careless Cleanup檢查。
	 * @param fileOutputStream
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	protected void y_closeStreamThrowingException(FileOutputStream fileOutputStream) {
		try {
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 如果是專門用來放在Finally關閉串流的method，將不做careless cleanup的檢查。
	 * 但是目前的程式碼有用到else，所以我們認為語意上，可能不是單純關閉串流的method。
	 * @param fileOutputStream
	 */
	protected void y_closeStreamWithElseBigTry(FileOutputStream fileOutputStream) {
		try {
			if (fileOutputStream != null) {
				fileOutputStream.close();
			} else {
				System.out.println("Stream cannot be closed.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 如果是專門用來放在Finally關閉串流的method，將不做careless cleanup的檢查
	 * 但是目前的程式碼if不只有關閉串流的程式碼，所以我們認為語意上，可能不是單純關閉串流的method。
	 * @param fileOutputStream
	 */
	protected void y_closeStreamWithMultiStatementInThenBigTry(FileOutputStream fileOutputStream) {
		try {
			if (fileOutputStream != null) {
				fileOutputStream.close();
				System.out.println("Stream cannot be closed.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 如果是專門用來放在Finally關閉串流的method，將不做careless cleanup的檢查
	 * @param fileOutputStream
	 */
	protected void closeStreamWithoutThrowingExceptionBigTry(FileOutputStream fileOutputStream) {
		try {
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 如果是專門用來放在Finally關閉串流的method，將不做careless cleanup的檢查
	 * @param fileOutputStream
	 */
	protected void closeStreamWithoutThrowingExceptionBigTryIfWithoutBlock(FileOutputStream fileOutputStream) {
		try {
			if (fileOutputStream != null)
				fileOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 如果是專門用來放在Finally關閉串流的method，將不做careless cleanup的檢查
	 * @param fileOutputStream
	 */
	protected void closeStreamWithoutThrowingException(FileOutputStream fileOutputStream) {
		if (fileOutputStream != null) {
			try {
				fileOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
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
	
	/**
	 * 會被CarelessCleanupVisitor在fis.close();加上mark
	 * @throws IOException 
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	private void y_closeStreamWithoutTryBlock() throws IOException {
		FileInputStream fis = new FileInputStream("");
		fis.read();
		fis.close();
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fis.close();加上mark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	private void y_closeStreamWithoutTryBlockInIfStatement() throws IOException {
		int a = 10;
		if(a != 20) {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();
		}
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fis.close();加上mark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	private void y_closeStreamWithoutTryBlockInForStatement() throws IOException {
		for (int a = 0; a < 10; a++) {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();
		}
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fis.close();加上mark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	private void y_closeStreamWithoutTryBlockInWhileStatement() throws IOException {
		int a = 10;
		while (a >= 10) {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();
			a--;
		}
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fis.close();加上mark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	private void y_closeStreamWithoutTryBlockInDoWhileStatement() throws IOException {
		int a = 10;
		do {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();
		} while (a ==10);
	}
	
	/**
	 * 只是要消除private method的警告用，不會有任何mark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void callAllPrivate() throws IOException {
		y_closeStreamWithoutTryBlock();
		y_closeStreamWithoutTryBlockInIfStatement();
		y_closeStreamWithoutTryBlockInForStatement();
		y_closeStreamWithoutTryBlockInWhileStatement();
		y_closeStreamWithoutTryBlockInDoWhileStatement();
	}
	
	/**
	 * close IO的標準寫法
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void openStream2Write() {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			fos.write(10000);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			closeStreamWithoutThrowingException(fos);
		}
		
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fis.close();加上mark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void y_multiNestedStatementWithoutTryBlock() throws IOException {
		for (int a = 0; a < 10; a++) {
			FileWriter fw = null;
			if (a == 5) {
				fw = new FileWriter("filepath");
			}
			if(fw != null) {
				fw.write("fileContents");
			}
			fw.close();
		}
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fis.close();加上mark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void y_multiNestedStatementWithTryBlock() throws IOException {
		for (int a = 0; a < 10; a++) {
			try {
				if (a == 5) {
					FileWriter fw = new FileWriter("filepath");
					fw.write("fileContents");
					fw.close();
				}
			} catch (IOException e) {
				throw e;
			}
		}
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fw.close();加上mark(因為catch與finally都會拋出例外)
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void y_closeStreamInFinallyButThrowsExceptionInCatchAndFinally() throws IOException {
		int a =10;
		while (a > 0) {
			FileWriter fw = null;
			try {
				if (a == 5) {
					fw = new FileWriter("filepath");
					fw.write("fileContents");
				}
			} catch (IOException e) {
				throw e;
			} finally {
				fw.close();
			}
			a--;
		}
	}

	/**
	 * 會被CarelessCleanupVisitor在method宣告處加上mark
	 * (因為使用者會想要在catch轉型使用functional code處理例外時，
	 * 應該是不希望method的宣告會拋出例外，所以finally不應該拋出例外。)
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 2, exception = java.io.IOException.class) })
	public void y_closeStreamInFinallyButThrowsExceptionOnlyInFinally() throws IOException {
		int a =10;
		while (a > 0) {
			FileWriter fw = null;
			try {
				if (a == 5) {
					fw = new FileWriter("filepath");
					fw.write("fileContents");
				}
			} catch (IOException e) {
				throw e;
			} finally {
				fw.close();
			}
			a--;
		}
	}
	
	//=========要在設定檔裡面加上使用者偵測條件，才能判斷出以下method是否有careless cleanup=========//
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void uy_userDefinedLibrary() throws IOException {
		UserDefinedCarelessCleanupWeather weather = new UserDefinedCarelessCleanupWeather();
		weather.Shine();
		weather.rain();
		weather.bark();
		
		UserDefinedCarelessCleanupDog dog = new UserDefinedCarelessCleanupDog();
		dog.bark();
	}
	
	//======要再設定檔裡面勾選Extra rule，才能判斷出以下method是否有careless cleanup======//
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void uy_closeStreaminOuterMethodInTry() throws IOException {
		try {
			FileOutputStream fi = new FileOutputStream("");
			fi.write(1);
			closeStreamWithoutThrowingException(fi);
		} catch (FileNotFoundException e) {
			throw e;
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void uy_closeStreaminOuterMethodInCatch() throws IOException {
		FileOutputStream fi = null;
		
		try {
			fi = new FileOutputStream("");
			fi.write(1);
		} catch (FileNotFoundException e) {
			closeStreamWithoutThrowingException(fi);
			throw e;
		} finally {
		}
	}
	
	//================測試moveInstance case使用的特定格式==========================
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void moveInstance() throws IOException {
		try {
			FileOutputStream fi = new FileOutputStream("");
			fi.write(1);
			fi.close();
		} catch (IOException e) {
			throw e;
		}
	}
	
	/**
	 * 若.close() method不會丟出例外，應可以直接quick fix放到finally block中
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void theCloseImplementClosableWillNotThrowException() throws IOException {
		ClassImplementCloseableWithoutThrowException anInstance = null;
		try {
			anInstance = new ClassImplementCloseableWithoutThrowException();
			anInstance.close();
		} finally {
		}
	}
}
