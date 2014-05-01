package ntut.csie.robusta.codegen.refactoring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;

public class ThrowExceptionInFinallyBlockExampleForRefactoring {

	// Method Invocation
	public void methodInvocation(File outputFile) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
		try {
		} finally {
			fileOutputStream.close(); // ThrownInFinally
		}
	}

	//Super method invocation
	public void superMethodInvocation() throws FileNotFoundException {
		FilterInputStream input =  new FilterInputStream(new FileInputStream(new File(""))) {
			public void close() throws IOException {
			}

			protected void finalize() throws Throwable {
				try {
					close();
				} finally {
					super.finalize(); // ThrownInFinally
				}
			}
		};
	}
	
	//Simple name
	public void simpleName() throws IOException {
		FileInputStream input = new FileInputStream(new File(""));
		try {
			//Do anything with input
		} finally {
			close(input);
		}
	}
	
	private void close(FileInputStream input) throws IOException {
		input.close();
	}

}
