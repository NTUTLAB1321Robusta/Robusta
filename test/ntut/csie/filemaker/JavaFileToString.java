package ntut.csie.filemaker;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

/**
 * read java file content in project。
 * use these content as test sample for unit test 
 * @author Charles
 */
public class JavaFileToString {
	/** store the content of file */
	StringBuilder stringBuilder;

	public JavaFileToString() {
		stringBuilder = new StringBuilder();
	}

	/**
	 * read content of java file and this feature will ignore "package xxxxxx" statement.
	 * this method will take .java as default file extend name.。
	 * please consult{@link #read(Class, String, String)}
	 * @param clazz class will be read
	 * @param folder class is coming from src folder or test folder
	 * @throws FileNotFoundException
	 */
	public void read(Class<?> clazz, String folder) throws FileNotFoundException {
		read(clazz, folder, ".java");
	}
	
	/**
	 * read content of java file and this feature will ignore "package xxxxxx" statement.
	 * @param clazz class will be read
	 * @param folder class is coming from src folder or test folder
	 * @param extension extension's name of class 
	 * @throws FileNotFoundException
	 */
	public void read(Class<?> clazz, String folder, String extension) throws FileNotFoundException {
		FileInputStream fileInputStream = null;
		Scanner scanner = null;
		
		String classCanonicalName = clazz.getCanonicalName();
		String classPath = classCanonicalName.replace('.', '/');
		String lineSeparator = System.getProperty("line.separator");

		File classFile = new File("./" + folder + "/" + classPath + extension);
		fileInputStream = new FileInputStream(classFile);
		scanner = new Scanner(fileInputStream);
		
		try {
			// ignore package xxxx
			scanner.nextLine();
			
			while(scanner.hasNextLine()) {
				stringBuilder.append(scanner.nextLine() + lineSeparator);
			}
		} finally {
			scanner.close();
			closeStream(fileInputStream);
		}
	}
	
	public String getFileContent() {
		return stringBuilder.toString();
	}
	
	public void clear() {
		stringBuilder = new StringBuilder();
	}
	
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
