package ntut.csie.robusta.util;

import ntut.csie.filemaker.JavaProjectMaker;

public class PathUtils {
	/**
	 * packageName裡面的.換成斜線
	 * @param packageName
	 * @return
	 */
	public static String dot2slash(String packageName) {
		String result = "";
		result = packageName.replace(java.util.regex.Matcher.quoteReplacement("."), "/");
		return result;
	}
	
	/**
	 * 取得傳入class的完整路徑
	 * @param clazz 要取得位置的.class檔
	 * @param projectName 該.class檔所在的專案名稱
	 * @return 該.class檔的實際路徑
	 * @author pig
	 */
	public static String getPathOfClassUnderSrcFolder(Class<?> clazz, String projectName) {
		String result = "";
		result = projectName + "/" + JavaProjectMaker.FOLDERNAME_SOURCE + "/"
			+ dot2slash(clazz.getName()) + JavaProjectMaker.JAVA_FILE_EXTENSION;
		return result;
	}
}
