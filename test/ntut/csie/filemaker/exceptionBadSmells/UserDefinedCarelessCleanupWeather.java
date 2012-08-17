package ntut.csie.filemaker.exceptionBadSmells;

import java.io.IOException;
import agile.exception.Robustness;
import agile.exception.RL;

/**
 * This class is not implemented Closeable.
 * User set this Class a careless cleanup class/method. 
 * @author charles
 *
 */
public class UserDefinedCarelessCleanupWeather {
	public void rain() {
		
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void Shine() throws IOException {
		throw new IOException("¹J¨ì¯Q¶³");
	}
	
	public void bark() {
		
	}
}
