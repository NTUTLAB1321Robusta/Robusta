package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import ntut.csie.robusta.agile.exception.Tag;
import ntut.csie.robusta.agile.exception.Robustness;

public class UserDefinedCarelessCleanupDog {
	public void bark() {
		
	}
	
	@Robustness(value = { @Tag(level = 1, exception = java.lang.Exception.class) })
	public void bite() throws Exception{
		throw new Exception("«r¤£¤U¥h");
	}
}
