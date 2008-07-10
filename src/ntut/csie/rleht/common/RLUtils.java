package ntut.csie.rleht.common;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

public class RLUtils {

	/**
	 * 字串轉數字
	 * 
	 * @param str
	 *            字串
	 * @param def
	 *            預設值(當轉換不成功時，會回傳預設值)
	 * @return 數字
	 */
	public static int str2int(String str, int def) {
		if (str == null) {
			return def;
		}
		try {
			return Integer.parseInt(str.trim());
		}
		catch (NumberFormatException e) {
			return def;
		}
	}

	public static IEditorPart openInEditor(Object element, boolean activate) throws JavaModelException,
			PartInitException {
		if (element instanceof IJavaElement) {
			return JavaUI.openInEditor((IJavaElement) element);
		}
		else {
			return null;
		}
	}
}
