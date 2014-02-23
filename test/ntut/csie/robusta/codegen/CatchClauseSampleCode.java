package ntut.csie.robusta.codegen;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 這個範例程式碼是拿來測試Visitor用的。這邊的Visitor，泛指非例外處理壞味道偵測的Visitor。
 * 例外處理外味道的範例程式碼，都不應該寫在這裡。
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
