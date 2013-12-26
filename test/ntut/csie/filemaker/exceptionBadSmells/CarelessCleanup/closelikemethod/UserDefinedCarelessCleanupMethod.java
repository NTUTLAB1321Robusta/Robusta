package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup.closelikemethod;

import java.io.IOException;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

public class UserDefinedCarelessCleanupMethod {
	
	public void bark() {
	}
	
	public void rain() {
	}
	
	public void Shine() throws IOException {
		throw new IOException("There are some clouds");
	}
	
}
