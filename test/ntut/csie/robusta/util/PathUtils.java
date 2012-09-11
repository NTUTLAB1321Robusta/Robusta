package ntut.csie.robusta.util;

import ntut.csie.filemaker.JavaProjectMaker;

public class PathUtils {
	/**
	 * packageName�̭���.�����׽u
	 * @param packageName
	 * @return
	 */
	public static String dot2slash(String packageName) {
		String result = "";
		result = packageName.replace(java.util.regex.Matcher.quoteReplacement("."), "/");
		return result;
	}
	
	/**
	 * ���o�ǤJclass��������|
	 * @param clazz �n���o��m��.class��
	 * @param projectName ��.class�ɩҦb���M�צW��
	 * @return ��.class�ɪ���ڸ��|
	 * @author pig
	 */
	public static String getPathOfClassUnderSrcFolder(Class<?> clazz, String projectName) {
		String result = "";
		result = projectName + "/" + JavaProjectMaker.FOLDERNAME_SOURCE + "/"
			+ dot2slash(clazz.getName()) + JavaProjectMaker.JAVA_FILE_EXTENSION;
		return result;
	}
}
