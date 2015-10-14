package ntut.csie.util;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import ntut.csie.rleht.RLEHTPlugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RLAnnotationFileUtil {
	private static Logger logger = LoggerFactory.getLogger(RLAnnotationFileUtil.class);
	// TODO globalize the pluginId and agileExceptionJarId
	private final static String pluginId = RLEHTPlugin.PLUGIN_ID;
	private final static String RLAnnotationJarId = "ntut.csie.robusta.agile.exception";

	static public boolean doesRLAnnotationExistInClassPath(IJavaProject javaProject)
			throws JavaModelException {
		IClasspathEntry[] ICPEntry = javaProject.getRawClasspath();
		for (IClasspathEntry entry : ICPEntry) {
			if (entry.toString().contains(RLAnnotationJarId)) {
				return true;
			}
		}
		return false;
	}

	static public JarFile getRobustaJar(Path eclipsePath) {
		JarFile RobustaJar = null;
		File pluginsDir = new File(eclipsePath.toString() + "/plugins");
		File[] files = pluginsDir.listFiles();

		for (File file : files) {
			if (file.getName().contains(pluginId)) {
				try {
					RobustaJar = new JarFile(eclipsePath.toString()
							+ "/plugins/" + file.getName());
					return RobustaJar;
				} catch (IOException e) {
					throw new RuntimeException(
							"Robusta plugin jar found, but fail to get path", e);
				}
			}
		}
		return null;
	}

	static public void copyFileUsingFileStreams(InputStream source, File dest)
			throws IOException {
		dest.getParentFile().mkdirs();
		OutputStream output = null;
		try {
			output = new FileOutputStream(dest);
			byte[] buf = new byte[1024];
			int bytesRead;
			while ((bytesRead = source.read(buf)) > 0) {
				output.write(buf, 0, bytesRead);
			}
		} finally {
			closeStream(output);
		}
	}
	
	static public String extractRLAnnotationJarId(String fullJarId) {
		String[] parts = fullJarId.split("/");
		for (String s : parts) {
			if (s.contains(RLAnnotationJarId)) {
				return s;
			}
		}
		return fullJarId;
	}
	
	static public File getProjectLibFolder(IProject project) {
		return new File(project.getLocation() + "/lib");
	}
	
	private static void closeStream(Closeable io) {
		try {
			if (io != null)
				io.close();
		} catch (IOException e) {
			logger.error("[IOException] EXCEPTION ", e);
		}
	}

	public static String getRLAnnotationJarId() {
//		URL installURL = Platform.getInstallLocation().getURL();
//		Path eclipsePath = new Path(installURL.getPath());
//		JarFile RobustaJar = RLAnnotationFileUtil.getRobustaJar(eclipsePath);
//		
//		if (RobustaJar != null) {
//			final Enumeration<JarEntry> entries = RobustaJar.entries();
//			while (entries.hasMoreElements()) {
//				final JarEntry entry = entries.nextElement();
//				String jarPath = entry.getName();
//				if (jarPath.contains(RLAnnotationJarId)) {
//					return extractRLAnnotationJarId(jarPath);
//				}
//			}
//		}
//		
//		return null;
		return RLAnnotationJarId + "_1.0.0.jar";
	}
	
	static public JarEntry getRLAnnotationJarEntry(IProject project)  {
		URL installURL = Platform.getInstallLocation().getURL();
		Path eclipsePath = new Path(installURL.getPath());
		JarFile RobustaJar = RLAnnotationFileUtil.getRobustaJar(eclipsePath);
		
		if (RobustaJar != null) {
			final Enumeration<JarEntry> entries = RobustaJar.entries();
			while (entries.hasMoreElements()) {
				final JarEntry entry = entries.nextElement();
				String jarPath = entry.getName();
				if (jarPath.contains(RLAnnotationJarId)) {
					return entry;
				}
			}
		}
		
		return null;
	}
	
	public static boolean isRLAnnotationJarInProjLibFolder(IProject project) {
		File projLib = getProjectLibFolder(project);
		String RLAnnotationJarId = getRLAnnotationJarId();
		File fileDest = new File(projLib.toString() + "/" + RLAnnotationJarId);
		
		return fileDest.exists();
	}
}
