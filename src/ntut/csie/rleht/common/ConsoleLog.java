package ntut.csie.rleht.common;

import ntut.csie.rleht.RLEHTPlugin;
import ntut.csie.rleht.caller.CallersLabelProvider;

import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleLog {
	private static Logger logger = LoggerFactory.getLogger(CallersLabelProvider.class);
	private static final String TITLE = "[" + RLEHTPlugin.PLUGIN_ID + "]";

	public static final int DEBUG = 0;

	public static final int INFO = 10;

	public static final int WARN = 20;

	public static final int ERROR = 30;

	private static MessageConsoleStream consoleStream = null;

	// private static PrintStream consoleStream=null;

	private static MessageConsole console = null;

	private static int logLevel = DEBUG;

	private static final boolean useConsoleLog = false;

	static {
		init();
	}

	protected ConsoleLog() {

	}

	public static void init() {
//		 console = new MessageConsole("RLEHT Console", null);
//		 ConsolePlugin.getDefault().getConsoleManager().addConsoles(new
//		 IConsole[] { console });
//		 consoleStream = console.newMessageStream();
//		 ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console);

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

	public static void warn(String msg) {
		if (logLevel >= WARN) {
			if (useConsoleLog) {
				consoleStream.println(TITLE + msg);
			}
			logger.debug(TITLE + msg);
		}
	}

	public static void warn(String msg, Throwable ex) {
		if (logLevel >= WARN) {
			if (useConsoleLog) {
				consoleStream.println(TITLE + msg + "\n" + getExceptionTrace(ex));
			}
			logger.debug(TITLE + msg, ex);
		}
	}

	public static void error(String msg) {
		if (logLevel >= ERROR) {
			if (useConsoleLog) {
				consoleStream.println(TITLE + msg);
			}
			logger.debug(TITLE + msg);
		}
	}

	public static void error(String msg, Throwable ex) {
		if (logLevel >= ERROR) {
			if (useConsoleLog) {
				consoleStream.println(TITLE + msg + "\n" + getExceptionTrace(ex));
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

	public static int getLogLevel() {
		return logLevel;
	}

	public static void setLogLevel(int logLevel) {
		ConsoleLog.logLevel = logLevel;
	}

}
