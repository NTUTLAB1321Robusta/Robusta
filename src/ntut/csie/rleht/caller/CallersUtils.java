package ntut.csie.rleht.caller;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("restriction")
public class CallersUtils {
	private static Logger logger =LoggerFactory.getLogger(CallersLabelProvider.class);
	
	public static MethodWrapper[] sortCallers(MethodWrapper[] wrappers) {
		
		if (wrappers != null) {
			Map<String, MethodWrapper> sortmap = new TreeMap<String, MethodWrapper>();
			for (int i = 0, size = wrappers.length; i < size; i++) {
				if (wrappers[i] != null) {
					if (wrappers[i].getMember() instanceof IMethod) {
						IMethod smethod = (IMethod) wrappers[i].getMember();
						IType type = smethod.getDeclaringType();

						String key = type.getFullyQualifiedName() + ":" + smethod.getElementName();
						sortmap.put(key, wrappers[i]);
					}
					else {
						logger.debug("[sortCallers]"+wrappers[i].getMember().getElementType());
					}
				}
			}
			wrappers = sortmap.values().toArray(new MethodWrapper[sortmap.size()]);
		}
		return wrappers;
	}

}
