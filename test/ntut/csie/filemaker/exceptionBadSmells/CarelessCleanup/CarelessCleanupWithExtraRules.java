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
	 * �|�o��Careless Cleanup�����p
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
	 * ���|�Q�{�wCareless Cleanup
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
	 * �]��FileInputStream fis = new FileInputStream(file1);���槹���H��A
	 * java.io.OutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(file2));�i��o�ͨҥ~�A
	 * �ҥHfis.close();�|�ܦ�careless cleanup�C
	 * <br /><br />
	 * �]��fis.close()�i��|�o�ͨҥ~�A
	 * �ҥHout.close()�|�ܦ�careless cleanup�C
	 * <br /><br />
	 * ���OCarelessCleanupCloseInFinallyRaisedExceptionNotInTryCaused�u�|������fis.close()�C
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
		
		// �p�Gout�o�ͨҥ~�A�hfis�̵M�|�� careless cleanup ���a���D
		java.io.OutputStream out = new java.io.BufferedOutputStream(new FileOutputStream(file2));
		try {
			while(fis.available() != 0) {
				out.write(fis.read());
			}
		} finally {
			fis.close();	// �]��new FileOutputStream�|�ߥX�ҥ~�A�y��fis.close()�ܦ�careless cleanup, it's CC
			out.close();	// �]��fis.close()�|�ߥX�ҥ~�A�y��out.close()�ܦ�careless cleanup, it's CC
		}
	}
	
	/**
	 * StringBuilder�b�إߪ��ɭԤ��|�o�ͨҥ~�A�ҥH�o��ӨҤl���Ocareless cleanup
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
	 * �o�Ҥl�|���ڭ̤u��ߥX NullPointerException
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
	 * �o�Ҥl�|���ڭ̤u��ߥX NullPointerException
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
