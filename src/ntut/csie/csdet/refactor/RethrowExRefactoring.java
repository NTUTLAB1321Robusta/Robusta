package ntut.csie.csdet.refactor;

import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.DummyHandlerVisitor;
import ntut.csie.csdet.visitor.IgnoreExceptionVisitor;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
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
 * Rethrow Unchecked exception的具體操作都在這個class中
 * @author chewei
 */

public class RethrowExRefactoring extends Refactoring {
	private static Logger logger = LoggerFactory.getLogger(RethrowExRefactoring.class);
	
	private IJavaProject project;
	
	// 紀錄code smell的type
	private String problem;
	// 使用者所選擇的Exception Type
	private IType exType;
	
	// 使用者所點選的Marker
	private IMarker marker;
	
	private IOpenable actOpenable;
	
	// user 所填寫要丟出的Exception,預設是RunTimeException
	private String exceptionType;
	
	private TextFileChange textFileChange;
	
	// 目前method的RL Annotation資訊
	private List<RLMessage> currentMethodRLList = null;
	
	// 存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	// 存放目前所要fix的method node
	private MethodDeclaration currentMethodNode = null;
	
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
			// 去修改AST Tree
			collectChange(marker.getResource());
			// 不需check final condition
			RefactoringStatus status = new RefactoringStatus();		
			return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		// 不需check initial condition
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm)
								throws CoreException, OperationCanceledException {
		// 2010.07.20 之前的寫法，Preview的Token不會變色
		// 把要變更的結果包成composite傳出去
		// Change[] changes = new Change[] {textFileChange};
		// CompositeChange change = new CompositeChange("Rethrow Unchecked Exception", changes);

		String name = "Rethrow Unchecked Exception";
		ICompilationUnit unit = (ICompilationUnit) this.actOpenable;
		CompilationUnitChange result = new CompilationUnitChange(name, unit);
		result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

		// 將修改結果設置在CompilationUnitChange
		TextEdit edits = textFileChange.getEdit();
		result.setEdit(edits);
		// 將修改結果設成Group，會顯示在Preview上方節點。
		result.addTextEditGroup(new TextEditGroup("Rethrow Unchecked Exception", 
								new TextEdit[] {edits} ));

		return result;
	}

	@Override
	public String getName() {		
		return "Rethrow Unchecked Exception";
	}

	/**
	 * 把marker傳進來供此class存取一些code smell資訊
	 * @param marker
	 */
	public void setMarker(IMarker marker) {
		this.marker = marker;
		this.project = JavaCore.create(marker.getResource().getProject());
	}
	
	/**
	 * parse AST Tree並取得要修改的method node
	 * @param resource
	 */
	private void collectChange(IResource resource) {
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);

			// 取得Method相關資訊
			if (findMethod(resource)) {
				// 去修改AST Tree的內容
				rethrowException();
			}
		} catch (CoreException e) {
			logger.error("[Find CS Method] EXCEPTION ",e);
		}
	}
	
	/**
	 * 取得Method相關資訊
	 * @param resource		來源
	 * @param methodIdx		Method的Index
	 * @return				是否成功
	 */
	private boolean findMethod(IResource resource) { 
		// 取得要修改的CompilationUnit
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			try {
				IJavaElement javaElement = JavaCore.create(resource);
				if (javaElement instanceof IOpenable)
					actOpenable = (IOpenable) javaElement;
				
				// Create AST to parse
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				
				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				actRoot = (CompilationUnit) parser.createAST(null);
				
				// 取得該class所有的method
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<MethodDeclaration> methodList = methodCollector.getMethodList();
				
				// 取得目前要被修改的method node
				currentMethodNode = methodList.get(Integer.parseInt(methodIdx));
				if (currentMethodNode != null) {
					// 取得這個method的RL資訊
					ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
					currentMethodNode.accept(exVisitor);
					currentMethodRLList = exVisitor.getMethodRLAnnotationList();

					// 判斷是Ignore Ex or Dummy handler並取得code smell的List
					if(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)) {
						IgnoreExceptionVisitor visitor = new IgnoreExceptionVisitor(this.actRoot);
						currentMethodNode.accept(visitor);
						currentExList = visitor.getIgnoreList();
					} else {
						DummyHandlerVisitor visitor = new DummyHandlerVisitor(this.actRoot);
						currentMethodNode.accept(visitor);
						currentExList = visitor.getDummyList();
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
	 *建立Throw Exception的資訊 
	 */
	@Robustness(value = { @RTag(level = 1, exception = RuntimeException.class) })
	private void rethrowException() {
		try {
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
			
			// 準備在Catch Clause中加入throw exception
			// 取得EH smell的資訊
			msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
			MarkerInfo markerInfo = currentExList.get(Integer.parseInt(msgIdx));
			// 收集該method所有的catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<CatchClause> catchList = catchCollector.getMethodList();
			
			// 去比對startPosition,找出要修改的catch
			for (int i =0; i < catchList.size(); i++) {
				if(catchList.get(i).getStartPosition() == markerInfo.getPosition()) {
					catchIdx = i;
					// 在catch clause中建立throw statement
					addThrowStatement(catchList.get(i), ast);
					if(smellSettings.isAddingRobustnessAnnotation()) {
						// 建立RLAnnotation
						addAnnotationRoot(ast);
					}
					// 加入未import的Library(遇到RuntimeException就不用加Library)
					if (!exceptionType.equals("RuntimeException")) {
						addImportDeclaration();
						checkMethodThrow(ast);
						break;
					}
				}
			}
			// 寫回Edit中
			applyChange();
		}catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 檢查在method前面有沒有throw exception
	 * @param ast
	 */
	private void checkMethodThrow(AST ast) {
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		List<SimpleName> thStat = md.thrownExceptions();
		boolean isExist = false;
		for(int i=0;i<thStat.size();i++) {
			if(thStat.get(i).getNodeType() ==  ASTNode.SIMPLE_NAME) {
				SimpleName sn = (SimpleName)thStat.get(i);
				if(sn.getIdentifier().equals(exceptionType)) {
					isExist = true;
					break;
				}
			}
		}
		if(!isExist)
			thStat.add(ast.newSimpleName(this.exceptionType));
	}
	
	/**
	 * 在catch中增加throw new RuntimeException(..)
	 * @param cc
	 * @param ast
	 */
	private void addThrowStatement(CatchClause cc, AST ast) {
		// 取得該catch()中的exception variable
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
		.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		// 自行建立一個throw statement加入
		ThrowStatement ts = ast.newThrowStatement();
		// 將throw的variable傳入
		ClassInstanceCreation cic = ast.newClassInstanceCreation();
		//throw new RuntimeException()
		cic.setType(ast.newSimpleType(ast.newSimpleName(exceptionType)));
		// 將throw new RuntimeException(ex)括號中加入參數 
		cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
		
		// 取得CatchClause所有的statement,將相關print例外資訊的東西移除
		List<Statement> statement = cc.getBody().statements();
		if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
			// 假如要fix的code smell是dummy handler,就要把catch中的列印資訊刪除
			deleteStatement(statement);
		}
		// 將新建立的節點寫回
		ts.setExpression(cic);
		statement.add(ts);	
	}
	
	/**
	 * FIXME - 參數沒使用到 2012.3.30
	 * @param markerInfo
	 */
	private void applyChange() {		
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);
		} catch (JavaModelException e) {
			logger.error("[Apply Change Rethrow Unchecked Exception] EXCEPTION ", e);
		}
	}
	
	/**
	 * 在Rethrow之前,先將相關的print字串都清除掉
	 */
	private void deleteStatement(List<Statement> statementTemp) {
		// 從Catch Clause裡面剖析兩種情形
		if(statementTemp.size() != 0) {
			for(int i=0;i<statementTemp.size();i++) {		
				if(statementTemp.get(i) instanceof ExpressionStatement ) {
					ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);
					// 遇到System.out.print or printStackTrace就把他remove掉
					if (statement.getExpression().toString().contains("System.out.print") ||
						statement.getExpression().toString().contains("printStackTrace") ||
						statement.getExpression().toString().contains("System.err.print")) {	
						statementTemp.remove(i);
						// 移除完之後ArrayList的位置會重新調整過,所以利用遞回來繼續往下找符合的條件並移除
						i--;
					}
				}			
			}
		}
	}
	
	private void addAnnotationRoot(AST ast) {
		// 要建立@Robustness(value={@Tag(level=1, exception=java.lang.RuntimeException.class)})這樣的Annotation
		// 建立Annotation root
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));

		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);
		
		MethodDeclaration method = (MethodDeclaration) currentMethodNode;		
		if(currentMethodRLList.size() == 0) {		
			rlary.expressions().add(getRLAnnotation(ast,1,exceptionType));
		} else {
		
			for(RLMessage rlmsg : currentMethodRLList) {
				// 把舊的annotation加進去
				int pos = rlmsg.getRLData().getExceptionType().toString().lastIndexOf(".");
				String cut = rlmsg.getRLData().getExceptionType().toString().substring(pos+1);
				
				// 如果有有RL annotation重複就不加進去
				if((!cut.equals(exceptionType)) && (rlmsg.getRLData().getLevel() == 1)) {					
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
				}	
			}
			rlary.expressions().add(getRLAnnotation(ast,1,exceptionType));
			
			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				// 找到舊有的annotation後將它移除
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}
		}
		if (rlary.expressions().size() > 0) {
			method.modifiers().add(0, root);
		}
		// 將RL的library加進來
		addImportRLDeclaration();
	}
	
	
	/**
	 * 產生RL Annotation之RL資料
	 * @param ast:AST Object
	 * @param levelVal:強健度等級
	 * @param exClass:例外類別
	 * @return NormalAnnotation AST Node
	 */
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal, String excption) {
		// 要建立@Robustness(value={@Tag(level=1, exception=java.lang.RuntimeException.class)})這樣的Annotation
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName(RTag.class.getSimpleName().toString()));

		// level = 1
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName(RTag.LEVEL));
		//throw statement 預設level = 1
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		rl.values().add(level);

		// exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName(RTag.EXCEPTION));
		TypeLiteral exclass = ast.newTypeLiteral();
		// 預設為RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);
	
		return rl;
	}
	
	/**
	 * 判斷是否有未加入的Library,但throw RuntimeException的情況要排除
	 * 因為throw RuntimeException不需import Library
	 */
	private void addImportDeclaration() {
		// 判斷是否有import library
		boolean isImportLibrary = false;
		List<ImportDeclaration> importList = actRoot.imports();
		for(ImportDeclaration id : importList) {
			if(exType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName()))
				isImportLibrary = true;
		}
		
		// 假如沒有import就加入到AST中
		AST rootAst = actRoot.getAST(); 
		if(!isImportLibrary) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(exType.getFullyQualifiedName()));
			actRoot.imports().add(imp);
		}
	}

	private void addImportRLDeclaration() {
		// 判斷是否已經Import Robustness及RL的宣告
		List<ImportDeclaration> importList = actRoot.imports();
		boolean isImportRobustnessClass = false;
		boolean isImportRLClass = false;
		for (ImportDeclaration id : importList) {
			if (RLData.CLASS_ROBUSTNESS.equals(id.getName().getFullyQualifiedName()))
				isImportRobustnessClass = true;
			if (RLData.CLASS_RL.equals(id.getName().getFullyQualifiedName()))
				isImportRLClass = true;
		}

		AST rootAst = this.actRoot.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			actRoot.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RTag.class.getName()));
			actRoot.imports().add(imp);
		}
	}
	
	/**
	 * 紀錄user所要throw的exception type
	 * @param name : exception type
	 */
	public RefactoringStatus setExceptionName(String name) {
		// 假如使用者沒有填寫任何東西,把RefactoringStatus設成Error
		if(name.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Please Choose an Exception Type");
		} else {
			// 假如有寫就把他存下來
			exceptionType = name;
			return new RefactoringStatus();
		}		
	}
	
	/**
	 * 取得JavaProject
	 */
	public IJavaProject getProject() {
		return project;
	}
	
	/**
	 * 儲存要Throw的Exception位置(要import使用)
	 * @param type
	 */
	public void setExType(IType type) {		
		exType = type;
	}
	
	/**
	 * 交換Annotation順序，再定位
	 */
	public void changeAnnotation() {
		if (methodIdx != null && msgIdx != null) {
			// 交換Annotation的順序
			new RLOrderFix().run(marker.getResource(), methodIdx, msgIdx);
			// 定位
			selectSourceLine();
		}
	}
	
	/**
	 * 取得Throw Statement行數
	 * @param catchIdx	catch的index
	 * @return			反白行數
	 */
	private int getThrowStatementSourceLine(int catchIdx) {
		// 反白行數
		int selectLine = -1;

		if (catchIdx != -1) {
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<CatchClause> catchList = catchCollector.getMethodList();
			// 尋找Throw statement的行數
			List<?> catchStatements = catchList.get(catchIdx).getBody().statements();
			for (int i = 0; i < catchStatements.size(); i++) {
				if (catchStatements.get(i) instanceof ThrowStatement) {
					ThrowStatement statement = (ThrowStatement) catchStatements.get(i);
					// 誰可以告訴我，為什麼selectLine要減一以後才回傳? charles 20120912
					selectLine = this.actRoot.getLineNumber(statement.getStartPosition()) - 1;
					return selectLine;
				}
			}
		}
		return selectLine;
	}
	
	/**
	 * 反白指定行數
	 * @param marker		欲反白Statement的Resource
	 * @param methodIdx		欲反白Statement的Method Index
	 * @param catchIdx		欲反白Statement的Catch Index
	 */
	private void selectSourceLine() {
		// 重新取得Method資訊
		boolean isOK = findMethod(marker.getResource());
		if (isOK) {
			try {
				ICompilationUnit cu = (ICompilationUnit) actOpenable;
				Document document = new Document(cu.getBuffer().getContents());
				// 取得目前的EditPart
				IEditorPart editorPart = EditorUtils.getActiveEditor();
				ITextEditor editor = (ITextEditor) editorPart;
	
				// 取得反白Statement的行數
				int selectLine = getThrowStatementSourceLine(catchIdx);
				// 若反白行數為
				if (selectLine == -1) {
					// 取得Method的起點位置
					int srcPos = currentMethodNode.getStartPosition();
					// 用Method起點位置取得Method位於第幾行數(起始行數從0開始，不是1，所以減1)
					selectLine = actRoot.getLineNumber(srcPos) - 1;
				}
				//取得反白行數在SourceCode的行數資料
				IRegion lineInfo = document.getLineInformation(selectLine);

				//反白該行 在Quick fix完之後,可以將游標定位在Quick Fix那行
				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			} catch (JavaModelException e) {
				logger.error("[Rethrow checked Exception] EXCEPTION ", e);
			} catch (BadLocationException e) {
				logger.error("[BadLocation] EXCEPTION ", e);
			}
		}
	}
}
