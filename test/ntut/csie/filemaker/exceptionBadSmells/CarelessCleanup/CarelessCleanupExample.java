package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.ClassCanCloseButNotImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.ClassImplementCloseable;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.ClassImplementCloseableWithoutThrowException;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.UserDefinedCarelessCleanupDog;
import ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.actionmethod.UserDefinedCarelessCleanupWeather;
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
//			closeStreamWithoutThrowingException(fi);
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
//			closeStreamWithoutThrowingException(fi);
			throw e;
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
