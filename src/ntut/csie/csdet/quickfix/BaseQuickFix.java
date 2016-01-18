package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.analyzer.ASTMethodCollector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.javaeditor.EditorUtility;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.IUndoManager;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseQuickFix {  
	private static Logger logger = LoggerFactory.getLogger(BaseQuickFix.class);

	protected IOpenable actOpenable = null;
	protected CompilationUnit javaFileWillBeQuickFixed = null;
	protected MethodDeclaration methodNodeWillBeQuickFixed = null;

	protected boolean findMethodNodeWillBeQuickFixed(IResource resource, int methodIdx){
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			try {
				IJavaElement javaElement = JavaCore.create(resource);
				
				if (javaElement instanceof IOpenable) {
					actOpenable = (IOpenable) javaElement;
				}
				
				//Create AST to parse
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
	
				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				javaFileWillBeQuickFixed = (CompilationUnit) parser.createAST(null);
				javaFileWillBeQuickFixed.recordModifications();
				
				//collect all methods in a class
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				javaFileWillBeQuickFixed.accept(methodCollector);
				List<MethodDeclaration> methodList = methodCollector.getMethodList();
				
				methodNodeWillBeQuickFixed = methodList.get(methodIdx);
			
				return true;
			} catch (Exception ex) {
				logger.error("[Find DH Method] EXCEPTION ",ex);
			}
		}
		return false;
	}

	/**
	 * update the change of Editor (Old)
	 */
	protected void applyChange() {
		applyChange(null);
	}
	
	/**
	 * update the change of Editor (New)
	 * @param msg
	 */
	protected void applyChange(ASTRewrite rewrite) {
		try {
			// consult org.eclipse.jdt.internal.ui.text.correction.CorrectionMarkerResolutionGenerator run
			// org.eclipse.jdt.internal.ui.text.correction.ChangeCorrectionProposal apply與performChange
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			IEditorPart part = EditorUtility.isOpenInEditor(cu);
			IEditorInput input = part.getEditorInput();
			IDocument doc = JavaPlugin.getDefault().getCompilationUnitDocumentProvider().getDocument(input);

			performChange(JavaPlugin.getActivePage().getActiveEditor(), doc, rewrite);
		} catch (CoreException e) {
			logger.error("[Core Exception] EXCEPTION ",e);
		}
	}

	/**
	 * invoke Quick Fix
	 * @param activeEditor
	 * @param document
	 * @throws CoreException
	 */
	private void performChange(IEditorPart activeEditor, IDocument document, ASTRewrite rewrite) throws CoreException {
		Change change= null;
		IRewriteTarget rewriteTarget= null;
		try {
			change= getChange(javaFileWillBeQuickFixed, rewrite);
			if (change != null) {
				if (document != null) {
					LinkedModeModel.closeAllModels(document);
				}
				if (activeEditor != null) {
					rewriteTarget= (IRewriteTarget) activeEditor.getAdapter(IRewriteTarget.class);
					if (rewriteTarget != null) {
						rewriteTarget.beginCompoundChange();
					}
				}

				change.initializeValidationData(new NullProgressMonitor());
				RefactoringStatus valid= change.isValid(new NullProgressMonitor());
				if (valid.hasFatalError()) {
					IStatus status= new Status(IStatus.ERROR, JavaPlugin.getPluginId(), IStatus.ERROR,
						valid.getMessageMatchingSeverity(RefactoringStatus.FATAL), null);
					throw new CoreException(status);
				} else {
					IUndoManager manager= RefactoringCore.getUndoManager();
					manager.aboutToPerformChange(change);
					Change undoChange= change.perform(new NullProgressMonitor());
					manager.changePerformed(change, true);
					if (undoChange != null) {
						undoChange.initializeValidationData(new NullProgressMonitor());
						manager.addUndo("Quick Undo", undoChange);
					}
				}
			}
		} finally {
			if (rewriteTarget != null) {
				rewriteTarget.endCompoundChange();
			}

			if (change != null) {
				change.dispose();
			}
		}
	}

	/**
	 * get modified code after quick fix
	 */
	private Change getChange(CompilationUnit actRoot, ASTRewrite rewrite) {
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());

			TextEdit edits = null;
			if (rewrite != null)
				edits = rewrite.rewriteAST(document, null);
			else
				edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));

			TextFileChange textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);

			return textFileChange;
		} catch (JavaModelException e) {
			logger.error("[Apply Change Rethrow Unchecked Exception] EXCEPTION ",e);
		}
		return null;
	}
}
