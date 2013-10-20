package ntut.csie.filemaker.exceptionBadSmells.OverLogging;

import java.util.logging.Logger;

import org.slf4j.LoggerFactory;

public class OverLoggingIntegrationExample {
	Logger javaLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
	org.apache.log4j.Logger log4jLogger = org.apache.log4j.Logger.getLogger(this.getClass());
	org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger(this.getClass());
}
