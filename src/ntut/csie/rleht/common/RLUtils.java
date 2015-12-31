package ntut.csie.rleht.common;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;

public class RLUtils {

	/**
	 * string cast to integer
	 * 
	 * @param str
	 *            string
	 * @param def
	 * 			  default return integer value(if casting is fail then return this value)
	 * @return integer
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
