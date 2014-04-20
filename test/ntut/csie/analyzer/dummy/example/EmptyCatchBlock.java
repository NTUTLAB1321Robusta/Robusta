package ntut.csie.analyzer.dummy.example;

import java.io.FileOutputStream;
import java.io.IOException;

public class EmptyCatchBlock {

	public void singleEmptyCatch() throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch (IOException e) {
		}
	}

	public void multiEmptyCatch() throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch (IOException e) {
		}

		try {
			fos = new FileOutputStream("path2");
			fos.write(185);
		} catch (IOException e) {
		}
	}

	public void emptyCatchInOtherCatch() throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch (IOException e) {
			try {
				FileOutputStream fos2 = new FileOutputStream("");
				fos2.write(185);
			} catch (IOException e2) {
			}
		}
	}
}
