package ntut.csie.robusta.codegen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * this sample code is used to test normal visitor, it isn't related to a visitor which detects the bad smell.
 * @author Charles
 *
 */
public class StatementBeFoundSampleCode {

	public static void writeFile(FileOutputStream fos) {
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println(e);
		}	
	}
	
	public static void readFile(FileInputStream fis) {
		IOException e = new IOException();
		e.printStackTrace();
	}
}
