package ntut.csie.robusta.codegen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * this sample code is used to test visitor.
 * but these visitors are not used to detect bad smell 
 * @author charles
 *
 */
public class CatchClauseSampleCode {

	public static void writeFile(FileOutputStream fos) {
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	public static void readFile(FileInputStream fis) {
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
}
