package ntut.csie.analyzer.careless;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ClosingResourceBeginningPositionFinderExample {

	public void resourceAssignAndUseMultiTimes(File file1) throws IOException {
		FileInputStream fis = null;  // First Declared
		fis = new FileInputStream(file1);
		fis.available();
		fis.close();  // First closed
	}

	public void resourceFromParameters(File file2) throws IOException {  // Second Declared
		FileInputStream fis = null;
		fis = new FileInputStream(file2);
		file2.canRead();  // Second closed
	}

	File file3 = null;  // Third Declared
	
	public void resourceFromField() throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream(file3);
		file3.canRead();  // Third closed
	}

	class ClassWithGetResource {
		public FileInputStream getResource() {
			return null;
		}
		public void close() {
		}
		
		public void closeResourceByInvokeMyClose() throws Exception {
			this.close();
			close();
		}
	}

	public void invokeGetResourceAndCloseIt() throws Exception {  // Fourth Declared
		ClassWithGetResource resourceManager = new ClassWithGetResource();
		resourceManager.getResource().close();  // Fourth closed
	}
}
