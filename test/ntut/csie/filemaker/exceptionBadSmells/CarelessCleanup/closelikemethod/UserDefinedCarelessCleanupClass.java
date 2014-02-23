package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod;

public class UserDefinedCarelessCleanupClass {
	
	public void bark() {
	}
	
	public void bite() throws Exception{
		throw new Exception("bite");
	}
	
}
