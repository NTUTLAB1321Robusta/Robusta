package ntut.csie.util;

import ntut.csie.filemaker.JavaProjectMaker;

public class PathUtils {
	/**
	 * change "." to "/" in packageName
	 * @param packageName
	 * @return
	 */
	public static String dot2slash(String packageName) {
		String result = "";
		result = packageName.replace(java.util.regex.Matcher.quoteReplacement("."), "/");
		return result;
	}
	
	/**
	 * get full path of parameter class
	 * @param clazz 
	 * 			class file
	 * @param projectName 
	 * 			project name of class file 
	 * @return 
	 * 			full path of class file
	 * @author pig
	 */
	public static String getPathOfClassUnderSrcFolder(Class<?> clazz, String projectName) {
		String result = "";
		result = projectName + "/" + JavaProjectMaker.FOLDERNAME_SOURCE + "/"
			+ dot2slash(clazz.getName()) + JavaProjectMaker.JAVA_FILE_EXTENSION;
		return result;
	}
}
