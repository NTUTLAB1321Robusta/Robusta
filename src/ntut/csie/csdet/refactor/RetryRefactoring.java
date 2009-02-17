package ntut.csie.csdet.refactor;

import java.util.List;

import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;

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
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.Document;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

public class RetryRefactoring extends Refactoring{

	//使用者所點選的Marker
	private IMarker marker;
	//使用者所選擇的Exception Type
	private IType exType;
	//retry的變數名稱
	private String retry;
	//attemp的變數名稱
	private String attempt;
	//最大retry次數	
	private String maxNum;
	//最大retry次數的變數名稱
	private String maxAttempt;	
	
	// user 所填寫要丟出的Exception,預設是RunTimeException
	private String exceptionType;
	
	private IJavaProject project;
	
	private IOpenable actOpenable;
	
	private TextFileChange textFileChange;
	
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {	
		//不需check final condition
		collectChange(marker.getResource());
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		//不需check initial condition
		RefactoringStatus status = new RefactoringStatus();		
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		Change[] changes = new Change[] {textFileChange};
		CompositeChange change = new CompositeChange("Introduce resourceful try clause", changes);
		return change;
	}

	private void collectChange(IResource resource){
		//取得要修改的CompilationUnit
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			try {
				IJavaElement javaElement = JavaCore.create(resource);
				if (javaElement instanceof IOpenable) {
					this.actOpenable = (IOpenable) javaElement;
				}
				
				//Create AST to parse
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				
				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				this.actRoot = (CompilationUnit) parser.createAST(null);
				
				//取得該class所有的method
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<ASTNode> methodList = methodCollector.getMethodList();
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				
				//取得目前要被修改的method node
				this.currentMethodNode = methodList.get(Integer.parseInt(methodIdx));
				if(currentMethodNode != null){
					//進行retry refactoring
					introduceRetry();
				}
				
			}catch (Exception ex) {
				ex.printStackTrace();
				//logger.error("[Find CS Method] EXCEPTION ",ex);
			}
		}
	}
	
	/**
	 * 進retry的Refactoring
	 */
	private void introduceRetry(){
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
			MethodDeclaration md = (MethodDeclaration)currentMethodNode;
			//取得Code smell的資訊

			List methodSt = md.getBody().statements();
			//先複製一份method內的statement保留下來
//			List methodCopy = ASTNode.copySubtrees(ast, methodSt);
			int pos = -1;
			TryStatement original = null;
			for(int i=0;i<methodSt.size();i++){
				if(methodSt.get(i) instanceof TryStatement){
					pos = i;
					original = (TryStatement)methodSt.get(i);
				}
			}
			
			Block newBlock = ast.newBlock();
			List newStat = newBlock.statements();
			//假設try之前有程式的話,把他加進新的statement中
			if(pos > 0){
				for(int i=0;i<pos;i++){
					newStat.add(ASTNode.copySubtree(ast, (ASTNode) methodSt.get(i)));
				}
			}
			
			//新增變數
			addNewVariable(ast,newStat);
			//新增do-while
			DoStatement doWhile = addDoWhile(ast,newStat);
			//在do-while新增try
			TryStatement ts =addTryBlock(ast,doWhile,original);
			//在try裡面新增catch
			addCatchBlock(ast, original, ts);
			
			for(int i=pos+1;i<methodSt.size();i++){
				System.out.println("【Copy Content】==>"+methodSt.get(i).toString());
				newStat.add(ASTNode.copySubtree(ast, (ASTNode) methodSt.get(i)));
			}
			//清掉原本的內容
			methodSt.clear();
//			Block block = md.getBody();
//			block.delete();
			//加入refactoring後的結果
			md.setBody(newBlock);
			

			//寫回Edit中
			applyChange();

	}
	
	/**
	 * 建立重試次數的相關變數
	 * @param ast
	 * @param newStat
	 */
	private void addNewVariable(AST ast,List newStat){
		//建立attempt變數
		VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName(this.attempt));
		VariableDeclarationStatement vds = ast.newVariableDeclarationStatement(vdf);
		vdf.setInitializer(ast.newNumberLiteral("0"));		
		newStat.add(vds);
		
		//建立Max Attempt變數
		VariableDeclarationFragment maxAttempt = ast.newVariableDeclarationFragment();
		maxAttempt.setName(ast.newSimpleName(this.maxAttempt));
		VariableDeclarationStatement number = ast.newVariableDeclarationStatement(maxAttempt);
		maxAttempt.setInitializer(ast.newNumberLiteral(maxNum));
		newStat.add(number);
		
		//建立retry變數
		VariableDeclarationFragment retry = ast.newVariableDeclarationFragment();
		retry.setName(ast.newSimpleName(this.retry));
		VariableDeclarationStatement retryValue = ast.newVariableDeclarationStatement(retry);
		//建立boolean型態
		retryValue.setType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));
//		retryValue.setType(ast.newSimpleType(ast.newSimpleName()));
		retry.setInitializer(ast.newBooleanLiteral(false));
		newStat.add(retryValue);
	}
	
	/**
	 * 建立do-while
	 * @param ast
	 * @param newStat
	 * @return
	 */
	private DoStatement addDoWhile(AST ast,List newStat){
		DoStatement doWhile = ast.newDoStatement();
		//先建立attempt <= maxAttempt
		InfixExpression sife = ast.newInfixExpression();
		sife.setLeftOperand(ast.newSimpleName(this.attempt));
		sife.setRightOperand(ast.newSimpleName(this.maxAttempt));
		sife.setOperator(InfixExpression.Operator.LESS_EQUALS);
		InfixExpression bigIfe = ast.newInfixExpression();
		bigIfe.setLeftOperand(sife);
		//建立(retry)
		bigIfe.setRightOperand(ast.newSimpleName(this.retry));
		//建立(attempt<=maxtAttempt && retry)
		bigIfe.setOperator(InfixExpression.Operator.AND);
		doWhile.setExpression(bigIfe);
		newStat.add(doWhile);
		return doWhile;
	}
	
	/**
	 * 在do-while中建立try 
	 * @param ast
	 * @param doWhile
	 * @param original
	 * @return
	 */
	private TryStatement addTryBlock(AST ast,DoStatement doWhile,TryStatement original){
		TryStatement ts = ast.newTryStatement();
		Block block = ts.getBody();
		List tryStatement = block.statements();
		List originalCatch = original.catchClauses();
		
		//建立retry = false
		Assignment as = ast.newAssignment();
		as.setLeftHandSide(ast.newSimpleName(this.retry));
		as.setOperator(Assignment.Operator.ASSIGN);
		as.setRightHandSide(ast.newBooleanLiteral(false));
		ExpressionStatement es =ast.newExpressionStatement(as);
		tryStatement.add(es);
		
		//建立If statement
		IfStatement ifStat = ast.newIfStatement();
		InfixExpression ife = ast.newInfixExpression();
		
		//建立if(....)
		ife.setLeftOperand(ast.newSimpleName(this.attempt));
		ife.setOperator(InfixExpression.Operator.EQUALS);
		ife.setRightOperand(ast.newNumberLiteral("0"));
		ifStat.setExpression(ife);

		//建立then statement
		Block thenBlock = ast.newBlock();
		List thenStat = thenBlock.statements();
		List originalStat = original.getBody().statements();
		ifStat.setThenStatement(thenBlock);
		for(int i=0;i<originalStat.size();i++){
			//將原本try的內容複製進來
			thenStat.add(ASTNode.copySubtree(ast, (ASTNode) originalStat.get(i)));
		}
		
		//建立else statement
		Block elseBlock = ast.newBlock();
		List elseStat = elseBlock.statements();
		ifStat.setElseStatement(elseBlock);
		
		//找出第二層try的位置
		TryStatement secTs = null;
		for(int i=0;i<originalCatch.size();i++){
			CatchClause temp = (CatchClause)originalCatch.get(i);
			List tempSt = temp.getBody().statements();
			for(int x=0;x<tempSt.size();x++){
				if(tempSt.get(x) instanceof TryStatement){
					secTs = (TryStatement)tempSt.get(x);
					break;
				}
			}
		}
		
		//開始複製seconde try statement的內容到else statement
		List secStat = secTs.getBody().statements();
		for(int i=0;i<secStat.size();i++){
			elseStat.add(ASTNode.copySubtree(ast, (ASTNode) secStat.get(i)));
		}

		//將if statement加進try之中
		tryStatement.add(ifStat);
		//替do while新建一個Block,並將Try加進去
		Block doBlock = doWhile.getAST().newBlock();
		doWhile.setBody(doBlock);
		List doStat = doBlock.statements();
		doStat.add(ts);		
		return ts;
	}
	
	/**
	 * 在try中建立catch block
	 * @param ast
	 * @param original
	 * @param ts
	 */
	private void addCatchBlock(AST ast,TryStatement original,TryStatement ts){
		Block block = ts.getBody();
		List catchStatement = ts.catchClauses();
		List originalCatch = original.catchClauses();

		//建立新的catch
		for(int i=0;i<originalCatch.size();i++){
			CatchClause temp = (CatchClause)originalCatch.get(i);
			SingleVariableDeclaration sv = (SingleVariableDeclaration) ASTNode.copySubtree(ast, temp.getException());
			CatchClause cc = ast.newCatchClause();		
			cc.setException(sv);
			catchStatement.add(cc);
		}
		
		//以下開始建立catch Statment中的內容
	
		for(int x=0;x<catchStatement.size();x++){
			//先建立attempt++
			PostfixExpression pfe = ast.newPostfixExpression();
			pfe.setOperand(ast.newSimpleName(this.attempt));
			pfe.setOperator(PostfixExpression.Operator.INCREMENT);
			ExpressionStatement epfe= ast.newExpressionStatement(pfe);
				
			//建立retry = true
			Assignment as = ast.newAssignment();
			as.setLeftHandSide(ast.newSimpleName(this.retry));
			as.setOperator(Assignment.Operator.ASSIGN);
			as.setRightHandSide(ast.newBooleanLiteral(true));
			ExpressionStatement es =ast.newExpressionStatement(as);
				
			//建立if statement,並throw exception
			IfStatement ifStat = ast.newIfStatement();
			InfixExpression ife = ast.newInfixExpression();
			
			//建立if(....)
			ife.setLeftOperand(ast.newSimpleName(this.attempt));
			ife.setOperator(InfixExpression.Operator.GREATER);
			ife.setRightOperand(ast.newSimpleName(this.maxAttempt));
			ifStat.setExpression(ife);
				
			CatchClause cc =(CatchClause)catchStatement.get(x);
			CatchClause temp = (CatchClause)originalCatch.get(x);
	
			//建立then statement
			Block thenBlock = ast.newBlock();
			List thenStat = thenBlock.statements();
			ifStat.setThenStatement(thenBlock);
			//自行建立一個throw statement加入
			ThrowStatement tst = ast.newThrowStatement();
			//將throw的variable傳入
			ClassInstanceCreation cic = ast.newClassInstanceCreation();
			//throw new RuntimeException()<--預設值
			cic.setType(ast.newSimpleType(ast.newSimpleName(this.exceptionType)));
				
			SingleVariableDeclaration svd = (SingleVariableDeclaration) temp
			.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			//將throw new RuntimeException(ex)括號中加入參數 
			cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
			tst.setExpression(cic);
			thenStat.add(tst);
			
			//將剩下的statement加進去catch之中
			List ccStat = cc.getBody().statements();
			ccStat.add(epfe);
			ccStat.add(es);
			ccStat.add(ifStat);

		}
		
		//加入未import的Library(遇到RuntimeException就不用加Library)
		if(!exceptionType.equals("RuntimeException")){
			addImportDeclaration();
			//假如method前面沒有throw東西的話,就加上去
			MethodDeclaration md = (MethodDeclaration)currentMethodNode;
			List thStat = md.thrownExceptions();
			boolean isExist = false;
			for(int i=0;i<thStat.size();i++){
				if(thStat.get(i) instanceof SimpleName){
					SimpleName sn = (SimpleName)thStat.get(i);
					if(sn.getIdentifier().equals(exceptionType)){
						isExist = true;
						break;
					}
				}
			}
			if(!isExist)
				thStat.add(ast.newSimpleName(this.exceptionType));
		}
	}
	
	
	/**
	 * 判斷是否有未加入的Library,但throw RuntimeException的情況要排除
	 * 因為throw RuntimeException不需import Library
	 */
	private void addImportDeclaration(){
		//判斷是否有import library
		boolean isImportLibrary = false;
		List<ImportDeclaration> importList = this.actRoot.imports();
		for(ImportDeclaration id : importList){
			if(exType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName())){
				isImportLibrary = true;
			}
		}
		
		//假如沒有import就加入到AST中
		AST rootAst = this.actRoot.getAST(); 
		if(!isImportLibrary){
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(exType.getFullyQualifiedName()));
			this.actRoot.imports().add(imp);
		}		
	}
	
	
	/**
	 * 將要變更的資料寫回至Document中
	 */
	private void applyChange(){
		//寫回Edit中
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());	
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);

		}catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	@Override
	public String getName() {	
		return "Introduce resourceful try clause";
	}
	
	/**
	 * 把marker傳進來供此class存取一些code smell資訊
	 * @param marker
	 */
	public void setMarker(IMarker marker){
		this.marker = marker;
		this.project = JavaCore.create(marker.getResource().getProject());
	}
	
	/**
	 * 取得JavaProject
	 */
	public IJavaProject getProject(){
		return project;
	}

	/**
	 * 儲存要Throw的Exception位置(要import使用)
	 * @param type
	 */
	public void setExType(IType type){		
		this.exType = type;
	}
	
	/**
	 * set attempt變數名稱
	 * @param attempt
	 */
	public RefactoringStatus setAttemptVariable(String attempt){		
		if(attempt.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.attempt = attempt;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * set maxAttempt變數名稱
	 * @param attempt
	 */
	public RefactoringStatus setMaxAttemptVariable(String attempt){
		if(attempt.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.maxAttempt = attempt;
			return new RefactoringStatus();
		}
	}
	
	/**
	 * set 最大重試次數
	 * @param num
	 */
	public RefactoringStatus setMaxAttemptNum(String num){
		if(num.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.maxNum = num;
			return new RefactoringStatus();
		}		
	}
	
	/**
	 * set Retry變數名稱
	 * @param attempt
	 */
	public RefactoringStatus setRetryVariable(String retry){
		if(retry.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			this.retry = retry;
			return new RefactoringStatus();
		}	
	}
	
	/**
	 * 紀錄user所要throw的exception type
	 * @param name : exception type
	 */
	public RefactoringStatus setExceptionName(String name){
		//假如使用者沒有填寫任何東西,把RefactoringStatus設成Error
		if(name.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			//假如有寫就把他存下來
			this.exceptionType = name;
			return new RefactoringStatus();
		}		
	}
}
