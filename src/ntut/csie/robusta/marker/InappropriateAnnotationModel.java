/*******************************************************************************
 * Copyright (c) 2006, 2014 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *    
 ******************************************************************************/
package ntut.csie.robusta.marker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InappropriateAnnotationModel implements IAnnotationModel {
	private static Logger logger = LoggerFactory.getLogger(InappropriateAnnotationModel.class);
	
	/** Key used to piggyback our model to the editor's model. */
	private static final Object KEY = new Object();

	/** List of current InappropriateAnnotation objects */
	private List<InappropriateAnnotation> annotations = new ArrayList<InappropriateAnnotation>(
			32);

	/** List of registered IAnnotationModelListener */
	private List<IAnnotationModelListener> annotationModelListeners = new ArrayList<IAnnotationModelListener>(
			2);

	private final ITextEditor editor;
	private final IDocument document;
	private int openConnections = 0;
	private boolean annotated = false;
	private MarkerModel markerModel;

	private IDocumentListener documentListener = new IDocumentListener() {
		public void documentChanged(DocumentEvent event) {
			updateAnnotations(true);
		}

		public void documentAboutToBeChanged(DocumentEvent event) {
		}
	};

	private InappropriateAnnotationModel(ITextEditor editor, IDocument document, MarkerModel markerModel) {
		this.editor = editor;
		this.document = document;
		this.markerModel = markerModel;
		updateAnnotations(true);
	}

	/**
	 * Attaches a inappropriate annotation model for the given editor if the
	 * editor can be annotated. Does nothing if the model is already attached.
	 * 
	 * @param editor
	 *            Editor to attach a annotation model to
	 */
	public static void attach(ITextEditor editor, MarkerModel markerModel) {
		IDocumentProvider provider = editor.getDocumentProvider();
		// there may be text editors without document providers (SF #1725100)
		if (provider == null)
			return;
		IAnnotationModel model = provider.getAnnotationModel(editor
				.getEditorInput());
		if (!(model instanceof IAnnotationModelExtension))
			return;
		IAnnotationModelExtension modelex = (IAnnotationModelExtension) model;

		IDocument document = provider.getDocument(editor.getEditorInput());

		InappropriateAnnotationModel iaModel = (InappropriateAnnotationModel) modelex.getAnnotationModel(KEY);
		
		if (iaModel == null) {
			iaModel = new InappropriateAnnotationModel(editor, document, markerModel);
			modelex.addAnnotationModel(KEY, iaModel);
		} 
		/* if we want to automatically remove annotation when the user remove markers, we have to update here;
		 * however, it would cause List concurrent modification exception
		 * which is a buy in eclipse's AnnotationModel library
		 * Eclipse Bug ID: 410052
		 */
//		else {
//			iaModel.updateAnnotations(true);
//		}
	}

	/**
	 * Detaches the inappropriate annotation model from the given editor. If the
	 * editor does not have a model attached, this method does nothing.
	 * 
	 * @param editor
	 *            Editor to detach the annotation model from
	 */
	public static void detach(ITextEditor editor) {
		IDocumentProvider provider = editor.getDocumentProvider();
		// there may be text editors without document providers (SF #1725100)
		if (provider == null)
			return;
		IAnnotationModel model = provider.getAnnotationModel(editor
				.getEditorInput());
		if (!(model instanceof IAnnotationModelExtension))
			return;
		IAnnotationModelExtension modelex = (IAnnotationModelExtension) model;
		modelex.removeAnnotationModel(KEY);
	}

	private void updateAnnotations(boolean force) {
		updateMarkerForEditor();
		final List<AnnotationInfo> annotationPosList = findAnnotationForEditor();
		if (annotationPosList != null) {
			if (!annotated || force) {
				createAnnotations(annotationPosList);
				annotated = true;
			}
		} else {
			if (annotated) {
				clear();
				annotated = false;
			}
		}
	}

	private void updateMarkerForEditor() {
		if(editor.getEditorInput() == null)
			return;
		final IEditorInput input = editor.getEditorInput();
		if (input == null) {
			return;
		}
		final Object element = input.getAdapter(IJavaElement.class);
		if (!hasSource((IJavaElement) element)) {
			return;
		}
		if (element instanceof ISourceReference) {
			try {
				markerModel.updateMarker((IFile)((IJavaElement) element).getCorrespondingResource());
			} catch (JavaModelException e) {
				e.printStackTrace();
			}
		}
	}

	private List<AnnotationInfo> findAnnotationForEditor() {
		// if (editor.isDirty()) {
		// return null;
		// }
		final IEditorInput input = editor.getEditorInput();
		if (input == null) {
			return null;
		}
		final Object element = input.getAdapter(IJavaElement.class);
		if (!hasSource((IJavaElement) element)) {
			return null;
		}
		return findAnnotationForElement(element);
	}

	private boolean hasSource(IJavaElement element) {
		if (element instanceof ISourceReference) {
			try {
				return ((ISourceReference) element).getSourceRange() != null;
			} catch (JavaModelException ex) {
				// we ignore this, the resource seems to have problems
			}
		}
		return false;
	}

	private List<AnnotationInfo> findAnnotationForElement(Object element) {
		try {
			return MarkerData.getAnnotationInfo(((IJavaElement) element).getCorrespondingResource());
		} catch (JavaModelException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void clear() {
		AnnotationModelEvent event = new AnnotationModelEvent(this);
		clear(event);
		fireModelChanged(event);
	}

	private void clear(AnnotationModelEvent event) {
		for (final InappropriateAnnotation ia : annotations) {
			event.annotationRemoved(ia, ia.getPosition());
		}
		annotations.clear();
	}

	private void createAnnotations(final List<AnnotationInfo> annotationPosList) {
		AnnotationModelEvent event = new AnnotationModelEvent(this);
		clear(event);

		try {
			for (AnnotationInfo ai : annotationPosList) {
				int startPos = ai.getStartLine();
				for (int remainingLength = ai.getLength(); remainingLength > 0;) {
					final IRegion region = document.getLineInformation(startPos - 1);
					final InappropriateAnnotation ia = new InappropriateAnnotation(
							Math.max(region.getOffset(), ai.getStartOffSet()), 
							Math.min(region.getLength(), remainingLength), 
							ai.getDescription());				
					
					annotations.add(ia);
					event.annotationAdded(ia);
					
					int currentLineLength = region.getLength() + 2; // include the "\n" char
					if(startPos == ai.getStartLine()) // the first line colored
						remainingLength -= (currentLineLength - (ai.getStartOffSet() - region.getOffset())); // exclude the chars in front of the CATCH key word
					else
						remainingLength -= currentLineLength;
					
					startPos++;
				}
			}
		} catch (BadLocationException ex) {
			throw new RuntimeException(ex);
		}
		fireModelChanged(event);
	}

	public void addAnnotationModelListener(IAnnotationModelListener listener) {
		if (!annotationModelListeners.contains(listener)) {
			annotationModelListeners.add(listener);
			fireModelChanged(new AnnotationModelEvent(this, true));
		}
	}

	public void removeAnnotationModelListener(IAnnotationModelListener listener) {
		annotationModelListeners.remove(listener);
	}

	private void fireModelChanged(AnnotationModelEvent event) {
		event.markSealed();
		if (!event.isEmpty()) {
			for (final IAnnotationModelListener l : annotationModelListeners) {
				if (l instanceof IAnnotationModelListenerExtension) {
					((IAnnotationModelListenerExtension) l).modelChanged(event);
				} else {
					l.modelChanged(this);
				}
			}
		}
	}

	public void connect(IDocument document) {
		if (this.document != document) {
			throw new IllegalArgumentException(
					"Can't connect to different document."); //$NON-NLS-1$
		}
		for (final InappropriateAnnotation ia : annotations) {
			try {
				document.addPosition(ia.getPosition());
			} catch (BadLocationException ex) {
				throw new RuntimeException(ex);
			}
		}
		if (openConnections++ == 0) {
			document.addDocumentListener(documentListener);
		}
	}

	public void disconnect(IDocument document) {
		if (this.document != document) {
			throw new IllegalArgumentException(
					"Can't disconnect from different document."); //$NON-NLS-1$
		}
		for (final InappropriateAnnotation ia : annotations) {
			document.removePosition(ia.getPosition());
		}
		if (--openConnections == 0) {
			document.removeDocumentListener(documentListener);
		}
	}

	/**
	 * External modification is not supported.
	 */
	public void addAnnotation(Annotation annotation, Position position) {
		throw new UnsupportedOperationException();
	}

	/**
	 * External modification is not supported.
	 */
	public void removeAnnotation(Annotation annotation) {
		throw new UnsupportedOperationException();
	}

	public Iterator<?> getAnnotationIterator() {
		return annotations.iterator();
	}

	public Position getPosition(Annotation annotation) {
		if (annotation instanceof InappropriateAnnotation) {
			return ((InappropriateAnnotation) annotation).getPosition();
		} else {
			return null;
		}
	}
}
