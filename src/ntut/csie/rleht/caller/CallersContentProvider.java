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
 * 內容器。由它決定哪些對象記錄應該輸出在TreeViewer裡顯示
 * 參考CallHierarchyContentProvider修改裕豐學長的程式
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
     * 當界面中單擊某結點時，由此方法決定被單擊結點應該顯示哪些子結點
     * parentElement就是被單擊的結點對象。返回的數組就是應顯示的子結點
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
            	//TODO 目前不清楚為什麼要排序,裕豐學長說可以先註解掉沒關係
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
     * 是否停止取得
     * @param methodWrapper
     * @return
     */
    private boolean shouldStopTraversion(MethodWrapper methodWrapper) {
        return (methodWrapper.getLevel() > CallHierarchyUI.getDefault().getMaxCallDepth()) || methodWrapper.isRecursive();
    }

    /**
     * 取得element的父結點。極少使用
     */
    public Object getParent(Object element) {    	
        if (element instanceof MethodWrapper) {
            return ((MethodWrapper) element).getParent();
        }

        return null;
    }

    /**
     * 判斷參數element結點是否有子結點
     * 返回true表示element有子結點，則其前面會顯示有「＋」號圖標
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
	 * 由此方法決定樹的「第一級」結點顯示哪些對象。inputElement是用TreeViewer.setInput()方法
	 * 輸入的那個對象。Object[]是一個數組，數組中一個元素就是一個結點
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	/**
	 * 樹被銷毀時觸發
	 */
	public void dispose() {
	}

	/**
	 * 每次TreeViewer.setInput觸發
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		// this.tv = (TreeViewer) viewer;
	}
}
