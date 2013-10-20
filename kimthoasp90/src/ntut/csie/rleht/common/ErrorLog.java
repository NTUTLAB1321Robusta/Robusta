package ntut.csie.rleht.common;

import ntut.csie.rleht.RLEHTPlugin;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Status;

public class ErrorLog {

	private static ErrorLog instance = null;

	private ILog logger = null;

	protected ErrorLog() {
		logger = RLEHTPlugin.getDefault().getLog();
	}

	public static ErrorLog getInstance() {
		if (instance == null) {
			instance = new ErrorLog();
		}

		return instance;
	}

	public void log(int severity, String message, Throwable exception) {
		logger.log(new Status(severity, RLEHTPlugin.PLUGIN_ID, Status.OK, message, exception));
	}

	public void logCancel(String message, Throwable exception) {
		logger.log(new Status(Status.CANCEL, RLEHTPlugin.PLUGIN_ID, Status.OK, message, exception));
	}

	public void logError(String message, Throwable exception) {
		logger.log(new Status(Status.ERROR, RLEHTPlugin.PLUGIN_ID, Status.OK, message, exception));
	}

	public void logInfo(String message, Throwable exception) {
		logger.log(new Status(Status.INFO, RLEHTPlugin.PLUGIN_ID, Status.OK, message, exception));
	}

	public void logOk(String message, Throwable exception) {
		logger.log(new Status(Status.OK, RLEHTPlugin.PLUGIN_ID, Status.OK, message, exception));
	}

	public void logWarning(String message, Throwable exception) {
		logger.log(new Status(Status.WARNING, RLEHTPlugin.PLUGIN_ID, Status.OK, message, exception));
	}
}
