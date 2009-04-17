package ntut.csie.csdet.refactor;

import java.util.List;

import ntut.csie.csdet.visitor.ASTTryCollect;
import ntut.csie.csdet.visitor.SpareHandlerAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;

import agile.exception.RL;
import agile.exception.Robustness;

public class RetryRefactoring extends Refactoring{

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
	
	//存放要轉換成ICompilationUnit的物件
	private IJavaElement element;
	// 目前method的RL Annotation資訊
	private List<RLMessage> currentMethodRLList = null;
	//存放被框選的物件
	private ITextSelection iTSelection;
	
	private TextFileChange textFileChange;
	
	private String RETRY_TYPE = "";
	
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	
	public RetryRefactoring(IJavaProject project,IJavaElement element,ITextSelection sele,String retryType){
		this.project = project;
		this.element = element;
		this.iTSelection = sele;
		this.RETRY_TYPE = retryType;
	}	
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {	
		//不需check final condition
		RefactoringStatus status = new RefactoringStatus();		
		collectChange(status);
		return status;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {
		RefactoringStatus status = new RefactoringStatus();		
		if(iTSelection.getOffset() < 0 || iTSelection.getLength() == 0){
			status.addFatalError("Selection Error, please retry again!!!");
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException,
			OperationCanceledException {
		Change[] changes = new Change[] {textFileChange};
		CompositeChange change = new CompositeChange("Introduce resourceful try clause", changes);
		return change;
	}

	private void collectChange(RefactoringStatus status){
		//Create AST to parse
		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
				
		parser.setSource((ICompilationUnit) element);
		parser.setResolveBindings(true);
		this.actRoot = (CompilationUnit) parser.createAST(null);
				
		//取得該class所有的method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		//利用ITextSelection資訊來取得使用者所選擇要變更的AST Node
		//要使用這個method需在xml檔import org.eclipse.jdt.astview
		ASTNode selectNode = NodeFinder.perform(actRoot, iTSelection.getOffset(), iTSelection.getLength());
		if(selectNode == null){
			status.addFatalError("Selection Error, please retry again!!!");
		}else if(!(selectNode instanceof TryStatement)){
			status.addFatalError("Selection Error, please retry again!!!");
		}else{			
			List<ASTNode> methodList = methodCollector.getMethodList();
			int methodIdx = -1;
			SpareHandlerAnalyzer visitor = null;
			for(ASTNode method : methodList){
				methodIdx++;
				visitor = new SpareHandlerAnalyzer(selectNode,actRoot);
				method.accept(visitor);
				if(visitor.getResult()){				
					break;
				}
			}
			if(!visitor.getResult()){
				status.addFatalError("Selection Error, please retry again!!!");
			}
			
			//取得目前要被修改的method node
			if(methodIdx != -1){
				//取得這個method的RL資訊
				currentMethodNode = methodList.get(methodIdx);
				ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
				currentMethodNode.accept(exVisitor);
				currentMethodRLList = exVisitor.getMethodRLAnnotationList();
				introduceRetry(selectNode);
			}else{
				status.addFatalError("Selection Error, please retry again!!!");
			}
		}

	}
	
	/**
	 * 進retry的Refactoring
	 */
	private void introduceRetry(ASTNode selectNode){
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
			MethodDeclaration md = (MethodDeclaration)currentMethodNode;
			List methodSt = md.getBody().statements();
			//先複製一份method內的statement保留下來
			int pos = -1;
			TryStatement original = null;
			
//			ASTTryCollect visitor = new ASTTryCollect();
//			md.accept(visitor);
//			List<ASTNode> tryList = visitor.getTryList();
//			for(int i=0; i<tryList.size() ; i++){
//				if(tryList.get(i) instanceof TryStatement){					
//					TryStatement temp = (TryStatement)tryList.get(i);
//					if(temp.getStartPosition() == selectNode.getStartPosition()){
//						System.out.println("【Find Try Statement】");
//						pos = i;
//						original = (TryStatement)tryList.get(i);
//						break;
//					}
//				}
//			}
			for(int i=0;i<methodSt.size();i++){
				if(methodSt.get(i) instanceof TryStatement){
					TryStatement temp = (TryStatement)methodSt.get(i);
					if(temp.getStartPosition() == selectNode.getStartPosition()){
						System.out.println("【Find Try Statement】");
						pos = i;
						original = (TryStatement)methodSt.get(i);
						break;
					}
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
			TryStatement ts = null;
			if(RETRY_TYPE.equals("Alt_Retry")){
				ts = addTryBlock(ast,doWhile,original);
			}else if(RETRY_TYPE.equals("No_Alt_Retry")){
				if(original == null){
					System.out.println("【Original Try Block is null】");
				}
				ts = addNoAltTryBlock(ast, doWhile, original);
			}else{
				//exception
			}
			
			//在try裡面新增catch
			addCatchBlock(ast, original, ts);
			//假如catch之後有finally,就加進去
			if(original.getFinally() != null){
				ts.setFinally((Block) ASTNode.copySubtree(ast, original.getFinally() ));
			}
			
			for(int i=pos+1;i<methodSt.size();i++){
				newStat.add(ASTNode.copySubtree(ast, (ASTNode) methodSt.get(i)));
			}
			//清掉原本的內容
			methodSt.clear();
			//加入refactoring後的結果
			md.setBody(newBlock);
			//建立RL Annotation
			addAnnotationRoot(ast);
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
		if(RETRY_TYPE.equals("Alt_Retry")){
			retry.setInitializer(ast.newBooleanLiteral(false));			
		}else if(RETRY_TYPE.equals("No_Alt_Retry")){
			retry.setInitializer(ast.newBooleanLiteral(true));
		}
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
	 * 假如是用No alternative Retry refactoring,則用這個method來修改try block
	 * @param ast
	 * @param doWhile
	 * @param original
	 * @return
	 */
	private TryStatement addNoAltTryBlock(AST ast,DoStatement doWhile,TryStatement original){
		TryStatement ts = ast.newTryStatement();
		Block block = ts.getBody();
		List tryStatement = block.statements();
		List originalStat = original.getBody().statements();
		for(int i=0;i<originalStat.size();i++){
			//將原本try的內容複製進來
			tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) originalStat.get(i)));
		}
		
		//建立retry = true
		Assignment as = ast.newAssignment();
		as.setLeftHandSide(ast.newSimpleName(this.retry));
		as.setOperator(Assignment.Operator.ASSIGN);
		as.setRightHandSide(ast.newBooleanLiteral(false));
		ExpressionStatement es =ast.newExpressionStatement(as);
		tryStatement.add(es);
		
		//替do while新建一個Block,並將Try加進去
		Block doBlock = doWhile.getAST().newBlock();
		doWhile.setBody(doBlock);
		List doStat = doBlock.statements();
		doStat.add(ts);		
		return ts;
		
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
		List originalCatch = original.catchClauses();
		boolean isTryExist = false;
		TryStatement secTs = null;
		for(int i=0;i<originalCatch.size();i++){
			CatchClause temp = (CatchClause)originalCatch.get(i);
			List tempSt = temp.getBody().statements();
			if(tempSt != null){
				for(int x=0;x<tempSt.size();x++){
					if(tempSt.get(x) instanceof TryStatement){
						secTs = (TryStatement)tempSt.get(x);
						isTryExist = true;
						break;
					}
				}	
			}
			
		}
		if(isTryExist){
			//開始複製second try statement的內容到else statement
			List secStat = secTs.getBody().statements();
			for(int i=0;i<secStat.size();i++){
				elseStat.add(ASTNode.copySubtree(ast, (ASTNode) secStat.get(i)));
			}	
		}else{
			//若catch statement中並沒有第二個try,則把catch中的結果當作alternative
			if(originalCatch != null){
				//將catch的內容copy進去
				CatchClause temp = (CatchClause)originalCatch.get(0);
				List tempSt = temp.getBody().statements();
				if(tempSt != null){
					for(int x=0;x<tempSt.size();x++){
						elseStat.add(ASTNode.copySubtree(ast, (ASTNode) tempSt.get(x)));
					}	
				}
			}
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
	
	private void addFinallyBlock(){
		
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
	
	private void addAnnotationRoot(AST ast){
		//要建立@Robustness(value={@RL(level=3, exception=java.lang.RuntimeException.class)})這樣的Annotation
		//建立Annotation root
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));

		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);
		
		MethodDeclaration method = (MethodDeclaration) currentMethodNode;		
		if(currentMethodRLList.size() == 0){		
			//Retry的RL = 3
			rlary.expressions().add(getRLAnnotation(ast,3,exceptionType));
		}else{	
			//假如本來就有annotation先把舊的加進去
			for (RLMessage rlmsg : currentMethodRLList) {
				
				int pos = rlmsg.getRLData().getExceptionType().toString().lastIndexOf(".");
				String cut = rlmsg.getRLData().getExceptionType().toString().substring(pos+1);

				//如果有有RL annotation重複就不加進去
				if((!cut.equals(exceptionType)) && (rlmsg.getRLData().getLevel() != 3)){										
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));					
				}
			}
			//舊的加完之後加新的RL = 3 annotation進來
			rlary.expressions().add(getRLAnnotation(ast,3,exceptionType));
			
			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//找到舊有的annotation後將它移除
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}
		}
		if (rlary.expressions().size() > 0) {
			method.modifiers().add(0, root);
		}
		//將RL的library加進來
		addImportRLDeclaration();
	}
	
	/**
	 * 產生RL Annotation之RL資料 
	 * @param ast :AST Object
	 * @param levelVal :強健度等級
	 * @param exClass : 例外類別
	 * @return NormalAnnotation AST Node
	 */

	@SuppressWarnings("unchecked")
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal,String excption) {
		//要建立@Robustness(value={@RL(level=1, exception=java.lang.RuntimeException.class)})這樣的Annotation
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("RL"));

		// level = 3
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName("level"));
		//Retry 預設level = 3
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		rl.values().add(level);

		//exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		//預設為RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);

		return rl;
	}
	
	@SuppressWarnings("unchecked")
	private void addImportRLDeclaration() {
		// 判斷是否已經Import Robustness及RL的宣告
		List<ImportDeclaration> importList = this.actRoot.imports();
		boolean isImportRobustnessClass = false;
		boolean isImportRLClass = false;
		for (ImportDeclaration id : importList) {
			if (RLData.CLASS_ROBUSTNESS.equals(id.getName().getFullyQualifiedName())) {
				isImportRobustnessClass = true;
			}
			if (RLData.CLASS_RL.equals(id.getName().getFullyQualifiedName())) {
				isImportRLClass = true;
			}
		}

		AST rootAst = this.actRoot.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			this.actRoot.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RL.class.getName()));
			this.actRoot.imports().add(imp);
		}
	}
	
	/**
	 * 將要變更的資料寫回至Document中
	 */
	private void applyChange(){
		//寫回Edit中
		try {
			ICompilationUnit cu = (ICompilationUnit) element;
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
