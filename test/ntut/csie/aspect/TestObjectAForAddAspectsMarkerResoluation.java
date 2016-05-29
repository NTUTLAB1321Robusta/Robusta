package ntut.csie.aspect;

import java.io.IOException;

public class TestObjectAForAddAspectsMarkerResoluation {

	public TestObjectBForAddAspectsMarkerResoluation getTestObjectB() {
		TestObjectBForAddAspectsMarkerResoluation b = new TestObjectBForAddAspectsMarkerResoluation();
		return b;
	}
	
	public TestObjectBForAddAspectsMarkerResoluation getTestObjectBWillThrowIOException() throws IOException{
		TestObjectBForAddAspectsMarkerResoluation b = new TestObjectBForAddAspectsMarkerResoluation();
		return b;
	}

	public void doSomethingWillThrowIOException() throws IOException{

	}

	public void doSomethingWillNotThrowIOException() {

	}
}
