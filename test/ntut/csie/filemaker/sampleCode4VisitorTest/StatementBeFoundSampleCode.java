package ntut.csie.filemaker.sampleCode4VisitorTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 這個範例程式碼是拿來測試Visitor用的。這邊的Visitor，泛指非例外處理壞味道偵測的Visitor。
 * 例外處理外味道的範例程式碼，都不應該寫在這裡。
 * @author Charles
 *
 */
public class StatementBeFoundSampleCode {

	public static void writeFile(FileOutputStream fos) {
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch (FileNotFoundException e) {
			// This expression statement start position is 574
			e.printStackTrace();
		} catch (IOException e) {
			// This expression statement start position is 683
			System.out.println(e);
		}	
	}
	
	public static void readFile(FileInputStream fis) {
		IOException e = new IOException();
		// This expression statement start position is 867
		e.printStackTrace();
	}
}
