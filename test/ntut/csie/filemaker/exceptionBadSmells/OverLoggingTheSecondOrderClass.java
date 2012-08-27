package ntut.csie.filemaker.exceptionBadSmells;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import agile.exception.RL;
import agile.exception.Robustness;

public class OverLoggingTheSecondOrderClass {
	Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void callee() throws IOException {
		try {
			throw new IOException("IOException throws in second order class");
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage());
			throw e;
		}
	}
}
