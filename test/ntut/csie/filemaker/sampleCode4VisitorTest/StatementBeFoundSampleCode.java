package ntut.csie.filemaker.sampleCode4VisitorTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * �o�ӽd�ҵ{���X�O���Ӵ���Visitor�Ϊ��C�o�䪺Visitor�A�x���D�ҥ~�B�z�a���D������Visitor�C
 * �ҥ~�B�z�~���D���d�ҵ{���X�A�������Ӽg�b�o�̡C
 * @author Charles
 *
 */
public class StatementBeFoundSampleCode {

	public static void writeFile(FileOutputStream fos) {
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch (FileNotFoundException e) {
			// This expression statement start position is 574
			e.printStackTrace();
		} catch (IOException e) {
			// This expression statement start position is 683
			System.out.println(e);
		}	
	}
	
	public static void readFile(FileInputStream fis) {
		IOException e = new IOException();
		// This expression statement start position is 867
		e.printStackTrace();
	}
}
