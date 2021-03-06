package ntut.csie.csdet.smell.ui;

import ntut.csie.rleht.common.ConsoleLog;
import ntut.csie.rleht.views.RLMethodView;

import org.eclipse.core.filebuffers.IFileBuffer;
import org.eclipse.core.filebuffers.IFileBufferListener;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EHSmellViewEventHandler implements IDocumentListener, IFileBufferListener, ISelectionListener,
ISelectionChangedListener, IPartListener2, IDoubleClickListener {
	private static Logger logger =LoggerFactory.getLogger(EHSmellViewEventHandler.class);
	private boolean visible = true;

	private EHSmellView view;

	public EHSmellViewEventHandler(EHSmellView view) {
		this.view = view;
	}
	
	// **************************************************************************
	// * IFileBufferListener
	// **************************************************************************
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart,
	*      org.eclipse.jface.viewers.ISelection)
	*/
	public void selectionChanged(IWorkbenchPart part, ISelection selection) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>org.eclipse.ui.ISelectionListener#selectionChanged");
		if (visible) {
			view.handleSelectionChanged4Editor(part, selection);
		}
	}
	
	// **************************************************************************
	// * IDocumentListener
	// **************************************************************************
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
	*/
	public void documentAboutToBeChanged(DocumentEvent event) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>documentAboutToBeChanged");
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.jface.text.IDocumentListener#documentChanged(org.eclipse.jface.text.DocumentEvent)
	*/
	public void documentChanged(DocumentEvent event) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>documentChanged");
		view.handleDocumentChanged(event.getDocument());
	}
	
	// **************************************************************************
	// * ISelectionChangedListener
	// **************************************************************************
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.jface.viewers.ISelectionChangedListener#selectionChanged(org.eclipse.jface.viewers.SelectionChangedEvent)
	*/
	public void selectionChanged(SelectionChangedEvent event) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>org.eclipse.jface.viewers.SelectionChangedEvent");
		view.handleSelectionChanged4View(event.getSelection());
	}
	
	// **************************************************************************
	// * IDoubleClickListener
	// **************************************************************************
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
	*/
	public void doubleClick(DoubleClickEvent event) {}
	
	// **************************************************************************
	// * IPartListener2
	// **************************************************************************
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui.IWorkbenchPartReference)
	*/
	public void partHidden(IWorkbenchPartReference partRef) {
		if (partRef.getPart(false) == view) {
			visible = false;
		}
		logger.debug("========>>>>>partHidden : " + visible);
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
	*/
	public void partVisible(IWorkbenchPartReference partRef) {
		if (partRef.getPart(false) == view) {
			visible = true;
		}		
		logger.debug("========>>>>>partVisible : " + visible);
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui.IWorkbenchPartReference)
	*/
	public void partOpened(IWorkbenchPartReference partRef) {}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
	*/
	public void partClosed(IWorkbenchPartReference partRef) {
		view.notifyWorkbenchPartClosed(partRef);
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui.IWorkbenchPartReference)
	*/
	public void partDeactivated(IWorkbenchPartReference partRef) {}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
	*/
	public void partActivated(IWorkbenchPartReference partRef) {}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui.IWorkbenchPartReference)
	*/
	public void partInputChanged(IWorkbenchPartReference partRef) {}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui.IWorkbenchPartReference)
	*/
	public void partBroughtToTop(IWorkbenchPartReference partRef) {}
	
	// **************************************************************************
	// * IFileBufferListener
	// **************************************************************************
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.core.filebuffers.IFileBufferListener#bufferCreated(org.eclipse.core.filebuffers.IFileBuffer)
	*/
	public void bufferCreated(IFileBuffer buffer) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>bufferCreated");
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.core.filebuffers.IFileBufferListener#bufferDisposed(org.eclipse.core.filebuffers.IFileBuffer)
	*/
	public void bufferDisposed(IFileBuffer buffer) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>bufferDisposed");
		if (buffer instanceof ITextFileBuffer) {
			view.handleDocumentDisposed(((ITextFileBuffer) buffer).getDocument());
		}
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.core.filebuffers.IFileBufferListener#bufferContentAboutToBeReplaced(org.eclipse.core.filebuffers.IFileBuffer)
	*/
	public void bufferContentAboutToBeReplaced(IFileBuffer buffer) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>bufferContentAboutToBeReplaced");
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.core.filebuffers.IFileBufferListener#bufferContentReplaced(org.eclipse.core.filebuffers.IFileBuffer)
	*/
	public void bufferContentReplaced(IFileBuffer buffer) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>bufferContentReplaced");
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.core.filebuffers.IFileBufferListener#stateChanging(org.eclipse.core.filebuffers.IFileBuffer)
	*/
	public void stateChanging(IFileBuffer buffer) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>stateChanging");
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.core.filebuffers.IFileBufferListener#dirtyStateChanged(org.eclipse.core.filebuffers.IFileBuffer,
	*      boolean)
	*/
	public void dirtyStateChanged(IFileBuffer buffer, boolean isDirty) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>dirtyStateChanged");
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.core.filebuffers.IFileBufferListener#stateValidationChanged(org.eclipse.core.filebuffers.IFileBuffer,
	*      boolean)
	*/
	public void stateValidationChanged(IFileBuffer buffer, boolean isStateValidated) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>stateValidationChanged");
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.core.filebuffers.IFileBufferListener#underlyingFileMoved(org.eclipse.core.filebuffers.IFileBuffer,
	*      org.eclipse.core.runtime.IPath)
	*/
	public void underlyingFileMoved(IFileBuffer buffer, IPath path) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>underlyingFileMoved");
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.core.filebuffers.IFileBufferListener#underlyingFileDeleted(org.eclipse.core.filebuffers.IFileBuffer)
	*/
	public void underlyingFileDeleted(IFileBuffer buffer) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>underlyingFileDeleted");
	}
	
	/*
	* (non-Javadoc)
	* 
	* @see org.eclipse.core.filebuffers.IFileBufferListener#stateChangeFailed(org.eclipse.core.filebuffers.IFileBuffer)
	*/
	public void stateChangeFailed(IFileBuffer buffer) {
		ConsoleLog.debug(EHSmellViewEventHandler.class + "==============>>>stateChangeFailed");
		// not interesting
	}
}
