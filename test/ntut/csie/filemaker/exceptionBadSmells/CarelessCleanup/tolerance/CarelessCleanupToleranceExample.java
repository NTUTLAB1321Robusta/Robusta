package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.tolerance;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class CarelessCleanupToleranceExample {

	public void readFile() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File("C:\\123.txt"));
			while(fis.available() != 0) {
				fis.read();
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e); 
		} finally {
			closeStream_valid(fis);
		}
	}
	
	public void readFile2() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(new File("C:\\123.txt"));
			while(fis.available() != 0) {
				fis.read();
			}
			closeStream_valid(fis);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e); 
		}
	}
	
	public void closeStream_valid(Closeable instance) {
		if(instance != null) {
			try {
				instance.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void closeStream_valid2(Closeable instance) {
		try {
			if (instance != null) {
				instance.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeStream_valid3(Closeable instance) {
		try {
			if (instance != null)
				instance.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeStream_invalid(Closeable instance) {
		try {
			if (instance != null)
				instance.close();
			int a = 10;
			a++;
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeStream_invalid2(Closeable instance) {
		try {
			if (instance != null) {
				System.out.println(instance.toString());
				instance.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void closeStream_invalid3(Closeable instance) {
		try {
			if (instance != null) {
				instance.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
