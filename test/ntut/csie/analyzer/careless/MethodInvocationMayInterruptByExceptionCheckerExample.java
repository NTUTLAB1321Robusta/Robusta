package ntut.csie.analyzer.careless;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ntut.csie.analyzer.careless.closingmethod.ResourceCloser;

public class MethodInvocationMayInterruptByExceptionCheckerExample {

	public void resourceAssignAndUseMultiTimes(File file1) throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream(file1);
		fis.available();
		fis.close();  // Is
	}

	public void resourceFromParameters(File file2) throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream(file2);
		file2.canRead();  // Is
	}

	File file3 = null;
	
	public void resourceFromField() throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream(file3);
		file3.canRead();  // Is
	}

	class ClassWithGetResource {
		public java.nio.channels.Channel getResourceWithInterface() {
			return null;
		}
		public FileOutputStream getResourceWithImp() {
			return null;
		}
		public ClassWithGetResource getResourceNotImpCloseable() {
			return this;
		}
		public void close() {
		}
		
		public void closeResourceByInvokeMyClose() throws Exception {
			this.close(); // Is not
			close(); // Is
		}
	}

	public void invokeGetResourceAndCloseItWithImp() throws Exception {
		ClassWithGetResource resourceManager = new ClassWithGetResource();
		resourceManager.getResourceWithImp().close();  // Is
	}
	
	public void invokeGetResourceAndCloseItWithInterface() throws Exception {
		ClassWithGetResource resourceManager = new ClassWithGetResource();
		resourceManager.getResourceWithInterface().close();  // Is
	}
	
	public void invokeGetResourceAndCloseItNotImpCloseable() throws Exception {
		ClassWithGetResource resourceManager = new ClassWithGetResource();
		resourceManager.getResourceNotImpCloseable().close(); // Is
	}

	public void closeByUserDefinedMethod(OutputStream zOut)
			throws IOException {
		InputStream is = null;
		try {
			zOut.write(is.read());
		} finally {
			ResourceCloser.closeResourceDirectly(is); // Safe even user defined
		}
	}
}
