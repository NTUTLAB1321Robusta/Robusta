package ntut.csie.filemaker.exceptionBadSmells.OverLogging;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

public class OverLoggingTheThirdOrderClass {
	Logger javaLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger(this.getClass());
	org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(this.getClass());
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void calleeWithJavaLog() throws IOException {
		try {
			throw new IOException("JavaLog throws IOException in third order class : ");
		} catch(IOException e) {
			e.printStackTrace();
			// OverLogging
			javaLogger.log(Level.WARNING, e.getMessage() + OverLoggingTheThirdOrderClass.class.toString());
			throw e;
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void calleeWithLog4J() throws IOException {
		try {
			throw new IOException("Log4J throws IOException in third order class : ");
		} catch(IOException e) {
			e.printStackTrace();
			// OverLogging
			log4jLogger.error(e.getMessage() + OverLoggingTheThirdOrderClass.class.toString());
			throw e;
		}
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void calleeWithSelf4J() throws IOException {
		try {
			throw new IOException("Self4J throws IOException in third order class : ");
		} catch(IOException e) {
			e.printStackTrace();
			// OverLogging
			slf4jLogger.error(e.getMessage() + OverLoggingTheThirdOrderClass.class.toString());
			throw e;
		}
	}
}
