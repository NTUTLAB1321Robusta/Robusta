package ntut.csie.filemaker.exceptionBadSmells;

import java.io.FileInputStream;
import java.io.IOException;

public class DummyHandlerExampleWithTryStatementInNonTryStatement {

	public void true_testWhenTryStatementInIfStatement() {
		FileInputStream fis = null;
		if(fis == null) {
			try {
				fis = new FileInputStream("");
				fis.read();
			} catch (IOException e) {
				e.printStackTrace();	//	DummyHandler
			}
		}
	}
	
	public void true_testWhenTryStatementInForStatement() {
		FileInputStream fis = null;
		for(int i=0; i<1; i++) {
			try {
				fis = new FileInputStream("");
				fis.read();
			} catch (IOException e) {
				e.printStackTrace();	//	DummyHandler
			}
		}
	}
}
