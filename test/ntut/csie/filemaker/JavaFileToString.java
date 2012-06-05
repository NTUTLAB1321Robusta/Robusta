package ntut.csie.filemaker;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

/**
 * Ū���M�פ�java�ɪ����e�C
 * ��KŪ���@��bad smell��code�A�bUnit test�ɡA�g�����եΪ�code�C
 * @author Charles
 *
 */
public class JavaFileToString {
	/**	�Ψ�Ū��class����y */
	FileInputStream fileInputStream;
	/** ���y��y�A�ন��r */
	Scanner scanner;
	/** �x�s�ɮת���r */
	StringBuilder sb;

	public JavaFileToString() {
		fileInputStream = null;
		scanner = null;
		sb = new StringBuilder();
	}

	/**
	 * Ū��java�ɪ����e�A���O�|���LPackage����C���ɦW�|�۰ʻ{�w��.java�C
	 * �аѦ�{@link #read(Class, String, String)}
	 * @param clazz �nŪ��class
	 * @param folder src��Ƨ��άOtest��Ƨ�
	 * @throws FileNotFoundException
	 */
	public void read(Class<?> clazz, String folder) throws FileNotFoundException {
		read(clazz, folder, ".java");
	}
	
	/**
	 * Ū��java�ɪ����e�A���O�|���LPackage����C
	 * @param clazz �nŪ��class
	 * @param folder src��Ƨ��άOtest��Ƨ�
	 * @param extension �o��class�����ɦW�C
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

			// ���Lpackage�����椣�n�x�s
			scanner.nextLine();
			
			// ���ɮצ�y�A��r�@��@��Ū�X��
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
	 * ���o�ɮת���r���e�C
	 * @return
	 */
	public String getFileContent() {
		return sb.toString();
	}
	
	/**
	 * ������y
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
