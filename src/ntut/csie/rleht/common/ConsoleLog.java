package ntut.csie.rleht.common;

import ntut.csie.rleht.RLEHTPlugin;

import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleLog {
	private static Logger logger = LoggerFactory.getLogger(ConsoleLog.class);
	private static final String TITLE = "[" + RLEHTPlugin.PLUGIN_ID + "]";

	public static final int DEBUG = 0;

	public static final int INFO = 10;

	public static final int ERROR = 30;

	private static MessageConsoleStream consoleStream = null;

	private static MessageConsole console = null;

	private static int logLevel = DEBUG;

	private static final boolean useConsoleLog = false;

	protected ConsoleLog() {

	}

	public static void debug(String msg) {
		if (logLevel >= DEBUG) {
			if (useConsoleLog) {
				consoleStream.println(TITLE + msg);
			}
			logger.debug(TITLE + msg);
		}
	}

	public static void info(String msg) {
		if (logLevel >= INFO) {
			if (useConsoleLog) {
				consoleStream.println(TITLE + msg);
			}
			logger.debug(TITLE + msg);
		}
	}

	public static void error(String msg, Throwable ex) {
		if (logLevel >= ERROR) {
			if (useConsoleLog) {
				consoleStream.println(TITLE + msg + "\n"
						+ getExceptionTrace(ex));
			}
			logger.debug(TITLE + msg, ex);
		}
	}

	private static String getExceptionTrace(Throwable ex) {
		StackTraceElement[] traces = ex.getStackTrace();
		StringBuffer sb = new StringBuffer();
		for (int i = 0, size = traces.length; i < size; i++) {
			sb.append(traces[i].toString()).append("\n");
		}
		return sb.toString();
	}
}
