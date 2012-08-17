package ntut.csie.filemaker.exceptionBadSmells;

import agile.exception.Robustness;
import agile.exception.RL;

public class UserDefinedCarelessCleanupDog {
	public void bark() {
		
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.lang.Exception.class) })
	public void bite() throws Exception{
		throw new Exception("«r¤£¤U¥h");
	}
}
