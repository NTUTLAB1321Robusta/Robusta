package ntut.csie.jdt.util;

public class Clazz {
	/**
	 * �P�_���w��class�O�_���S�winterface����@�C
	 * @param clazz
	 * @param looking4interface
	 * @return
	 */
	public static boolean isImplemented(Class<?> clazz, Class<?> looking4interface) {
		// �p�G�Ƕi�Ӫ��OObject���O�A���N���|��interface�F
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
	 * �q���O�W�٥h�M�����O�O�_�s�b
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
	 * �P�_�O���O�D���˨ҥ~
	 * @param exceptionName �]�tpackage name�����O�W��
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
