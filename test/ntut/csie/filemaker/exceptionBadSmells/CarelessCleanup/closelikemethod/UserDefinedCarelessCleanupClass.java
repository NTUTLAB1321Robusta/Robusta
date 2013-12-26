package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

public class UserDefinedCarelessCleanupClass {
	
	public void bark() {
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.Exception.class) })
	public void bite() throws Exception{
		throw new Exception("bite");
	}
	
}
