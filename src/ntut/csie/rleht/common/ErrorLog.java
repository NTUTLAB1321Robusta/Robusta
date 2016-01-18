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

	public void logError(String message, Throwable exception) {
		logger.log(new Status(Status.ERROR, RLEHTPlugin.PLUGIN_ID, Status.OK, message, exception));
	}

	public void logWarning(String message, Throwable exception) {
		logger.log(new Status(Status.WARNING, RLEHTPlugin.PLUGIN_ID, Status.OK, message, exception));
	}
}
