package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileUtils {
	public static void close(FileOutputStream fos) {
		try {
			fos.close();
		} catch (IOException e) {
			
		}
	}
	
	public static void closeFile(FileOutputStream fos) throws IOException {
		fos.close();
	}
	
	public static void close(FileInputStream fis) throws IOException {
		fis.close();
	}
	
	public static void closeFile(FileInputStream fis) {
		try {
			fis.close();
		} catch (IOException e) {
			
		}
	}
}
