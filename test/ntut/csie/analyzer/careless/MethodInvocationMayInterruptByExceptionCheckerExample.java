package ntut.csie.analyzer.careless;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ntut.csie.analyzer.careless.closingmethod.ResourceCloser;

public class MethodInvocationMayInterruptByExceptionCheckerExample {

	File file = null;
	MethodInvocationBeforeClose methodBeforeClose = new MethodInvocationBeforeClose();
	
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
			ResourceCloser.closeResourceDirectly(is); // Isn't even user defined
		}
	}

	public void createAndCloseDirectlyWithNewFile() throws Exception {
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		fis.close(); // Isn't
	}

	public void sameResourceCloseManyTimes(byte[] context, File outputFile)
			throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close(); // Is
		} catch (FileNotFoundException e) {
			System.out.println("FileNotFoundException.");
			fileOutputStream.close(); // Is
			throw e;
		} catch (IOException e) {
			fileOutputStream.close(); // Isn't
			throw e;
		} finally {
			System.out.println("Close nothing at all.");
			fileOutputStream.close(); // Is
		}
	}
}
