package ntut.csie.filemaker.sampleCode4VisitorTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class DeclaredExceptionSampleCode {
	
	public void dumpPropertyByteData() {
		String result = readFileWithUncheckedException(new File("C:\\12312234234.txt"));
		System.out.println(result);
	}
	
	/**
	 * 這個讀檔功能會拋出RuntimeException
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
	 * 這個寫檔案功能會拋出兩種例外
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
	 * 測試throw new xxxException的形式
	 * @param filepath
	 * @return
	 */
	public String readFilename(File filepath) {
		String result = "";
		if(!filepath.exists()) {
			throw new RuntimeException("file not found"); 
		}
		result = filepath.getName();
		return result;
	}
	
	/**
	 * 測試throw new xxxException與throw e的形式
	 * @param filepath
	 * @return
	 * @throws IOException
	 */
	public String getFileDetailPath(File filepath) throws IOException {
		String result = "";
		if(!filepath.exists()) {
			throw new RuntimeException("file not found"); 
		}
		
		try {
			result = filepath.getCanonicalPath();
		} catch (IOException e) {
			throw e;
		}
		return result;
	}
}
