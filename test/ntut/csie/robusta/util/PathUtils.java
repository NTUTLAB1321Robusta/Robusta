package ntut.csie.robusta.util;

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
}
