package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.IOException;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

/**
 * This class is not implemented Closeable.
 * User set this Class a careless cleanup class/method. 
 * @author charles
 *
 */
public class UserDefinedCarelessCleanupWeather {
	public void rain() {
		
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void Shine() throws IOException {
		throw new IOException("¹J¨ì¯Q¶³");
	}
	
	public void bark() {
		
	}
}
