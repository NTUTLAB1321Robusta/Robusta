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
	 * 這個名字會顯示在 Undo Redo 清單上面
	 */
	@Override
	public String getName() {
		return "Rethrow Unchecked Exception";
	}

	/**
	 * 使用重構前的初始狀態，建議先檢查程式碼是否有錯誤。
	 * 可以設定成「程式碼無錯誤才提供重構功能」。
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
		// TODO 要如何讓記錄過的改變，在使用者選擇back的時候一併Roll back??
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
		// 不是拋出 RuntimeException就要幫他import (其實Unchecked Exception都不用import，但是我先不管啦)
		if(!exceptionName.equals(RuntimeException.class.getSimpleName())) {
			QuickFixUtils.addImportDeclaration(compilationUnit, exceptionType);
		}
		// 如果拋出的是Checked exception，就要幫他宣告在method上面
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

		// 將修改結果設置在CompilationUnitChange
		TextEdit edits = applyRefactoringChange().getEdit();
		result.setEdit(edits);
		// 將修改結果設成Group，會顯示在Preview上方節點。
		result.addTextEditGroup(new TextEditGroup("Rethrow Unchecked Exception", 
								new TextEdit[] {edits} ));
		return result;
	}

	public RefactoringStatus setExceptionName(String name) {
		// 假如使用者沒有填寫任何東西,把RefactoringStatus設成Error
		if(name.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Please Choose an Exception Type");
		} else {
			// 假如有寫就把他存下來
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
	 * 套用Refactoring要變更的內容
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
