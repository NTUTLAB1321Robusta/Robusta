package ntut.csie.filemaker.sampleCode4VisitorTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class ClassInstanceCreationSampleCode {
	
	public void createInstanceOneLine() throws IOException {
		FileOutputStream fos = new FileOutputStream("C:\\123456.txt");
		fos.write(10);
		fos.flush();
		fos.close();
	}
	
	public void createInstanceTwoLine() throws IOException {
		FileOutputStream fos = null;
		fos = new FileOutputStream("C:\\123456.txt");
		fos.write(20);
		fos.flush();
		fos.close();
	}
	
	public void createInstanceTwoLineAndNewNew() throws IOException {
		File f = null;
		FileOutputStream fos = null;
		fos = new FileOutputStream( f = new File("C:\\123123.txt"));
		if(f.exists()) {
			fos.write(20);
			fos.flush();
			fos.close();
		}
	}
}
