package ntut.csie.filemaker.exceptionBadSmells;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import agile.exception.RL;
import agile.exception.Robustness;

public class OverLoggingExample {
	Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	
	/* -----------------------Call Chain In the Same Class------------------------- */
	
	public void theFirstOrderInTheSameClass() {
		try {
			theSecondOrderInTheSameClass();
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage());
			int forVariableDeclarationStatement;
		}
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void theSecondOrderInTheSameClass() throws IOException {
		try {
			theThirdOrderInTheSameClass();
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage());
			throw e;
		}
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void theThirdOrderInTheSameClass() throws IOException {
		try {
			theFourthOrderInTheSameClass();
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage());
			throw e;
		}
	}
	
	@Robustness(value = { @RL(level = 1, exception = java.io.IOException.class) })
	public void theFourthOrderInTheSameClass() throws IOException {
		try {
			throw new IOException("IOException throws in callee");
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage());
			throw e;
		}
	}
	
	/* -----------------------Call Chain In the outer Class------------------------- */
	
	public void calleeInOutterClass() {
		try {
			OverLoggingTheFirstOrderClass outer = new OverLoggingTheFirstOrderClass();
			outer.callee();
		} catch(IOException e) {
			logger.log(Level.WARNING, e.getMessage());
		}
	}
}
