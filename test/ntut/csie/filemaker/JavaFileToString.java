package ntut.csie.filemaker;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

/**
 * 讀取專案內java檔的內容。
 * 方便讀取一些bad smell的code，在Unit test時，寫成測試用的code。
 * @author Charles
 *
 */
public class JavaFileToString {
	/**	用來讀取class的串流 */
	FileInputStream fileInputStream;
	/** 掃描串流，轉成文字 */
	Scanner scanner;
	/** 儲存檔案的文字 */
	StringBuilder sb;

	public JavaFileToString() {
		fileInputStream = null;
		scanner = null;
		sb = new StringBuilder();
	}

	/**
	 * 讀取java檔的內容，但是會略過Package那行。副檔名會自動認定為.java。
	 * 請參考{@link #read(Class, String, String)}
	 * @param clazz 要讀的class
	 * @param folder 要讀的class是來自src資料夾或是test資料夾
	 * @throws FileNotFoundException
	 */
	public void read(Class<?> clazz, String folder) throws FileNotFoundException {
		read(clazz, folder, ".java");
	}
	
	/**
	 * 讀取java檔的內容，但是會略過Package那行。
	 * @param clazz 要讀的class
	 * @param folder src資料夾或是test資料夾
	 * @param extension 這個class的副檔名。
	 * @throws FileNotFoundException
	 */
	public void read(Class<?> clazz, String folder, String extension) throws FileNotFoundException {
		String classCanonicalName = clazz.getCanonicalName();
		String classPath = classCanonicalName.replace('.', '/');
		String lineSeparator = System.getProperty("line.separator");
		try {
			File classFile = new File("./" + folder + "/" + classPath + extension);
			fileInputStream = new FileInputStream(classFile);
			scanner = new Scanner(fileInputStream);

			// 跳過package的那行不要儲存
			scanner.nextLine();
			
			// 把檔案串流，文字一行一行讀出來
			while(scanner.hasNextLine()) {
				sb.append(scanner.nextLine() + lineSeparator);
			}
		} catch (FileNotFoundException e) {
			throw e;
		} finally {
			scanner.close();
			closeStream(fileInputStream);
		}
	}
	
	/**
	 * 取得檔案的文字內容。
	 * @return
	 */
	public String getFileContent() {
		return sb.toString();
	}
	
	/**
	 * 清除已經讀取過的內容。
	 * @return
	 */
	public void clear() {
		sb = new StringBuilder();
	}
	
	/**
	 * 關閉串流
	 * @param cloz
	 */
	private void closeStream(Closeable cloz) {
		try {
			if (cloz != null) {
				cloz.close();
			}
		} catch (IOException e) {
			System.err.println(e);
		}
	}
}
