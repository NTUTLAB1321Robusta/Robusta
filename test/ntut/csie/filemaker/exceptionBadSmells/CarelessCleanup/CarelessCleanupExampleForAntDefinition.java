package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.JarURLConnection;
import java.net.URLConnection;
import java.nio.channels.Channel;
import java.util.jar.JarFile;

/**
 * Capture from Apache Ant project which is named FileUtils.java
 * CarelessClenanupAntExample 中會使用到這支 Class 有關關閉串流的 method
 */
public class CarelessCleanupExampleForAntDefinition {
	public static void close(Writer device) {
		if (null != device) {
			try {
				device.close();
			} catch (IOException e) {
			}
		}
	}

	public static void close(Reader device) {
		if (null != device) {
			try {
				device.close();
			} catch (IOException e) {
			}
		}
	}

	public static void close(OutputStream device) {
		if (null != device) {
			try {
				device.close();
			} catch (IOException e) {
			}
		}
	}

	public static void close(InputStream device) {
		if (null != device) {
			try {
				device.close();
			} catch (IOException e) {
			}
		}
	}

	public static void close(Channel device) {
		if (null != device) {
			try {
				device.close();
			} catch (IOException e) {
			}
		}
	}

	public static void close(URLConnection conn) {
		if (conn != null) {
			try {
				if (conn instanceof JarURLConnection) {
					JarURLConnection juc = (JarURLConnection) conn;
					JarFile jf = juc.getJarFile();
					jf.close();
					jf = null;
				} else if (conn instanceof HttpURLConnection) {
					((HttpURLConnection) conn).disconnect();
				}
			} catch (IOException exc) {
			}
		}
	}
}
