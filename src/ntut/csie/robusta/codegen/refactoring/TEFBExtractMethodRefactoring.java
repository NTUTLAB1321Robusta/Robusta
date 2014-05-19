package ntut.csie.robusta.codegen.refactoring;

import java.util.List;

import ntut.csie.util.NodeUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ImportRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jdt.internal.corext.codemanipulation.StubUtility;
import org.eclipse.jdt.internal.corext.dom.ASTNodeFactory;
import org.eclipse.jdt.internal.corext.dom.ASTNodes;
import org.eclipse.jdt.internal.corext.refactoring.ParameterInfo;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;

@SuppressWarnings({"restriction", "unchecked"})
public class TEFBExtractMethodRefactoring extends org.eclipse.jdt.internal.corext.refactoring.code.ExtractMethodRefactoring {
	private String logType;
	private String modifierType;
	private ASTRewrite rewrite;
	private CompilationUnit cunit;
	private ASTNode enclosingNode;
	private AST ast;
	private ExtractMethodAnalyzer analyzer;
	private ImportRewrite importWriter;
	private BodyDeclaration bodyDeclaration;
	private ICompilationUnit iCompilationUnit;
	private String loggerName = "logger";
	
	public TEFBExtractMethodRefactoring(CompilationUnit root, ASTNode enclosingNode) {
		super(root, enclosingNode.getStartPosition(), enclosingNode.getLength());
		this.enclosingNode = enclosingNode;
		this.cunit = root;
		this.iCompilationUnit = (ICompilationUnit)cunit.getTypeRoot();
		analyzer = new ExtractMethodAnalyzer(enclosingNode);
		importWriter = StubUtility.createImportRewrite(root, true);
		ast = root.getAST();
		bodyDeclaration = (BodyDeclaration)ASTNodes.getParent(enclosingNode, BodyDeclaration.class);
	}


	@Override
	public String getName() {
		return "Extract Method";
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status;
		status = new RefactoringStatus();
		status.merge(super.checkInitialConditions(pm));
		return status;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();
		pm.done();
		return status;
	}

	public String getSignature() {
		MethodDeclaration methodDecl= createNewMethodSignature();
		methodDecl.setBody(null);
		String str= ASTNodes.asString(methodDecl);
		return str.substring(0, str.indexOf(';'));
	}
	
	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		rewrite = ASTRewrite.create(cunit.getAST());
		addNewMethodDeclaration();
		replaceNodeWithMethodInvocation();
		String name = "Extract Method";
		CompilationUnitChange result = new CompilationUnitChange(name, iCompilationUnit);
		result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);
		TextEdit textEdit = rewrite.rewriteAST();
		MultiTextEdit root= new MultiTextEdit();
		result.setEdit(root);
		root.addChild(textEdit);
		if (importWriter.hasRecordedChanges()) {
			TextEdit edit = importWriter.rewriteImports(null);
			root.addChild(edit);
		}
		result.addTextEditGroup(new TextEditGroup("Extract Method", new TextEdit[] {textEdit} ));
		return result;
	}
	

	private void addNewMethodDeclaration() {
		MethodDeclaration md = createNewMethodDeclaration();
		ASTNode typeNode = NodeUtils.getSpecifiedParentNode(enclosingNode, ASTNode.ANONYMOUS_CLASS_DECLARATION);
		ListRewrite list;
		if(typeNode != null)
		{
			list = rewrite.getListRewrite(typeNode, AnonymousClassDeclaration.BODY_DECLARATIONS_PROPERTY);
		} else {
			typeNode = NodeUtils.getSpecifiedParentNode(enclosingNode, ASTNode.TYPE_DECLARATION);
			list = rewrite.getListRewrite(typeNode, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		}
		list.insertAfter(md, bodyDeclaration, null);
	}
	
	private void replaceNodeWithMethodInvocation() {
		rewrite.replace(enclosingNode, createNewMethodInvocation(), null);
	}

	private MethodDeclaration createNewMethodDeclaration() { 
		MethodDeclaration result = createNewMethodSignature();
		result.setBody(createMethodBody());
		return result;
	}

	private MethodDeclaration createNewMethodSignature() {
		MethodDeclaration result = ast.newMethodDeclaration();
		if (modifierType == "public")
			result.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PUBLIC_KEYWORD));			
		else if (modifierType == "protected")
			result.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PROTECTED_KEYWORD));						
		else
			result.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
		int enclosingModifiers= bodyDeclaration.getModifiers();
		if(Modifier.isStatic(enclosingModifiers))
			result.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD));
		
		//The return type must be always void, because we just support method that return void for TEIFB bad smell
		result.setReturnType2(ast.newPrimitiveType(PrimitiveType.VOID));
		result.setName(ast.newSimpleName(getMethodName()));

		//Parameters
		List parameters= result.parameters();
		List fParameterInfos = getParameterInfos();
		for (int i= 0; i < fParameterInfos.size(); i++) {
			ParameterInfo info= (ParameterInfo)fParameterInfos.get(i);
			VariableDeclaration infoDecl= getVariableDeclaration(info);
			SingleVariableDeclaration parameter= ast.newSingleVariableDeclaration();
			parameter.modifiers().addAll(ASTNodeFactory.newModifiers(ast, ASTNodes.getModifiers(infoDecl)));
			parameter.setType(ASTNodeFactory.newType(ast, infoDecl));
			parameter.setName(ast.newSimpleName(info.getNewName()));
			parameter.setVarargs(info.isNewVarargs());
			parameters.add(parameter);
		}
		return result;
	}
	
	private Block createMethodBody() {
		Block result= ast.newBlock();
		ListRewrite statements = rewrite.getListRewrite(result, Block.STATEMENTS_PROPERTY);
		TryStatement tryStatement = createTryStatement();
		statements.insertLast(tryStatement, null);
		return result;
	}
	
	private TryStatement createTryStatement() {
		TryStatement result = ast.newTryStatement();
		
		/* 
		 * Try Block
		 */
		Block block = result.getBody();
		ListRewrite statements = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
		ASTNode copiedNode = ASTNode.copySubtree(ast, enclosingNode);
		Statement newStatement = null;
		newStatement = ast.newExpressionStatement((Expression)copiedNode);
		statements.insertLast(newStatement, null);
		
		/* 
		 * Catches Clauses
		 */
		List<CatchClause> catchClauses = result.catchClauses();
		ITypeBinding[] exceptionTypes = analyzer.getDeclaredExceptions();
		for (int i= 0; i < exceptionTypes.length; i++) {
			ITypeBinding exceptionType= exceptionTypes[i];
			CatchClause cc = ast.newCatchClause();
			SingleVariableDeclaration svdCatch = ast.newSingleVariableDeclaration();
			Type type = importWriter.addImport(exceptionType, ast);
			svdCatch.setType(type);
			svdCatch.setName(ast.newSimpleName("e"));
			cc.setException(svdCatch);
			addCommentLine(ast, cc);
			if(logType.equals("e.printStackTrace();"))
				addPrintStackStatement(ast, cc);
			else
				addJavaLoggerStatement(ast, cc);
			catchClauses.add(cc);
		}

		return result;
	}
	
	private MethodInvocation createNewMethodInvocation() {
		MethodInvocation newMI = ast.newMethodInvocation();
		newMI.setName(ast.newSimpleName(getMethodName()));
		List arguments = newMI.arguments();
		List fParameterInfos = getParameterInfos();
		for (int i= 0; i < fParameterInfos.size(); i++) {
			ParameterInfo parameter= (ParameterInfo)fParameterInfos.get(i);
			arguments.add(ASTNodeFactory.newName(ast, parameter.getOldName()));
		}
		return newMI;
	}
	
	private VariableDeclaration getVariableDeclaration(ParameterInfo parameter) {
		return ASTNodes.findVariableDeclaration(parameter.getOldBinding(), bodyDeclaration);
	}

	private void addPrintStackStatement(AST ast, CatchClause cc) {
		MethodInvocation catchMI = ast.newMethodInvocation();
		catchMI.setName(ast.newSimpleName("printStackTrace"));
		catchMI.setExpression(ast.newSimpleName("e"));			
		ExpressionStatement catchES = ast.newExpressionStatement(catchMI);
		cc.getBody().statements().add(catchES);
	}
	
	private void addCommentLine(AST ast, CatchClause cc) {
		Statement placeHolder = (Statement) rewrite.createStringPlaceholder("// TODO Auto-generated catch block", ASTNode.EMPTY_STATEMENT);
		cc.getBody().statements().add(placeHolder);
	}

	private void addJavaLoggerStatement(AST ast, CatchClause cc) {
		//import java.util.logging.Logger;
		addJavaLoggerLibrary();
		
		//private Logger logger = Logger.getLogger(CarelessCleanupTest.class.getName());
		addLoggerField();
		
		MethodInvocation cbMI = ast.newMethodInvocation();
		cbMI.setName(ast.newSimpleName("warning"));
		cbMI.setExpression(ast.newSimpleName(loggerName));
		MethodInvocation cbarguMI = ast.newMethodInvocation();
		cbarguMI.setName(ast.newSimpleName("getMessage"));
		cbarguMI.setExpression(ast.newSimpleName("e"));
		cbMI.arguments().add(cbarguMI);
		ExpressionStatement catchES = ast.newExpressionStatement(cbMI);
		cc.getBody().statements().add(catchES);
	}
	
	private void addJavaLoggerLibrary() {
		importWriter.addImport("java.util.logging.Logger", null);
	}
	
	private void addLoggerField() {
		TypeDeclaration td = (TypeDeclaration)NodeUtils.getSpecifiedParentNode(enclosingNode, ASTNode.TYPE_DECLARATION);
		
		List<?> bodyList = td.bodyDeclarations();
		for (Object node: bodyList) {
			if (node instanceof FieldDeclaration) {
				FieldDeclaration test = (FieldDeclaration) node;
				for(VariableDeclarationFragment vdf : (List<VariableDeclarationFragment>)test.fragments()) {
					if (vdf.getName().getIdentifier().equals("logger")) {
						if(vdf.resolveBinding().getType().getQualifiedName().equals("java.util.logging.Logger")) {
							return;
						} else {
							loggerName = "javaLogger";
						}
					}
				}
			}
		}
		
		//加入private Logger logger = Logger.getLogger(LoggerTest.class.getName());
		VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		//設定logger
		vdf.setName(ast.newSimpleName("logger"));
		
		//vdf的initializer的Method Invocation
		MethodInvocation initMI = ast.newMethodInvocation();
		//設定initMI的Name
		initMI.setName(ast.newSimpleName("getLogger"));
		//設定initMI的Expression
		initMI.setExpression(ast.newSimpleName("Logger"));

		/* 設定arguMI的Expression */
		MethodInvocation arguMI = ast.newMethodInvocation();
		//設定arguMI的Name
		arguMI.setName(ast.newSimpleName("getName"));

		/* 取得class Name */
		ICompilationUnit icu = iCompilationUnit;
		String javaName = icu.getElementName();
		//濾掉".java"
		String className = javaName.substring(0, javaName.length()-5);
		//設定Expression的Type Literal
		TypeLiteral tl = ast.newTypeLiteral();
		tl.setType(ast.newSimpleType(ast.newName(className)));

		arguMI.setExpression(tl);
		
		//設定initMI的arguments的Method Invocation
		initMI.arguments().add(arguMI);
		vdf.setInitializer(initMI);

		//建立FieldDeclaration
		FieldDeclaration fd = ast.newFieldDeclaration(vdf);
		fd.modifiers().add(ast.newModifier(Modifier.ModifierKeyword.PRIVATE_KEYWORD));
		fd.setType(ast.newSimpleType(ast.newName("Logger")));

		ListRewrite list = rewrite.getListRewrite(td, TypeDeclaration.BODY_DECLARATIONS_PROPERTY);
		list.insertAt(fd, 0, null);
	}
	
	public RefactoringStatus setNewMethodLogType(String logType){
		if (logType.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		} else {
			this.logType = logType;
			return new RefactoringStatus();
		}
	}
	
	public RefactoringStatus setNewMethodModifierType(String modifierType){
		if (modifierType.length() == 0) {
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		} else {
			this.modifierType = modifierType;
			return new RefactoringStatus();
		}
	}

	public RefactoringStatus setNewMethodName(String methodName){
		if (methodName.length() == 0)
			return RefactoringStatus.createFatalErrorStatus("Method Name is empty");
		
		boolean isError = false;
		String methodPattern = "^[_a-zA-Z][_0-9a-zA-Z]*$";
		isError = !methodName.matches(methodPattern);
		
		if (isError){
			return RefactoringStatus.createFatalErrorStatus(methodName + " is not a valid Java identifer");
		}
		else {
			setMethodName(methodName);
		}
		return new RefactoringStatus();
	}
}
