package ntut.csie.rleht.caller;

import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.internal.ui.callhierarchy.TreeRoot;

@SuppressWarnings("restriction")
public class CallersRoot {
	public static final Object EMPTY_ROOT = new Object();

	public static final MethodWrapper[] EMPTYP_ROOTS = new MethodWrapper[] { null };

	public static final TreeRoot EMPTY_TREE = new TreeRoot(EMPTYP_ROOTS);

	private Object root;

	public CallersRoot(Object root) {
		this.root = root;
	}

	public Object getRoot() {
		return root;
	}
}
