package ntut.csie.analyzer.careless;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class ClosingResourceBeginningPositionFinderExample {

	public void resourceAssignAndUseMultiTimes(File file1) throws IOException {
		FileInputStream fis = null;  // First Declaration
		fis = new FileInputStream(file1);
		fis.available();
		fis.close();  // First closed
	}

	public void resourceFromParameters(File file2) throws IOException {  // Second Declaration
		FileInputStream fis = null;
		fis = new FileInputStream(file2);
		file2.canRead();  // Second closed
	}

	File file3 = null;  // Third Declaration
	
	public void resourceFromField() throws IOException {
		FileInputStream fis = null;
		fis = new FileInputStream(file3);
		file3.canRead();  // Third closed
	}

	class ClassWithGetResource {
		public FileInputStream getResource() {
			return null;
		}
		public void close() {
		}
		
		public void closeResourceByInvokeMyClose() throws Exception {
			this.close();
			close();
		}
	}

	public void invokeGetResourceAndCloseIt() throws Exception {  // Fourth Declaration
		ClassWithGetResource resourceManager = new ClassWithGetResource();
		resourceManager.getResource().close();  // Fourth closed
	}
	
	public void resourceWithMultipleAssignmentAndClose(int state) throws Exception {
		FileInputStream fis = null;
		fis = new FileInputStream("Input1");
		closeResource(fis);
		fis = new FileInputStream("Input2");
		fis.close();
	}
	
	private void closeResource(Closeable resource) throws IOException{
		resource.close();
	}
	
	public void resourceAssignmentInIfElseBlock(int state) throws Exception {
		FileInputStream fis = null;
		if(state == 1)
			fis = new FileInputStream("Input1");
		else if(state == 2)
			fis = new FileInputStream("Input2");
		else
			fis = new FileInputStream("BackupInput");
		fis.close();
	}
	
	public void resourceAssignmentInSwitchBlock(int state) throws Exception {
		FileInputStream fis = null;
		switch(state) {
			case(1):
				fis = new FileInputStream("Input1");
				break;
			case(2):
				fis = new FileInputStream("Input2");
				break;
			default:
				fis = new FileInputStream("BackupInput");
				break;
		}	
		fis.close();
	}
	
	public void initializedResourceAssignAgainInIfElseBlock(int state) throws Exception {
		FileInputStream fis = new FileInputStream("Initial");
		if(state == 1)
			fis = new FileInputStream("Input1");
		else if(state == 2)
			fis = new FileInputStream("Input2");
		else
			fis = new FileInputStream("BackupInput");
		fis.close();
	}
	
	public void resourceAssignmentByQuestionColonOperator(boolean connected) throws Exception {
		FileInputStream fis = null;
		fis = (connected)? new FileInputStream("Input1"): new FileInputStream("BackupInput");
		fis.close();
	}
	
	public void assignmentAndCloseInTheSameIfBlockAndAreSibling(boolean connected) throws Exception {
		FileInputStream fis = null;
		if(connected){
			fis = new FileInputStream("Input");
			int available = fis.available();
			if(available <= 0)
				fis.reset();
			fis.close();
		}
	}
	
	public void assignmentAndCloseInTheSameIfBlockButAreNotSibling(boolean connected) throws Exception {
		FileInputStream fis = null;
		if(connected){
			fis = new FileInputStream("Input");
			int available = fis.available();
			if(available <= 0)
				fis.close();
		}
	}
	
	public void assignmentAndCloseInDifferentIfBlockButAreAtSameDepth(boolean connected) throws Exception {
		FileInputStream fis = null;
		if(connected){
			fis = new FileInputStream("Input");
		}
		if(connected)
			fis.close();
	}
}
