package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

import ntut.csie.robusta.agile.exception.Tag;
import ntut.csie.robusta.agile.exception.Robustness;



/**
 * Careless Cleanup�����~�d�ҡC
 * Careless Cleanup�G��ϥΪ̹���������y�ɡA�������ʧ@�èS���bfinally�̭�����A�K�O�ݩ�o���a���D�C
 * �o�ǽd�ҳ��i�H�Q�ڭ̪�CarelessCleanupVisitor������C
 * @author Charles
 *
 */
public class CarelessCleanupExample {
	
	/**
	 * ���|�QCarelessCleanupVisitor�[�Wmark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void withoutCloseStream(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfileOutputStream.close();�[�Wmark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y_closeStreamInTryBlock(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfileOutputStream.close();�[�Wmark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y2_close2StreamInTryBlock(byte[] context, File inputFile, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		FileInputStream  fileInputStream = null;
		try {
			fileInputStream = new FileInputStream(inputFile);
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileInputStream.close();
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfileOutputStream.close();�[�Wmark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y_closeStreamInTryBlockWithEmptyFinally(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfileOutputStream.close();�[�Wmark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y_closeStreamInTryBlockWithBlankFinally(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			
		}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfileOutputStream.close();�[�Wmark
	 * @param context
	 * @param outputFile
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y_closeStreamInTryBlockWithNonblankFinally(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			fileOutputStream.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			System.out.println("Close nothing at all.");
		}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfileOutputStream.close();�[�Wmark(��B)
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = {
			@Tag(level = 1, exception = java.io.FileNotFoundException.class),
			@Tag(level = 1, exception = java.io.IOException.class) })
	public void y2_closeStreamInCatchClause(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream  = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			fileOutputStream.close();
			throw e;
		} catch (IOException e) {
			fileOutputStream.close();
			throw e;
		} finally {
			System.out.println("Close nothing at all.");
		}
	}
	
	/**
	 * ���|�QCarelessCleanupVisitor�banInstance.close();�[�Wmark
	 * �]��ClassWithNotThrowingExceptionCloseable���Oimplements Closeable����
	 * �ҥH���|�Q�����O�����귽���ʧ@
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void closeNonClosableInstance() {
		ClassWithNotThrowingExceptionCloseable anInstance = null;
		try {
			anInstance = new ClassWithNotThrowingExceptionCloseable();
			anInstance.open();
			anInstance.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void y_closeUserImplementingClosableInstance() {
		ClassImplementCloseable anInstance = null;
		try {
			anInstance = new ClassImplementCloseable();
			anInstance.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * �ݦ��M���Ψө�bFinally��������y���{���X�A���O�L�|�ߥX�ҥ~�A�ҥH�٬O�n�QCareless Cleanup�ˬd�C
	 * @param fileOutputStream
	 * @throws IOException 
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	protected void y_closeStreamThrowingExceptionDeclaring(FileOutputStream fileOutputStream) throws IOException {
		try {
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		throw new IOException();
	}
	
	/**
	 * �ݦ��M���Ψө�bFinally��������y���{���X�A���O�L�|�ߥX�ҥ~�A�ҥH�٬O�n�QCareless Cleanup�ˬd�C
	 * @param fileOutputStream
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	protected void y_closeStreamThrowingException(FileOutputStream fileOutputStream) {
		try {
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * �p�G�O�M���Ψө�bFinally������y��method�A�N����careless cleanup���ˬd�C
	 * ���O�ثe���{���X���Ψ�else�A�ҥH�ڭ̻{���y�N�W�A�i�ण�O���������y��method�C
	 * @param fileOutputStream
	 */
	protected void y_closeStreamWithElseBigTry(FileOutputStream fileOutputStream) {
		try {
			if (fileOutputStream != null) {
				fileOutputStream.close();
			} else {
				System.out.println("Stream cannot be closed.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * �p�G�O�M���Ψө�bFinally������y��method�A�N����careless cleanup���ˬd
	 * ���O�ثe���{���Xif���u��������y���{���X�A�ҥH�ڭ̻{���y�N�W�A�i�ण�O���������y��method�C
	 * @param fileOutputStream
	 */
	protected void y_closeStreamWithMultiStatementInThenBigTry(FileOutputStream fileOutputStream) {
		try {
			if (fileOutputStream != null) {
				fileOutputStream.close();
				System.out.println("Stream cannot be closed.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * �p�G�O�M���Ψө�bFinally������y��method�A�N����careless cleanup���ˬd
	 * @param fileOutputStream
	 */
	protected void closeStreamWithoutThrowingExceptionBigTry(FileOutputStream fileOutputStream) {
		try {
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * �p�G�O�M���Ψө�bFinally������y��method�A�N����careless cleanup���ˬd
	 * @param fileOutputStream
	 */
	protected void closeStreamWithoutThrowingExceptionBigTryIfWithoutBlock(FileOutputStream fileOutputStream) {
		try {
			if (fileOutputStream != null)
				fileOutputStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * �p�G�O�M���Ψө�bFinally������y��method�A�N����careless cleanup���ˬd
	 * @param fileOutputStream
	 */
	protected void closeStreamWithoutThrowingException(FileOutputStream fileOutputStream) {
		if (fileOutputStream != null) {
			try {
				fileOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * �p�G�O�M���Ψө�bFinally������y��method�A�N����careless cleanup���ˬd
	 * @param fileOutputStream
	 */
	public void closeStreamWithoutThrowingExceptionNestedIfTry(FileOutputStream fileOutputStream) {
		if (fileOutputStream != null)
			try {
				if(fileOutputStream != null) {
					fileOutputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfis.close();�[�Wmark
	 * @throws IOException 
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	private void y_closeStreamWithoutTryBlock() throws IOException {
		FileInputStream fis = new FileInputStream("");
		fis.read();
		fis.close();
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfis.close();�[�Wmark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	private void y_closeStreamWithoutTryBlockInIfStatement() throws IOException {
		int a = 10;
		if(a != 20) {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();
		}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfis.close();�[�Wmark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	private void y_closeStreamWithoutTryBlockInForStatement() throws IOException {
		for (int a = 0; a < 10; a++) {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();
		}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfis.close();�[�Wmark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	private void y_closeStreamWithoutTryBlockInWhileStatement() throws IOException {
		int a = 10;
		while (a >= 10) {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();
			a--;
		}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfis.close();�[�Wmark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	private void y_closeStreamWithoutTryBlockInDoWhileStatement() throws IOException {
		int a = 10;
		do {
			FileInputStream fis = new FileInputStream("");
			fis.read();
			fis.close();
		} while (a ==10);
	}
	
	/**
	 * �u�O�n����private method��ĵ�i�ΡA���|������mark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void callAllPrivate() throws IOException {
		y_closeStreamWithoutTryBlock();
		y_closeStreamWithoutTryBlockInIfStatement();
		y_closeStreamWithoutTryBlockInForStatement();
		y_closeStreamWithoutTryBlockInWhileStatement();
		y_closeStreamWithoutTryBlockInDoWhileStatement();
	}
	
	/**
	 * close IO���зǼg�k
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.lang.RuntimeException.class) })
	public void openStream2Write() {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream("");
			fos.write(10000);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			closeStreamWithoutThrowingException(fos);
		}
		
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfis.close();�[�Wmark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void y_multiNestedStatementWithoutTryBlock() throws IOException {
		for (int a = 0; a < 10; a++) {
			FileWriter fw = null;
			if (a == 5) {
				fw = new FileWriter("filepath");
			}
			if(fw != null) {
				fw.write("fileContents");
			}
			fw.close();
		}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfis.close();�[�Wmark
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void y_multiNestedStatementWithTryBlock() throws IOException {
		for (int a = 0; a < 10; a++) {
			try {
				if (a == 5) {
					FileWriter fw = new FileWriter("filepath");
					fw.write("fileContents");
					fw.close();
				}
			} catch (IOException e) {
				throw e;
			}
		}
	}
	
	/**
	 * �|�QCarelessCleanupVisitor�bfw.close();�[�Wmark(�]��catch�Pfinally���|�ߥX�ҥ~)
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void y_closeStreamInFinallyButThrowsExceptionInCatchAndFinally() throws IOException {
		int a =10;
		while (a > 0) {
			FileWriter fw = null;
			try {
				if (a == 5) {
					fw = new FileWriter("filepath");
					fw.write("fileContents");
				}
			} catch (IOException e) {
				throw e;
			} finally {
				fw.close();
			}
			a--;
		}
	}

	/**
	 * �|�QCarelessCleanupVisitor�bmethod�ŧi�B�[�Wmark
	 * (�]���ϥΪ̷|�Q�n�bcatch�૬�ϥ�functional code�B�z�ҥ~�ɡA
	 * ���ӬO���Ʊ�method���ŧi�|�ߥX�ҥ~�A�ҥHfinally�����өߥX�ҥ~�C)
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 2, exception = java.io.IOException.class) })
	public void y_closeStreamInFinallyButThrowsExceptionOnlyInFinally() throws IOException {
		int a =10;
		while (a > 0) {
			FileWriter fw = null;
			try {
				if (a == 5) {
					fw = new FileWriter("filepath");
					fw.write("fileContents");
				}
			} catch (IOException e) {
				throw e;
			} finally {
				fw.close();
			}
			a--;
		}
	}
	
	//=========�n�b�]�w�ɸ̭��[�W�ϥΪ̰�������A�~��P�_�X�H�Umethod�O�_��careless cleanup=========//
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void uy_userDefinedLibrary() throws IOException {
		UserDefinedCarelessCleanupWeather weather = new UserDefinedCarelessCleanupWeather();
		weather.Shine();
		weather.rain();
		weather.bark();
		
		UserDefinedCarelessCleanupDog dog = new UserDefinedCarelessCleanupDog();
		dog.bark();
	}
	
	//======�n�A�]�w�ɸ̭��Ŀ�Extra rule�A�~��P�_�X�H�Umethod�O�_��careless cleanup======//
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void uy_closeStreaminOuterMethodInTry() throws IOException {
		try {
			FileOutputStream fi = new FileOutputStream("");
			fi.write(1);
			closeStreamWithoutThrowingException(fi);
		} catch (FileNotFoundException e) {
			throw e;
		}
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void uy_closeStreaminOuterMethodInCatch() throws IOException {
		FileOutputStream fi = null;
		
		try {
			fi = new FileOutputStream("");
			fi.write(1);
		} catch (FileNotFoundException e) {
			closeStreamWithoutThrowingException(fi);
			throw e;
		} finally {
		}
	}
	
	//================����moveInstance case�ϥΪ��S�w�榡==========================
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void moveInstance() throws IOException {
		try {
			FileOutputStream fi = new FileOutputStream("");
			fi.write(1);
			fi.close();
		} catch (IOException e) {
			throw e;
		}
	}
	
	/**
	 * �Y.close() method���|��X�ҥ~�A���i�H����quick fix���finally block��
	 * @throws IOException
	 */
	@Robustness(value = { @Tag(level = 1, exception = java.io.IOException.class) })
	public void theCloseImplementClosableWillNotThrowException() throws IOException {
		ClassImplementCloseableWithoutThrowException anInstance = null;
		try {
			anInstance = new ClassImplementCloseableWithoutThrowException();
			anInstance.close();
		} finally {
		}
	}
}
