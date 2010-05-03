package ntut.csie.rleht.caller;


import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.callhierarchy.CallHierarchyUI;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ���e���C�ѥ��M�w���ǹ�H�O�����ӿ�X�bTreeViewer�����
 * �Ѧ�CallHierarchyContentProvider�ק���׾Ǫ����{��
 * @author Shiau
 */
public class CallersContentProvider implements ITreeContentProvider {
	private final static Object[] EMPTY_ARRAY = new Object[0];
	
	// private TreeViewer tv;
	private static Logger logger = LoggerFactory.getLogger(CallersContentProvider.class);

	
    private class MethodWrapperRunnable implements IRunnableWithProgress {
        private MethodWrapper fMethodWrapper;
        private MethodWrapper[] fCalls= null;

        MethodWrapperRunnable(MethodWrapper methodWrapper) {
            fMethodWrapper= methodWrapper;
        }

        public void run(IProgressMonitor pm) {
        	fCalls= fMethodWrapper.getCalls(pm);
        }
        
        MethodWrapper[] getCalls() {
            if (fCalls != null) {
                return fCalls;
            }
            return new MethodWrapper[0];
        }
    }

    /**
     * ��ɭ��������Y���I�ɡA�Ѧ���k�M�w�Q�������I������ܭ��Ǥl���I
     * parentElement�N�O�Q���������I��H�C��^���ƲմN�O����ܪ��l���I
     */
	public Object[] getChildren(Object parentElement) {

        if (parentElement instanceof CallersRoot) {
        	CallersRoot dummyRoot = (CallersRoot) parentElement;

            return new Object[] { dummyRoot.getRoot() };
        } else if (parentElement instanceof MethodWrapper) {
            MethodWrapper methodWrapper = ((MethodWrapper) parentElement);

            if (shouldStopTraversion(methodWrapper)) {
                return EMPTY_ARRAY;
            } else {
            	Object[] wrappers = fetchChildren(methodWrapper);
            	//TODO �ثe���M��������n�Ƨ�,���׾Ǫ����i�H�����ѱ��S���Y
				//wrappers = CallersUtils.sortCallers(wrappers);
            	return wrappers;
            }
        }

        return EMPTY_ARRAY;
	}
	
    protected Object[] fetchChildren(MethodWrapper methodWrapper) {
        IRunnableContext context= JavaPlugin.getActiveWorkbenchWindow();
        MethodWrapperRunnable runnable= new MethodWrapperRunnable(methodWrapper);
        try {
            context.run(true, true, runnable);
        } catch (InvocationTargetException e) {
//            ExceptionHandler.handle(e, CallHierarchyMessages.CallHierarchyContentProvider_searchError_title, CallHierarchyMessages.CallHierarchyContentProvider_searchError_message);  
            return EMPTY_ARRAY;
        } catch (InterruptedException e) {
        	return EMPTY_ARRAY;
        }

        return runnable.getCalls();
    }
	
    /**
     * �O�_������o
     * @param methodWrapper
     * @return
     */
    private boolean shouldStopTraversion(MethodWrapper methodWrapper) {
        return (methodWrapper.getLevel() > CallHierarchyUI.getDefault().getMaxCallDepth()) || methodWrapper.isRecursive();
    }

    /**
     * ���oelement�������I�C���֨ϥ�
     */
    public Object getParent(Object element) {    	
        if (element instanceof MethodWrapper) {
            return ((MethodWrapper) element).getParent();
        }

        return null;
    }

    /**
     * �P�_�Ѽ�element���I�O�_���l���I
     * ��^true���element���l���I�A�h��e���|��ܦ��u�ϡv���ϼ�
     */
	public boolean hasChildren(Object element) {	
		if (element == CallersRoot.EMPTY_ROOT) {
			return false;
		}

		// Only methods can have subelements, so there's no need to fool the
		// user into believing that there is more
		if (element instanceof MethodWrapper) {
			MethodWrapper methodWrapper= (MethodWrapper) element;
			if (methodWrapper.getMember().getElementType() != IJavaElement.METHOD) {
				return false;
			}
			if (shouldStopTraversion(methodWrapper)) {
				return false;
			}
			return true;
		} else if (element instanceof CallersRoot) {
			return true;
		}

		return false; // the "Update ..." placeholder has no children
	}


	/**
	 * �Ѧ���k�M�w�𪺡u�Ĥ@�šv���I��ܭ��ǹ�H�CinputElement�O��TreeViewer.setInput()��k
	 * ��J�����ӹ�H�CObject[]�O�@�ӼƲաA�Ʋդ��@�Ӥ����N�O�@�ӵ��I
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	/**
	 * ��Q�P����Ĳ�o
	 */
	public void dispose() {
	}

	/**
	 * �C��TreeViewer.setInputĲ�o
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// this.tv = (TreeViewer) viewer;
	}
}
