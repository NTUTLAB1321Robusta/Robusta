package ntut.csie.filemaker.sampleCode4VisitorTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * �� TryStatementExceptionsVisitor�ϥΪ��d�ҵ{���X�C
 * �D�n�O���RTryStatement����ӵ��c�O���O�i��ߥX�ҥ~�C
 * @author charles
 *
 */
public class TryStatementExceptionsSampleCode {
	
	public void dumpPropertyByteData() {
		String result = readFileWithUncheckedException(new File("C:\\12312234234.txt"));
		System.out.println(result);
	}
	
	/**
	 * �o��Ū�ɥ\��|�ߥXRuntimeException
	 * @return
	 * @throws RuntimeException
	 */
	public String readFileWithUncheckedException(File path) throws RuntimeException {
		StringBuilder sb = new StringBuilder();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(path);
			while(fis.available() != 0) {
				sb.append(fis.read());
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if(fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return sb.toString();
	}
	
	public void initialPropertyFile() throws IOException {
		writeFile(new File("C:\\property.txt"), 10);
	}

	/**
	 * �o�Ӽg�ɮץ\��|�ߥX��بҥ~
	 * @param outputPath
	 * @param content
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void writeFile(File outputPath, int content) throws FileNotFoundException, IOException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(outputPath);
			fos.write(content);
		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * ���եu��TryFinally�����p
	 * @param fos
	 * @param outputPath
	 * @throws IOException
	 */
	public void writeTextFile(FileOutputStream fos, File outputPath) throws IOException {
		try {
			fos.write(10);
		} finally {
			fos.write(20);
		}
	}
	
	/**
	 * ����Try����Try
	 */
	public void nestedTryStatement() {
		InputStream is = null;
		String defaultPath = "/home/charles/default.txt";
		String firstChoosenPath = "/home/charles/123456.txt";
		try {
			for(int i = 0; i<2; i++) {
				try {
					is = new FileInputStream(firstChoosenPath);
				} catch (FileNotFoundException e) {
					is = new FileInputStream(defaultPath);
					is.read();
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void complicatedNestedTryStatement() {
		InputStream is = null;
		try {
			is = new FileInputStream("/home/charles/download/abc.txt");
			is.read();
		} catch (FileNotFoundException e) {
			File alternativeFile1 = new File("/home/charles/download/a12.txt");
			if(alternativeFile1.exists()) {
				try {
					is = new FileInputStream(alternativeFile1);
					is.read();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		} catch (IOException e) {
			File alternativeFile2 = new File("/home/charles/download/123.txt");
			while (alternativeFile2.exists()){
				try {
					is = new FileInputStream(alternativeFile2);
					is.read();
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					if(alternativeFile2.exists()) {
						alternativeFile2.delete();
					}
				}
			}
			e.printStackTrace();
		}
	}
	
	public void nestedFinallyInNestedTry() throws FileNotFoundException, IOException {
		FileInputStream fis = null;
		FileOutputStream fos = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			for(int i = 0; i<2; i++) {	// This situation should never happened
				try {
					fis.close();
				} catch (IOException e) {
					try {
						fos = new FileOutputStream("");
						fos.write(Byte.valueOf(e.getMessage()));
					} catch (FileNotFoundException e1) {
						throw e1;
					} catch (NumberFormatException e1) {
						throw new RuntimeException(e1);
					} catch (IOException e1) {
						throw new RuntimeException(e1);
					} finally {
						try {
							fos.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
					throw e;
				}
			}
		}
	}
}
