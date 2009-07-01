package ntut.csie.rleht.caller;


import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CallersContentProvider implements ITreeContentProvider {
	// private TreeViewer tv;
	private static Logger logger = LoggerFactory.getLogger(CallersContentProvider.class);

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof CallersRoot) {
			return new Object[] { ((CallersRoot) parentElement).getRoot() };
		} else if (parentElement instanceof MethodWrapper) {
			MethodWrapper[] wrappers = null;
			if (parentElement != null) {
				MethodWrapper wrapper = (MethodWrapper) parentElement;
				wrappers = wrapper.getCalls(new NullProgressMonitor());
				//TODO 目前不清楚為什麼要排序,裕豐學長說可以先註解掉沒關係
				//wrappers = CallersUtils.sortCallers(wrappers);
//				for(int i=0;i<wrappers.length;i++){
//					System.out.println("*****Wrappers******"+wrappers[i].getName());
//				}
			}
			return wrappers;
		}

		return null;
	}

	public Object getParent(Object element) {
		if (element instanceof MethodWrapper) {
			return ((MethodWrapper) element).getParent();
		}
		return null;
	}

	public boolean hasChildren(Object element) {
		Object[] obj = getChildren(element);
		return obj == null ? false : obj.length > 0;

	}

	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	public void dispose() {
	}

	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// this.tv = (TreeViewer) viewer;
	}

}
