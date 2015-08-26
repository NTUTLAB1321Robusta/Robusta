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
package ntut.csie.rleht.builder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ISourceReference;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
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

public class InappropriateAnnotationModel implements IAnnotationModel {
	  /** Key used to piggyback our model to the editor's model. */
	  private static final Object KEY = new Object();

	  private static MarkerModel markerModel = null;
	  
	  /** List of current CoverageAnnotation objects */
	  private List<InappropriateAnnotation> annotations = new ArrayList<InappropriateAnnotation>(32);

	  /** List of registered IAnnotationModelListener */
	  private List<IAnnotationModelListener> annotationModelListeners = new ArrayList<IAnnotationModelListener>(2);

	  private final ITextEditor editor;
	  private final IDocument document;
	  private int openConnections = 0;
	  private boolean annotated = false;

	  private IDocumentListener documentListener = new IDocumentListener() {
	    public void documentChanged(DocumentEvent event) {
	      updateAnnotations(false);
	    }

	    public void documentAboutToBeChanged(DocumentEvent event) {
	    }
	  };

	  private InappropriateAnnotationModel(ITextEditor editor, IDocument document) {
	    this.editor = editor;
	    this.document = document;
	    updateAnnotations(true);
	  }

	  /**
	   * Attaches a inappropriate annotation model for the given editor if the editor can
	   * be annotated. Does nothing if the model is already attached.
	   * 
	   * @param editor
	   *          Editor to attach a annotation model to
	   */
	  public static void attach(ITextEditor editor) {
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
	      iaModel = new InappropriateAnnotationModel(editor, document);
	      modelex.addAnnotationModel(KEY, iaModel);
	    }
	  }

	  /**
	   * Detaches the inappropriate annotation model from the given editor. If the editor
	   * does not have a model attached, this method does nothing.
	   * 
	   * @param editor
	   *          Editor to detach the annotation model from
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
		// TODO: update bad smell marker for single editor
//	    final ISourceNode coverage = findSourceCoverageForEditor();
//	    if (coverage != null) {
//	      if (!annotated || force) {
//	        createAnnotations(coverage);
//	        annotated = true;
//	      }
//	    } else {
//	      if (annotated) {
//	        clear();
//	        annotated = false;
//	      }
//	    }
	  }
	  // TODO: update bad smell marker for single editor
//	  private ISourceNode findSourceCoverageForEditor() {
//	    if (editor.isDirty()) {
//	      return null;
//	    }
//	    final IEditorInput input = editor.getEditorInput();
//	    if (input == null) {
//	      return null;
//	    }
//	    final Object element = input.getAdapter(IJavaElement.class);
//	    if (!hasSource((IJavaElement) element)) {
//	      return null;
//	    }
//	    return findSourceCoverageForElement(element);
//	  }

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
	// TODO: update bad smell marker for single editor
//	  private ISourceNode findSourceCoverageForElement(Object element) {
//	    // Do we have a coverage info for the editor input?
//	    ICoverageNode coverage = CoverageTools.getCoverageInfo(element);
//	    if (coverage instanceof ISourceNode) {
//	      return (ISourceNode) coverage;
//	    }
//	    return null;
//	  }

	  private void clear() {
	    AnnotationModelEvent event = new AnnotationModelEvent(this);
	    clear(event);
	    fireModelChanged(event);
	  }

	  private void clear(AnnotationModelEvent event) {
	    for (final InappropriateAnnotation ca : annotations) {
	      event.annotationRemoved(ca, ca.getPosition());
	    }
	    annotations.clear();
	  }

	  // TODO: create annotations for bad smell
//	  private void createAnnotations(final ISourceNode linecoverage) {
//	    AnnotationModelEvent event = new AnnotationModelEvent(this);
//	    clear(event);
//	    final int firstline = linecoverage.getFirstLine();
//	    final int lastline = Math.min(linecoverage.getLastLine(),
//	        document.getNumberOfLines());
//	    try {
//	      for (int l = firstline; l <= lastline; l++) {
//	        final ILine line = linecoverage.getLine(l);
//	        if (line.getStatus() != ICounter.EMPTY) {
//	          final IRegion region = document.getLineInformation(l - 1);
//	          final CoverageAnnotation ca = new CoverageAnnotation(
//	              region.getOffset(), region.getLength(), line);
//	          annotations.add(ca);
//	          event.annotationAdded(ca);
//	        }
//	      }
//	    } catch (BadLocationException ex) {
//	      ex.printStackTrace(); //TODO:
//	    }
//	    fireModelChanged(event);
//	  }

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
	      throw new IllegalArgumentException("Can't connect to different document."); //$NON-NLS-1$
	    }
	    for (final InappropriateAnnotation ca : annotations) {
	      try {
	        document.addPosition(ca.getPosition());
	      } catch (BadLocationException ex) {
	        ex.printStackTrace(); //TODO:
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
	    for (final InappropriateAnnotation ca : annotations) {
	      document.removePosition(ca.getPosition());
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
