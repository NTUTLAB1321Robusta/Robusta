package ntut.csie.filemaker.sampleCode4VisitorTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * �o�ӽd�ҵ{���X�O���Ӵ���Visitor�Ϊ��C�o�䪺Visitor�A�x���D�ҥ~�B�z�a���D������Visitor�C
 * �ҥ~�B�z�~���D���d�ҵ{���X�A�������Ӽg�b�o�̡C
 * @author charles
 *
 */
public class CatchClauseSampleCode {

	public static void writeFile(FileOutputStream fos) {
		try {
			fos = new FileOutputStream("");
			fos.write(10);
		} catch (FileNotFoundException e) {
			// This cc start position is 476
			e.printStackTrace();
		} catch (IOException e) {
			// This cc start position is 577
			e.printStackTrace();
		}	
	}
	
	public static void readFile(FileInputStream fis) {
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			// This cc start position is 794
			System.out.println(e);
		}
	}
}
