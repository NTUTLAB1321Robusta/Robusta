package robusta.specificNodeCounter;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Date;
import java.util.TimeZone;

public class SpecificNodeExample {

	public void oneTryStatement() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
		}
	}

	public void nestedTryStatement() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
		} catch (Exception e2) {
		} finally {
			try {
				fis.close();
			} catch (IOException e2) {
			}
		}
	}

	public void tryWithoutCatchClause() throws IOException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} finally {
			fis.close();
		}
	}

	public void statementInTryBlockInCatchBlock(String path, String backupPath)
			throws FileNotFoundException {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(path);
		} catch (FileNotFoundException e1) {
			try {
				fis = new FileInputStream(backupPath);
			} catch (FileNotFoundException e2) {
				throw new FileNotFoundException("Both path fail");
			}
		}
	}

	public String reflectInstance(Class c, Date millisecond, TimeZone zone) {
		String result = null;
		try {
			Constructor constructor = c.getDeclaredConstructor(new Class[] {
					Date.class, TimeZone.class });
			result = constructor
					.newInstance(new Object[] { millisecond, zone }).toString();
		} catch (Exception e) {
			System.out.println("Something wrong.");
		}
		return result;
	}

	public String switchStatement(int index) {
		String translatedText = null;
		switch (index) {
		case 1:
			translatedText = "The One";
			break;
		case 2:
			translatedText = "Twins";
			break;
		default:
			translatedText = "Nothing";
			break;
		}
		return translatedText;
	}

	public void emptyStatement() {
		if (true) {
			;
			;
			;
		}
	}
}
