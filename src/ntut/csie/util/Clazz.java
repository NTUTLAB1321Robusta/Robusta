package ntut.csie.util;

public class Clazz {
	/**
	 * check whether the specified class is the practice of specified interface
	 * 
	 * @param clazz
	 * @param looking4interface
	 * @return
	 */
	public static boolean isImplemented(Class<?> clazz,
			Class<?> looking4interface) {
		// if passed in class is an object, it would not be a practice of
		// interface
		if ((clazz == null) || clazz.equals(Object.class)) {
			return false;
		}

		Class<?>[] interfaces = clazz.getInterfaces();
		if (interfaces != null) {
			for (int i = 0; i < interfaces.length; i++) {
				if (interfaces[i].equals(looking4interface)) {
					return true;

				}
			}
		}
		return isImplemented(clazz.getSuperclass(), looking4interface);
	}

	public static boolean isUncheckedException(String exceptionName) {
		try {
			Class<?> exception = Class.forName(exceptionName);
			if (exception.equals(RuntimeException.class)) {
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
