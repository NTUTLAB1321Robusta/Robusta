package ntut.csie.csdet.visitor.aidvisitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MethodInvocationMayInterruptByExceptionCheckerExample {

	public void resourceAssignAndUseMultiTimes(File file1) throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream(file1);
		fis.available();
		fis.close();
	}

	public void resourceFromParameters(File file2) throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream(file2);
		file2.canRead();
	}

	File file3 = null;
	
	public void resourceFromField() throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream(file3);
		file3.canRead();
	}

}
