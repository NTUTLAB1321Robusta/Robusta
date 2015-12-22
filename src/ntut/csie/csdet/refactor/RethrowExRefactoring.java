package ntut.csie.csdet.refactor;

import java.util.List;

import ntut.csie.analyzer.ASTCatchCollect;
import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.analyzer.dummy.DummyHandlerVisitor;
import ntut.csie.analyzer.empty.EmptyCatchBlockVisitor;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.builder.RLOrderFix;
import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * the implementation of rethrow unchecked exception is in this class 
 * @author chewei
 */

public class RethrowExRefactoring extends Refactoring {
	private static Logger logger = LoggerFactory.getLogger(RethrowExRefactoring.class);
	
	private IJavaProject project;
	
	private String badSmellType;
	
	private IType userSelectingexceptionType;
	
	private IMarker marker;
	
	private IOpenable actOpenable;
	
	private String exceptionTypeWillBeRethrown;
	
	private TextFileChange textFileChange;
	
	private List<RLMessage> methodRobustnessLevelList = null;
	
	private CompilationUnit javaFileWillBeRefactored;
	
	private MethodDeclaration methodNodeWillBeRefactored = null;
	
	private List<MarkerInfo> currentExList = null;
	
	private String msgIdx;
	String methodIdx;
	int catchIdx = -1;
	
	private SmellSettings smellSettings;
	
	public RethrowExRefactoring() {
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
	}
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
			collectChange(marker.getResource());
			RefactoringStatus status = new RefactoringStatus();		
			return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm)
								throws CoreException, OperationCanceledException {
		String name = "Rethrow Unchecked Exception";
		ICompilationUnit unit = (ICompilationUnit) this.actOpenable;
		CompilationUnitChange result = new CompilationUnitChange(name, unit);
		result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

		TextEdit edits = textFileChange.getEdit();
		result.setEdit(edits);
		result.addTextEditGroup(new TextEditGroup("Rethrow Unchecked Exception", 
								new TextEdit[] {edits} ));

		return result;
	}

	@Override
	public String getName() {		
		return "Rethrow Unchecked Exception";
	}

	public void setMarker(IMarker marker) {
		this.marker = marker;
		this.project = JavaCore.create(marker.getResource().getProject());
	}
	
	private void collectChange(IResource resource) {
		try {
			badSmellType = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);

			if (findMethod(resource)) {
				rethrowException();
			}
		} catch (CoreException e) {
			logger.error("[Find CS Method] EXCEPTION ",e);
		}
	}
	
	/**
	 * get the information of method which will be refactored
	 * @param resource		
	 * @param methodIdx		Method的Index
	 * @return				
	 */
	private boolean findMethod(IResource resource) { 
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			try {
				IJavaElement javaElement = JavaCore.create(resource);
				if (javaElement instanceof IOpenable)
					actOpenable = (IOpenable) javaElement;
				
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				
				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				javaFileWillBeRefactored = (CompilationUnit) parser.createAST(null);
				
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				javaFileWillBeRefactored.accept(methodCollector);
				List<MethodDeclaration> methodList = methodCollector.getMethodList();
				
				methodNodeWillBeRefactored = methodList.get(Integer.parseInt(methodIdx));
				if (methodNodeWillBeRefactored != null) {
					// get robustness level information of method
					ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.javaFileWillBeRefactored, methodNodeWillBeRefactored.getStartPosition(), 0);
					methodNodeWillBeRefactored.accept(exVisitor);
					methodRobustnessLevelList = exVisitor.getMethodRLAnnotationList();

					// Check if it is Empty Catch Block or Dummy handler, and get code smell list
					if(badSmellType.equals(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK)) {
						EmptyCatchBlockVisitor visitor = new EmptyCatchBlockVisitor(this.javaFileWillBeRefactored);
						methodNodeWillBeRefactored.accept(visitor);
						currentExList = visitor.getEmptyCatchList();
					} else {
						DummyHandlerVisitor visitor = new DummyHandlerVisitor(this.javaFileWillBeRefactored);
						methodNodeWillBeRefactored.accept(visitor);
						currentExList = visitor.getDummyHandlerList();
					}
				}
				return true;
			} catch (Exception ex) {
				logger.error("[Find CS Method] EXCEPTION ",ex);
			}
		}
		return false;
	}
	
	/**
	 *establish rethrow exception 
	 */
	@Robustness(value = { @RTag(level = 1, exception = RuntimeException.class) })
	private void rethrowException() {
		try {
			javaFileWillBeRefactored.recordModifications();
			AST ast = methodNodeWillBeRefactored.getAST();
			
			// add throw exception in catch clause
			msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
			MarkerInfo markerInfo = currentExList.get(Integer.parseInt(msgIdx));
			// collect all catch clause of method
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			methodNodeWillBeRefactored.accept(catchCollector);
			List<CatchClause> catchList = catchCollector.getMethodList();
			
			// compare the position to find out specified catch clause which will be refactored.
			for (int i =0; i < catchList.size(); i++) {
				if(catchList.get(i).getStartPosition() == markerInfo.getPosition()) {
					catchIdx = i;
					// add throw statement in catch clause
					addThrowExceptionStatement(catchList.get(i), ast);
					if(smellSettings.isAddingRobustnessAnnotation()) {
						addAnnotationRoot(ast);
					}
					if (!exceptionTypeWillBeRethrown.equals("RuntimeException")) {
						importExceptionLibrary();
						/* don't need to declare RuntimeExcpetion on method signature 
						 because user can only select RuntimeException on the search dialog */
						break;
					}
				}
			}
			applyChange();
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * check whether there is an exception declaration on method signature.
	 * @param ast
	 */
	private void checkMethodThrow(AST ast) {
		MethodDeclaration md = (MethodDeclaration)methodNodeWillBeRefactored;
		List<SimpleName> thStat = md.thrownExceptions();
		boolean isExist = false;
		for(int i=0;i<thStat.size();i++) {
			if(thStat.get(i).getNodeType() ==  ASTNode.SIMPLE_NAME) {
				SimpleName sn = (SimpleName)thStat.get(i);
				if(sn.getIdentifier().equals(exceptionTypeWillBeRethrown)) {
					isExist = true;
					break;
				}
			}
		}
		if(!isExist)
			thStat.add(ast.newSimpleName(this.exceptionTypeWillBeRethrown));
	}
	
	
	private void addThrowExceptionStatement(CatchClause cc, AST ast) {
		// get exception variable from catch clause expression
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
		.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		ThrowStatement ts = ast.newThrowStatement();
		ClassInstanceCreation cic = ast.newClassInstanceCreation();
		//throw new RuntimeException()
		cic.setType(ast.newSimpleType(ast.newSimpleName(exceptionTypeWillBeRethrown)));
		// add argument to throw new RuntimeException() 
		cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
		
		List<Statement> statement = cc.getBody().statements();
		if(badSmellType.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
			// delete the statement which will cause dummy handler
			deleteStatement(statement);
		}
		ts.setExpression(cic);
		statement.add(ts);	
	}
	
	private void applyChange() {		
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			TextEdit edits = javaFileWillBeRefactored.rewrite(document, cu.getJavaProject().getOptions(true));
			textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);
		} catch (JavaModelException e) {
			logger.error("[Apply Change Rethrow Unchecked Exception] EXCEPTION ", e);
		}
	}
	
	/**
	 * delete the statement which will cause dummy handler
	 */
	private void deleteStatement(List<Statement> statementTemp) {
		if(statementTemp.size() != 0) {
			for(int i=0;i<statementTemp.size();i++) {		
				if(statementTemp.get(i) instanceof ExpressionStatement ) {
					ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);
					// remove System.out.print and printStackTrace
					if (statement.getExpression().toString().contains("System.out.print") ||
						statement.getExpression().toString().contains("printStackTrace") ||
						statement.getExpression().toString().contains("System.err.print")) {	
						statementTemp.remove(i);
						i--;
					}
				}			
			}
		}
	}
	
	private void addAnnotationRoot(AST ast) {
		// establish the annotation like "@Robustness(value={@RTag(level=1, exception=java.lang.RuntimeException.class)})"
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));

		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);
		
		MethodDeclaration method = (MethodDeclaration) methodNodeWillBeRefactored;		
		if(methodRobustnessLevelList.size() == 0) {		
			rlary.expressions().add(getRLAnnotation(ast,1,exceptionTypeWillBeRethrown));
		} else {
		
			for(RLMessage rlmsg : methodRobustnessLevelList) {
				int pos = rlmsg.getRLData().getExceptionType().toString().lastIndexOf(".");
				String cut = rlmsg.getRLData().getExceptionType().toString().substring(pos+1);
				
				// if there are duplicate RL annotation then ignore it.
				if((!cut.equals(exceptionTypeWillBeRethrown)) && (rlmsg.getRLData().getLevel() == 1)) {					
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
				}	
			}
			rlary.expressions().add(getRLAnnotation(ast,1,exceptionTypeWillBeRethrown));
			
			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				// remove existing annotation
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}
		}
		if (rlary.expressions().size() > 0) {
			method.modifiers().add(0, root);
		}
		importRobuatnessLevelLibrary();
	}
	
	
	/**
	 * generate robustness level information for robustness level annotation
	 * @param ast:AST Object
	 * @param robustnessLevelVal
	 * @param exceptionType
	 * @return NormalAnnotation AST Node
	 */
	private NormalAnnotation getRLAnnotation(AST ast, int robustnessLevelVal, String exceptionType) {
		// generate the annotation like "@Robustness(value={@RTag(level=1, exception=java.lang.RuntimeException.class)})"
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName(RTag.class.getSimpleName().toString()));

		// level = 1
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName(RTag.LEVEL));
		//default level of throw statement is 1
		level.setValue(ast.newNumberLiteral(String.valueOf(robustnessLevelVal)));
		rl.values().add(level);

		// exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName(RTag.EXCEPTION));
		TypeLiteral exclass = ast.newTypeLiteral();
		// default exception is RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(exceptionType)));
		exception.setValue(exclass);
		rl.values().add(exception);
	
		return rl;
	}
	
	/**
	 * import required exception's library except RuntimeException, because it doesn't need to import RuntimeException's library.
	 */
	private void importExceptionLibrary() {
		boolean isImportLibrary = false;
		List<ImportDeclaration> importList = javaFileWillBeRefactored.imports();
		for(ImportDeclaration id : importList) {
			if(userSelectingexceptionType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName()))
				isImportLibrary = true;
		}
		
		AST rootAst = javaFileWillBeRefactored.getAST(); 
		if(!isImportLibrary) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(userSelectingexceptionType.getFullyQualifiedName()));
			javaFileWillBeRefactored.imports().add(imp);
		}
	}

	private void importRobuatnessLevelLibrary() {
		List<ImportDeclaration> importList = javaFileWillBeRefactored.imports();
		boolean isImportRobustnessClass = false;
		boolean isImportRLClass = false;
		for (ImportDeclaration id : importList) {
			if (RLData.CLASS_ROBUSTNESS.equals(id.getName().getFullyQualifiedName()))
				isImportRobustnessClass = true;
			if (RLData.CLASS_RL.equals(id.getName().getFullyQualifiedName()))
				isImportRLClass = true;
		}

		AST rootAst = this.javaFileWillBeRefactored.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			javaFileWillBeRefactored.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RTag.class.getName()));
			javaFileWillBeRefactored.imports().add(imp);
		}
	}
	
	public RefactoringStatus setExceptionType(String exceptionType) {
		if(exceptionType.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Please Choose an Exception Type");
		} else {
			exceptionTypeWillBeRethrown = exceptionType;
			return new RefactoringStatus();
		}		
	}
	
	public IJavaProject getProject() {
		return project;
	}
	
	/**
	 * save the exception type which user has selected and that is used for to import library 
	 * @param exceptionType
	 */
	public void setUserSelectingExceptionType(IType exceptionType) {		
		userSelectingexceptionType = exceptionType;
	}
	
	public void swapTheIndexOfAnnotation() {
		if (methodIdx != null && msgIdx != null) {
			// swap Annotation's index
			new RLOrderFix().run(marker.getResource(), methodIdx, msgIdx);
			// highlight line number
			highLightSpecifiedLineNumber();
		}
	}
	
	private int getLineNumberOfThrowExceptionStatement(int catchIdx) {
		int selectLine = -1;

		if (catchIdx != -1) {
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			methodNodeWillBeRefactored.accept(catchCollector);
			List<CatchClause> catchList = catchCollector.getMethodList();
			List<?> catchStatements = catchList.get(catchIdx).getBody().statements();
			for (int i = 0; i < catchStatements.size(); i++) {
				if (catchStatements.get(i) instanceof ThrowStatement) {
					ThrowStatement statement = (ThrowStatement) catchStatements.get(i);
					// due to the line number is 0 based, so we should subtract by 1 to get correct line number
					selectLine = this.javaFileWillBeRefactored.getLineNumber(statement.getStartPosition()) - 1;
					return selectLine;
				}
			}
		}
		return selectLine;
	}
	
	private void highLightSpecifiedLineNumber() {
		boolean isOK = findMethod(marker.getResource());
		if (isOK) {
			try {
				ICompilationUnit cu = (ICompilationUnit) actOpenable;
				Document document = new Document(cu.getBuffer().getContents());
				IEditorPart editorPart = EditorUtils.getActiveEditor();
				ITextEditor editor = (ITextEditor) editorPart;
	
				int selectLine = getLineNumberOfThrowExceptionStatement(catchIdx);
				if (selectLine == -1) {
					int srcPos = methodNodeWillBeRefactored.getStartPosition();
					// due to the line number is 0 based, so we should subtract by 1 to get correct line number
					selectLine = javaFileWillBeRefactored.getLineNumber(srcPos) - 1;
				}
				IRegion lineInfo = document.getLineInformation(selectLine);

				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			} catch (JavaModelException e) {
				logger.error("[Rethrow checked Exception] EXCEPTION ", e);
			} catch (BadLocationException e) {
				logger.error("[BadLocation] EXCEPTION ", e);
			}
		}
	}
}
