package ntut.csie.jdt.util;

public class Clazz {
	/**
	 * 判斷指定的class是否為特定interface的實作。
	 * @param clazz
	 * @param looking4interface
	 * @return
	 */
	public static boolean isImplemented(Class<?> clazz, Class<?> looking4interface) {
		// 如果傳進來的是Object類別，那就不會有interface了
		if( (clazz == null) || clazz.equals(Object.class)) {
			return false;
		}
		
		Class<?>[] interfaces = clazz.getInterfaces();
		if(interfaces != null) {
			for(int i = 0; i<interfaces.length; i++) {
				if(interfaces[i].equals(looking4interface)) {
					return true;
				}
			}
		}
		return isImplemented(clazz.getSuperclass(), looking4interface);
	}
}
