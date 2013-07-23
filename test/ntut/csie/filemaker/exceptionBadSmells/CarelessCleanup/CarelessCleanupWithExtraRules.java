package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;

import org.apache.log4j.lf5.util.Resource;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

public class CarelessCleanupWithExtraRules {
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void withoutExtraRule(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();	// it's CC
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void theMethodExtraRuleButItsInterfaceWontThrowExcepion(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			FileUtils.close(fileOutputStream);	// it isn't CC
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void theMethodIsExtraRuleAndItsInterfaceWillThrowExcepion(byte[] context, File outputFile) throws IOException {
		FileInputStream fileinputStream  = null;
		try {
			fileinputStream = new FileInputStream(outputFile);
			fileinputStream.read(context);
			FileUtils.close(fileinputStream);	// it's CC
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void theMethodIsExtraRuleWithoutThrowingExceptionAndItsNameContainsClose(byte[] context, File outputFile) {
		FileInputStream fileinputStream  = null;
		try {
			fileinputStream = new FileInputStream(outputFile);
			fileinputStream.read(context);
			FileUtils.closeFile(fileinputStream);	// it's CC, but it isn't marked
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void theMethodIsExtraRuleWithThrowingExceptionAndItsNameContainsClose(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			FileUtils.closeFile(fileOutputStream);	// it isn't CC
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void beforeYes(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			fileOutputStream.close();
			FileUtils.close(fileOutputStream);	// it's CC
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void afterYes(byte[] context, File outputFile) throws IOException {
		FileInputStream fileInputStream  = null;
		try {
			fileInputStream = new FileInputStream(outputFile);
			fileInputStream.read(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			FileUtils.closeFile(fileInputStream);
			fileInputStream.close(); // it isn't CC
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void afterThrowing(byte[] context, File outputFile) throws IOException {
		if(context == null)
			throw new IOException();
		FileInputStream fileInputStream  = null;
		try {
			fileInputStream = new FileInputStream(outputFile);
			fileInputStream.read(context);
		} finally {
			FileUtils.close(fileInputStream);	// it isn't CC
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void beforeThrowing(byte[] context, File outputFile) throws IOException {
		FileInputStream fileInputStream  = null;
		try {
			fileInputStream = new FileInputStream(outputFile);
			fileInputStream.read(context);
		} finally {
			FileUtils.close(fileInputStream);	// it isn't CC
		}
		if(context == null)
			throw new IOException();
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void both(byte[] context, File outputFile) throws IOException {
		FileUtils.closeFile(new FileInputStream(outputFile));
		FileInputStream fileInputStream  = null;
		try {
			fileInputStream = new FileInputStream(outputFile);
			fileInputStream.read(context);
		} finally {
			FileUtils.close(fileInputStream);	// it isn't CC
		}
		if(context == null)
			throw new IOException();
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void ifafterThrowing(byte[] context, File outputFile) throws IOException {
		if(context == null)
			throw new IOException();
		if(outputFile != null) {
			FileInputStream fileInputStream  = null;
			try {
				fileInputStream = new FileInputStream(outputFile);
				fileInputStream.read(context);
			} finally {
				FileUtils.close(fileInputStream);	// it isn't CC
			}
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void ifbeforeThrowing(byte[] context, File outputFile) throws IOException {
		if(outputFile != null) {
			FileInputStream fileInputStream  = null;
			try {
				fileInputStream = new FileInputStream(outputFile);
				fileInputStream.read(context);
			} finally {
				FileUtils.close(fileInputStream);	// it isn't CC
			}
		}
		if(context == null)
			throw new IOException();
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public void whileafterThrowing(byte[] context, File outputFile) throws IOException {
		FileInputStream fileInputStream  = new FileInputStream(outputFile);
		if(context == null)
			throw new IOException();
		while(outputFile != null) {
			try {
				fileInputStream.read(context);
			} finally {
				FileUtils.close(fileInputStream);	// it's CC
			}
		}
	}
	/**
	 * 會發生Careless Cleanup的情況
	 * @throws IOException
	 */
	public void true_readFile() throws IOException {
		FileInputStream fis = new FileInputStream("C:\\12312.txt");
		if(fis.available() == 0) {
			return;
		}
		StringBuilder sb = new StringBuilder();
		try {
			while(fis.available() == 0) {
				sb.append(fis.read());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			fis.close();	// it's CC
		}
	}
	
	/**
	 * 不會被認定Careless Cleanup
	 * @throws IOException
	 */
	public void readFile2() throws IOException {
		FileInputStream fis = new FileInputStream("C:\\12312.txt");
		StringBuilder sb = new StringBuilder();
		try {
			while(fis.available() == 0) {
				sb.append(fis.read());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			fis.close();	// it isn't CC
		}
	}
	
	/**
	 * 因為FileInputStream fis = new FileInputStream(file1);執行完畢以後，
	 * java.io.OutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(file2));可能發生例外，
	 * 所以fis.close();會變成careless cleanup。
	 * <br /><br />
	 * 因為fis.close()可能會發生例外，
	 * 所以out.close()會變成careless cleanup。
	 * <br /><br />
	 * 但是CarelessCleanupCloseInFinallyRaisedExceptionNotInTryCaused只會偵測到fis.close()。
	 * @param file1
	 * @param file2
	 * @throws IOException
	 */
	public void true_thrownExceptionOnMethodDeclarationWithTryStatementWith2KindsInstance(
			File file1, File file2) throws IOException {
		if((file1 == null) || (file2 == null)) {
			throw new IllegalArgumentException("Null 'file' argument.");
		}
		FileInputStream fis = new FileInputStream(file1);
		
		// 如果out發生例外，則fis依然會有 careless cleanup 的壞味道
		java.io.OutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(file2));
		try {
			while(fis.available() != 0) {
				out.write(fis.read());
			}
		} finally {
			fis.close();	// 因為new FileOutputStream會拋出例外，造成fis.close()變成careless cleanup, it's CC
			out.close();	// 因為fis.close()會拋出例外，造成out.close()變成careless cleanup, it's CC
		}
	}
	
	/**
	 * StringBuilder在建立的時候不會發生例外，所以這整個例子不是careless cleanup
	 * @throws IOException
	 */
	public void create2InstanceWithoutThrowingExceptions() throws IOException {
		FileInputStream fis = new FileInputStream("C:\\123123123");
		StringBuilder sb = new StringBuilder();
		try{
			sb.append("1111");
			while (fis.available() != 0) {
				fis.read();
			}
		} finally {
			fis.close();	// it isn't CC
		}
	}
	
	public void test() throws IOException, InterruptedException {
		StringBuilder sb = new StringBuilder();
		sb.wait();
		FileInputStream fis = new FileInputStream("C:\\123123123");
		try{
			sb.append("1111");
			while (fis.available() != 0) {
				fis.read();
			}
		} finally {
			fis.close();	// it isn't CC
		}
	}

	public void arbintest1() throws FileNotFoundException, IOException {
		URLConnection connection = null;
		InputStream is = null;
		try {
			is = connection.getInputStream();
		} finally {
			FileUtils.close(is); // it isn't CC
		}
	}

	public void arbintest2() throws FileNotFoundException, IOException {
		OutputStream os = new FileOutputStream("123");
		URLConnection connection = null;
		InputStream is = null;
		try {
			is = connection.getInputStream();
		} finally {
			FileUtils.close(os); // it isn't CC
			FileUtils.close(is); // it isn't CC
		}
	}

	public void arbintest3(boolean trueOrFalse) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (trueOrFalse) {
			throw new IOException("IOException");
		}
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		try {
			bais.read();
		} finally {
			FileUtils.close(bais); // it isn't CC
		}
	}

	/**
	 * 這例子會讓我們工具拋出 NullPointerException
	 */
	public void arbintest4(Resource resource, OutputStream zOut)
			throws IOException {
		InputStream rIn = resource.getInputStream();
		try {
			rIn.read();
		} finally {
			rIn.close(); // it isn't CC
		}
	}

	/**
	 * 這例子會讓我們工具拋出 NullPointerException
	 */
	public void arbintest5(Resource resource, OutputStream zOut)
			throws IOException {
		InputStream in = null;
		try {
			in = resource.getInputStream();
		} finally {
			FileUtils.close(in); // it isn't CC
		}
	}
}
