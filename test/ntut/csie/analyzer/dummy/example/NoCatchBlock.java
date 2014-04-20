package ntut.csie.analyzer.dummy.example;

import java.io.FileOutputStream;
import java.io.IOException;

public class NoCatchBlock {

	public void emptyMethod() {
	}

	public void tryWithoutCatch() throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} finally {
			if (fos != null)
				fos.close();
		}
		System.out.println("Not in catch");
	}
}
