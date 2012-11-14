package ntut.csie.robusta.codegen.refactoring;

import ntut.csie.jdt.util.Clazz;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.codegen.CatchClauseFinderVisitor;
import ntut.csie.robusta.codegen.QuickFixUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefineExceptionRefactoring extends Refactoring {
	private static Logger logger = LoggerFactory.getLogger(RefineExceptionRefactoring.class);

	private String problem;
	private int methodIndex;
	private CompilationUnit compilationUnit;
	private int catchClauseStartPosition;
	private IJavaProject project;
	private IType exceptionType;
	private String exceptionName;
	private IOpenable iOpenable;

	public RefineExceptionRefactoring(IMarker marker) throws CoreException {
		project = JavaCore.create(marker.getResource().getProject());
		problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
		compilationUnit = QuickFixUtils.getCompilationUnit(marker.getResource());
		iOpenable = QuickFixUtils.getIOpenable(marker.getResource());
		methodIndex = Integer.parseInt((String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX));
		catchClauseStartPosition = Integer.parseInt(marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString());
		exceptionName = RuntimeException.class.getSimpleName();
	}

	/**
	 * �o�ӦW�r�|��ܦb Undo Redo �M��W��
	 */
	@Override
	public String getName() {
		return "Rethrow Unchecked Exception";
	}

	/**
	 * �ϥέ��c�e����l���A�A��ĳ���ˬd�{���X�O�_�����~�C
	 * �i�H�]�w���u�{���X�L���~�~���ѭ��c�\��v�C
	 */
	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		return new RefactoringStatus();
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		// TODO �n�p�����O���L�����ܡA�b�ϥΪ̿��back���ɭԤ@��Roll back??
		AST cuAST = compilationUnit.getAST();
		CatchClauseFinderVisitor catchClauseFinder = new CatchClauseFinderVisitor(catchClauseStartPosition);
		compilationUnit.accept(catchClauseFinder);
		CatchClause exactlyCatchClause = catchClauseFinder.getFoundCatchClause();
		if(exactlyCatchClause == null) {
			status.addFatalError("Can't find the catch Clause");
			return status;
		}
		QuickFixUtils.removeStatementsInCatchClause(exactlyCatchClause, ".printStackTrace()", "System.out.print", "System.err.print");
		QuickFixUtils.addThrowRefinedException(exactlyCatchClause, cuAST, exceptionName);
		// ���O�ߥX RuntimeException�N�n���Limport (���Unchecked Exception������import�A���O�ڥ����ް�)
		if(!exceptionName.equals(RuntimeException.class.getSimpleName())) {
			QuickFixUtils.addImportDeclaration(compilationUnit, exceptionType);
		}
		// �p�G�ߥX���OChecked exception�A�N�n���L�ŧi�bmethod�W��
		if(!Clazz.isUncheckedException(exceptionName)) {
			QuickFixUtils.addThrowExceptionOnMethodDeclaration(
					cuAST, 
					(MethodDeclaration) NodeUtils.getSpecifiedParentNode(exactlyCatchClause,
					ASTNode.METHOD_DECLARATION), exceptionName);
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		
		String name = "Rethrow Unchecked Exception";
		ICompilationUnit unit = (ICompilationUnit) iOpenable;
		CompilationUnitChange result = new CompilationUnitChange(name, unit);
		result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

		// �N�קﵲ�G�]�m�bCompilationUnitChange
		TextEdit edits = applyRefactoringChange().getEdit();
		result.setEdit(edits);
		// �N�קﵲ�G�]��Group�A�|��ܦbPreview�W��`�I�C
		result.addTextEditGroup(new TextEditGroup("Rethrow Unchecked Exception", 
								new TextEdit[] {edits} ));
		return result;
	}

	public RefactoringStatus setExceptionName(String name) {
		// ���p�ϥΪ̨S����g����F��,��RefactoringStatus�]��Error
		if(name.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Please Choose an Exception Type");
		} else {
			// ���p���g�N��L�s�U��
			exceptionName = name;
			return new RefactoringStatus();
		}
	}

	public void setExType(IType exType) {
		exceptionType = exType;
	}

	public IJavaProject getProject() {
		return project;
	}
	
	/**
	 * �M��Refactoring�n�ܧ󪺤��e
	 * @param textFileChange
	 * @throws CoreException 
	 */
	public TextFileChange applyRefactoringChange() throws JavaModelException {
		ICompilationUnit cu = (ICompilationUnit) iOpenable;
		Document document = new Document(cu.getBuffer().getContents());
		TextEdit edits = compilationUnit.rewrite(document, cu.getJavaProject().getOptions(true));
		TextFileChange textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
		textFileChange.setEdit(edits);
		return textFileChange;
	}

}
