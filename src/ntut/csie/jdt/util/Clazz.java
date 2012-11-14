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
	
	/**
	 * 從類別名稱去尋找類別是否存在
	 * @param className
	 * @return
	 */
	public static boolean isClassExisted(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	/**
	 * 判斷是不是非受檢例外
	 * @param exceptionName 包含package name的類別名稱
	 * @return
	 */
	public static boolean isUncheckedException(String exceptionName) {
		try {
			Class<?> exception = Class.forName(exceptionName);
			if(exception.equals(RuntimeException.class)) {			
				return true;
			}
			exception.asSubclass(RuntimeException.class);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		} catch (ClassCastException e) {
			return false;
		}
	}
}
