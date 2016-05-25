package ntut.csie.analyzer.careless;

import java.io.IOException;

import ntut.csie.analyzer.careless.closingmethod.AppendCloseInvocationObject;

public class CarelessCleanupExampleFoundInEzScrum {
	public void closeAppendedAfterMethodInvocationAndThereISAMethodInvocationAbove() throws IOException {
		AppendCloseInvocationObject object = new AppendCloseInvocationObject();
		object.getClosableObj().doSomethingWithoutThrowsExceptionOnSignature();
		object.getClosableObj().close();
	}

	public void closeAppendedAfterMethodInvocationAndThereISAMethodInvocationAboveWhichWillThrowIOException() throws IOException {
		AppendCloseInvocationObject object = new AppendCloseInvocationObject();
		object.getClosableObj().doSomethingWhichThrowsExceptionOnSignature();
		object.getClosableObj().close();
	}
}
