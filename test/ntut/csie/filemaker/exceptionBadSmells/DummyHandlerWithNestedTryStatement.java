package ntut.csie.filemaker.exceptionBadSmells;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class DummyHandlerWithNestedTryStatement {
	
	public void inTryBlock() throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			try {
				fos.write(10);
			} catch(IOException e) {
				System.out.println(e.getMessage());	// Dummy handler
			}
		} catch(FileNotFoundException e) {
			System.err.println(e.getMessage());	// Dummy handler
		} finally {
			if(fos != null)
				fos.close();
		}
	}
	
	public void inCatckClause() throws IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch(FileNotFoundException e) {
			try {
				fos = new FileOutputStream("");
				fos.write(10);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();	// Dummy handler
			}
		} finally {
			if(fos != null)
				fos.close();
		}
	}
	
	public void inFinallyBlock() {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch (FileNotFoundException e) {
			e.printStackTrace();	// Dummy handler
		} catch (IOException e) {
			e.printStackTrace();	// Dummy handler
		} finally {
			if(fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();	// Dummy handler
				}
			}
		}
	}
}
