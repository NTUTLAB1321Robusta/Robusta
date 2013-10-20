package ntut.csie.rleht.caller;

import ntut.csie.rleht.RLEHTPlugin;
import ntut.csie.rleht.preferences.PreferenceConstants;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class CallersViewerFilter extends ViewerFilter {
	private static Logger logger = LoggerFactory.getLogger(CallersView.class);

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		logger.debug(">>>>>>====>" + element);

		if (element instanceof CallersRoot) {
			return true;
		}
		MethodWrapper wrapper = (MethodWrapper) element;
		try {
			String text = null;
			if (wrapper.getMember() instanceof IMethod) {
				IMethod method = (IMethod) wrapper.getMember();
				IType type = method.getDeclaringType();

				text = type.getFullyQualifiedName();
			} else if (wrapper.getMember() instanceof IType) {
				text = ((IType) wrapper.getMember()).getFullyQualifiedName();
			}

			if (text != null) {
				String filter = RLEHTPlugin.getDefault().getPreferenceStore().getString(PreferenceConstants.P_RLCHPF_STRING);
				String[] filters = StringUtils.split(filter, ',');

				logger.debug(">>>>>>====>" + text);

				boolean inFilterStr = false;
				for (int i = 0, size = filters.length; i < size; i++) {
					if (filters[i] != null && !"".equals(filters[i].trim())) {
						inFilterStr = text.startsWith(filters[i].trim());
						if (inFilterStr) {
							break;
						}
					}
				}

				return !inFilterStr;
			}
		} catch (Exception ex) {
			logger.error("", ex);
		}

		return true;
	}

}
