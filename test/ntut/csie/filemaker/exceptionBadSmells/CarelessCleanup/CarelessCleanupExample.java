package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import ntut.csie.robusta.agile.exception.RTag;
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
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
			@RTag(level = 1, exception = java.io.FileNotFoundException.class),
			@RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	private void y_closeStreamWithoutTryBlock() throws IOException {
		FileInputStream fis = new FileInputStream("");
		fis.read();
		fis.close();
	}
	
	/**
	 * 會被CarelessCleanupVisitor在fis.close();加上mark
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 2, exception = java.io.IOException.class) })
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
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void uy_userDefinedLibrary() throws IOException {
		UserDefinedCarelessCleanupWeather weather = new UserDefinedCarelessCleanupWeather();
		weather.Shine();
		weather.rain();
		weather.bark();
		
		UserDefinedCarelessCleanupDog dog = new UserDefinedCarelessCleanupDog();
		dog.bark();
	}
	
	//======要再設定檔裡面勾選Extra rule，才能判斷出以下method是否有careless cleanup======//
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void uy_closeStreaminOuterMethodInTry() throws IOException {
		try {
			FileOutputStream fi = new FileOutputStream("");
			fi.write(1);
			closeStreamWithoutThrowingException(fi);
		} catch (FileNotFoundException e) {
			throw e;
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
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
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void theCloseImplementClosableWillNotThrowException() throws IOException {
		ClassImplementCloseableWithoutThrowException anInstance = null;
		try {
			anInstance = new ClassImplementCloseableWithoutThrowException();
			anInstance.close();
		} finally {
		}
	}
	
	/*=================================================================================
	 * 其他可能出現 Careless Cleanup 壞味道的程式碼結構。
	 * 發想靈感來自jfreechart。
	 =================================================================================*/
	
	/**
	 * 同一個instance，在MethodDeclaration上面有拋出例外，
	 * 又用了try-catch去捕捉這個這個instance其他method，並在finally裡面關閉。
	 * 這種也是careless cleanup的一種。
	 * @throws IOException
	 */
	public void y_thrownExceptionOnMethodDeclarationWithTryStatementWith2KindsInstance(
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
	 * 同上，但是使用者自定義的偵測條件
	 * @throws IOException
	 */
	public void y_thrownExceptionOnMethodDeclarationWithTryStatementWith2KindsInstanceUserDefined()
			throws IOException {
		UserDefinedCarelessCleanupWeather uccw1 = new UserDefinedCarelessCleanupWeather();
		
		UserDefinedCarelessCleanupWeather uccw2 = new UserDefinedCarelessCleanupWeather();
		try {
			uccw1.bark();
			uccw2.rain();
		} finally {
			uccw1.Shine();	// 在這個例子中，uccw1怎樣都不會被當作careless cleanup
			uccw2.Shine();	// 定義shine為close
		}
	}
	
	/**
	 * 傳進來的資源，要在此處關閉。
	 * @param fis
	 * @throws IOException
	 */
	public void y_inputResource(FileInputStream fis) throws IOException {
		FileOutputStream fos = new FileOutputStream("C:\\123.txt");
		fis.reset();	// 會拋例外
		try {
			while(fis.available() != 0) {
				fos.write(fis.read());
			}
		} finally {
			fos.close();	// 因為fis.reset();會拋例外，所以fos.close()是careless cleanup
		}
	}
	
	/**
	 * 同一個instance，在MethodDeclaration上面有拋出例外，
	 * 又用了try-catch去捕捉這個這個instance其他method，並在finally裡面關閉。
	 * 這種也是careless cleanup的一種。
	 * 
	 * finally裡面兩個關閉串流的動作，第一個可能會拋例外，導致第二個動作可能執行不到。
	 * @throws IOException
	 */
	public void y_thrownExceptionOnMethodDeclarationWithTryStatementWith2KindsInstanceAndLastOneNotThrowsException(
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
			fis.close(); // 如果out發生例外，則fis依然會有 careless cleanup 的壞味道	
			close(out);
		}
	}
	
	public void close(Closeable instance) {
		if(instance != null) {
			try{
				instance.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void y_thrownExceptionOnMethodDeclarationWithoutTryStatement(File file1, File file2) throws IOException {
		int a = 10;
		FileInputStream fis = new FileInputStream("");
		for(int i = 1; i<a; i++) {
			while(fis.available() != 0) {	// 會拋例外
				fis.reset();				// 會拋例外
			}
		}
		try {
			fis.read();	//會拋出例外
		} finally {
			fis.close();
		}
	}
	
	/**
	 * 同一個instance，在MethodDeclaration上面有拋出例外，
	 * 又用了try-catch去捕捉這個這個instance其他method，沒有任何關閉的動作在finally裡面。
	 * 就不算是careless cleanup的一種。
	 * @throws IOException
	 */
	public void thrownExceptionOnMethodDeclarationWithTryStatement(File file1, File file2) throws IOException {
		if((file1 == null) || (file2 == null)) {
			throw new IllegalArgumentException("Null 'file' argument.");
		}
		FileInputStream fis = new FileInputStream(file1);
		
		// 如果out發生例外，則fis依然會有 careless cleanup 的壞味道
		java.io.OutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(file2));
		try {
			out.write(fis.read());
		} finally {
			System.out.println(fis.available());
		}
	}
	
	/**
	 * 同一個instance，在MethodDeclaration上面有拋出例外，
	 * 又用了try-catch去捕捉這個這個instance其他method，沒有任何關閉的動作在finally裡面。
	 * 就不算是careless cleanup的一種。
	 * @throws IOException
	 */
	public void thrownExceptionOnMethodDeclarationWithTryStatement() throws IOException {
		int a = 10;
		FileInputStream fis = new FileInputStream("");
		for(int i = 1; i<a; i++) {
			fis.mark(1);
			System.out.println(fis.toString());
		}
		try {
			fis.read();	//會拋出例外
		} finally {
			fis.close();
		}
	}
	
	public void throwExceptionBeforeCreation(FileOutputStream fos) throws IOException {
		fos.write(10);
		int a = 10;
		FileInputStream fis = new FileInputStream("");
		for(int i = 1; i<a; i++) {
			fis.mark(1);
			System.out.println(fis.toString());
		}
		try {
			fis.read();
		} finally {
			/*
			 * 雖然fos.write(10);會拋出例外，但是發生時fis尚未被建立，所以無需關閉。
			 * 所以不算是careless cleanup
			 */
			fis.close();
		}
	}
	
	public void y_thrownExceptionInOtherTryStatement(File file1, File file2) throws IOException {
		int a = 10;
		FileInputStream fis = new FileInputStream("");
		try {
			for(int i = 1; i<a; i++) {
				while(fis.available() != 0) {	// 會拋例外
					fis.reset();				// 會拋例外
				}
			}
		} finally {
			System.out.println(fis.toString());
		}
		
		try {
			fis.read();	//會拋出例外
		} finally {
			fis.close();
		}
	}
}
