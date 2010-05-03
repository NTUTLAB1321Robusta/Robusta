package ntut.csie.rleht.caller;

import org.eclipse.jdt.internal.ui.callhierarchy.TreeRoot;

@SuppressWarnings("restriction")
public class CallersRoot {
	public static final Object EMPTY_ROOT = new Object();

	public static final TreeRoot EMPTY_TREE = new TreeRoot(EMPTY_ROOT);

	private Object root;

	public CallersRoot(Object root) {
		this.root = root;
	}

	public Object getRoot() {
		return root;
	}
}
