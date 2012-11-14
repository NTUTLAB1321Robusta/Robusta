package ntut.csie.csdet.refactor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.SpareHandlerVisitor;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

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
import org.eclipse.jdt.core.dom.ITypeBinding;
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
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jdt.core.refactoring.CompilationUnitChange;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RetryRefactoring extends Refactoring {
	private static Logger logger = LoggerFactory.getLogger(RetryRefactoring.class);
	
	//使用者所選擇的Exception Type
	private IType exType;
	//retry的變數名稱
	private String retry;
	//attempt的變數名稱
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
	private MethodDeclaration currentMethodNode = null;
	
	private ASTRewrite rewrite;
	
	private SmellSettings smellSettings;
	
	public RetryRefactoring(IJavaProject project,IJavaElement element,ITextSelection sele,String retryType){
		this.project = project;
		this.element = element;
		this.iTSelection = sele;
		this.RETRY_TYPE = retryType;
		smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
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
		System.out.println(iTSelection.getOffset() );
		System.out.println(iTSelection.getLength() );
		if(iTSelection.getOffset() < 0 || iTSelection.getLength() == 0){
			status.addFatalError("Selection Error, please retry again!!!");
		}
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm)
								throws CoreException, OperationCanceledException {
		/* 2010.07.20 之前的寫法，Preview的Token不會變色
		 * Change[] changes = new Change[] {textFileChange};
		 * CompositeChange change = new CompositeChange("Introduce resourceful try clause", changes);
		 */
		String name = "Introduce resourceful try clause";
		ICompilationUnit unit = (ICompilationUnit) element;
		CompilationUnitChange result = new CompilationUnitChange(name, unit);
		result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

		// 將修改結果設置在CompilationUnitChange
		TextEdit edits = textFileChange.getEdit();
		result.setEdit(edits);
		// 將修改結果設成Group，會顯示在Preview上方節點。
		result.addTextEditGroup(new TextEditGroup("Introduce resourceful try clause", 
								new TextEdit[] {edits} ));

		return result;
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
		NodeFinder nodeFinder = new NodeFinder(iTSelection.getOffset(), iTSelection.getLength());
		actRoot.accept(nodeFinder);
		ASTNode selectNode = nodeFinder.getCoveredNode();
		if (selectNode == null) {
			status.addFatalError("Selection Error, please retry again!!!");
		} else if(selectNode.getNodeType() != ASTNode.TRY_STATEMENT) {
			status.addFatalError("Selection Error, please retry again!!!");
		} else {
			//取得class中所有的method
			List<MethodDeclaration> methodList = methodCollector.getMethodList();
			int methodIdx = -1;
			SpareHandlerVisitor visitor = null;
			for(MethodDeclaration method : methodList){
				methodIdx++;
				visitor = new SpareHandlerVisitor(selectNode);
				method.accept(visitor);
				//檢查是否有框選到try statement
				if(visitor.getResult()){
					break;
				}
			}
			
			//假如框選錯誤就不允許重構
			if(!visitor.getResult()){
				status.addFatalError("Selection Error, please retry again!!!");
			}
			
			//取得目前要被修改的method node
			if (methodIdx != -1) {
				//取得這個method的RL資訊
				currentMethodNode = methodList.get(methodIdx);
				ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
				currentMethodNode.accept(exVisitor);
				currentMethodRLList = exVisitor.getMethodRLAnnotationList();
				//進行Retry Refactoring
				introduceTryClause(selectNode);
			} else {
				status.addFatalError("Selection Error, please retry again!!!");
			}
		}
	}

	/**
	 * 在do-while中加入try clause
	 * @param selectNode
	 */
	private void introduceTryClause(ASTNode selectNode){
		AST ast = actRoot.getAST();
		rewrite = ASTRewrite.create(actRoot.getAST());

		//去取得selectNode的parent節點,並尋找selectNode在parent block中的位置
		ListRewrite parentRewrite = rewrite.getListRewrite(selectNode.getParent(),Block.STATEMENTS_PROPERTY );
		//利用此變數來紀錄Try Statement位置
		int replacePos = -1;
		TryStatement original = null;
		for (int i=0;i<parentRewrite.getRewrittenList().size();i++) {
			if (parentRewrite.getRewrittenList().get(i).equals(selectNode)) {
				original = (TryStatement)parentRewrite.getRewrittenList().get(i);
				//找到Try Statement就把他的位置記錄下來
				replacePos = i;
			}
		}		

		//假如有找到的話,就開始進行重構
		if(replacePos >= 0){
			//新增變數
			addNewVariable(ast,parentRewrite,replacePos);
			//新增do-while
			DoStatement doWhile = addDoWhile(ast);
			parentRewrite.replace(original,doWhile,null);
			//在do-while新增try
			TryStatement ts = null;
			//利用傳入參數來判斷是哪一種Retry
			if (RETRY_TYPE.equals("Alt_Retry")) {
				ts = addTryClause(ast, doWhile, original);
			} else if(RETRY_TYPE.equals("Retry_with_original")) {
				ts = addNoAltTryBlock(ast, doWhile, original);
			} else {
				System.out.println("【No Retry Action!!!!Exception Occur!!!】");
			}
		
			//在第一層的try新增catch
			addCatchBlock(ast, original, ts);
			//假如catch之後有finally,就加進去
			if(original.getFinally() != null){
				ts.setFinally((Block) ASTNode.copySubtree(ast, original.getFinally()));
			}
			//建立RL Annotation
			if(smellSettings.isAddingRobustnessAnnotation()) {
				addAnnotationRoot(ast);
			}
			//寫回Edit中
			applyChange();
		}

	}
	
	/**
	 * 建立重試次數的相關變數
	 * @param ast
	 * @param newStat
	 */
	private void addNewVariable(AST ast,ListRewrite parentRewrite,int replacePos){
		//建立attempt變數
		VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName(this.attempt));
		vdf.setInitializer(ast.newNumberLiteral("0"));
		VariableDeclarationStatement vds = ast.newVariableDeclarationStatement(vdf);
		parentRewrite.insertAt(vds, replacePos, null);

		//建立Max Attempt變數
		VariableDeclarationFragment maxAttempt = ast.newVariableDeclarationFragment();
		maxAttempt.setName(ast.newSimpleName(this.maxAttempt));
		maxAttempt.setInitializer(ast.newNumberLiteral(maxNum));
		VariableDeclarationStatement number = ast.newVariableDeclarationStatement(maxAttempt);
		parentRewrite.insertAt(number, replacePos+1, null);
		
		//建立retry變數
		VariableDeclarationFragment retry = ast.newVariableDeclarationFragment();
		retry.setName(ast.newSimpleName(this.retry));
		VariableDeclarationStatement retryValue = ast.newVariableDeclarationStatement(retry);
		//建立boolean型態
		retryValue.setType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));
		
		if(RETRY_TYPE.equals("Alt_Retry")){
			retry.setInitializer(ast.newBooleanLiteral(false));			
		}else if(RETRY_TYPE.equals("Retry_with_original")){
			retry.setInitializer(ast.newBooleanLiteral(true));
		}
		parentRewrite.insertAt(retryValue, replacePos+2, null);
	}
	
	/**
	 * 建立do-while
	 * @param ast
	 * @param newStat
	 * @return
	 */
	private DoStatement addDoWhile(AST ast){
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
//		newStat.add(doWhile);
		return doWhile;
	}
	
	/**
	 * 在do-while中建立try 
	 * @param ast
	 * @param doWhile
	 * @param original
	 * @return
	 */
	private TryStatement addTryClause(AST ast,DoStatement doWhile,TryStatement original){
		TryStatement ts = ast.newTryStatement();
		Block block = ts.getBody();
		List<Statement> tryStatement = block.statements();
		
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
//		List<Statement> thenStat = thenBlock.statements();
		List<?> originalStat = original.getBody().statements();
		ifStat.setThenStatement(thenBlock);

		for(int i=0;i<originalStat.size();i++){
			//將原本try的內容複製進來
			//thenStat.add(ASTNode.copySubtree(ast, (ASTNode) originalStat.get(i)));

			//用移動的方式，才不會刪除註解
			ASTNode placeHolder = rewrite.createMoveTarget((ASTNode) originalStat.get(i));
			ListRewrite moveRewrite = rewrite.getListRewrite(thenBlock, Block.STATEMENTS_PROPERTY);
			moveRewrite.insertLast(placeHolder, null);
		}
		
		//建立else statement
		Block elseBlock = ast.newBlock();
//		List<Statement> elseStat = elseBlock.statements();
		ifStat.setElseStatement(elseBlock);

		//找出第二層try的位置
		List<?> originalCatch = original.catchClauses();
		boolean isTryExist = false;
		TryStatement secTs = null;
		for (int i =0; i < originalCatch.size(); i++) {
			CatchClause temp = (CatchClause) originalCatch.get(i);
			List<?> tempSt = temp.getBody().statements();
			if(tempSt != null){
				for (int x =0; x < tempSt.size(); x++) {
					if (tempSt.get(x) instanceof TryStatement) {
						secTs = (TryStatement)tempSt.get(x);
						isTryExist = true;
						break;
					}
				}	
			}			
		}
		
		if(isTryExist){
			//開始複製second try statement的內容到else statement
			List<?> secStat = secTs.getBody().statements();
			for (int i =0;i < secStat.size(); i++) {
				//elseStat.add(ASTNode.copySubtree(ast, (ASTNode) secStat.get(i)));
				ASTNode placeHolder = rewrite.createMoveTarget((ASTNode) secStat.get(i));
				ListRewrite moveRewrite = rewrite.getListRewrite(elseBlock, Block.STATEMENTS_PROPERTY);
				moveRewrite.insertLast(placeHolder, null);
			}
		} else {
			//若catch statement中並沒有第二個try,則把catch中的結果當作alternative
			if(originalCatch != null){
				//將catch的內容copy進去
				CatchClause temp = (CatchClause)originalCatch.get(0);
				List<?> tempSt = temp.getBody().statements();
				if (tempSt != null) {
					for (int x =0; x < tempSt.size(); x++) {
						//elseStat.add(ASTNode.copySubtree(ast, (ASTNode) tempSt.get(x)));
						ASTNode placeHolder = rewrite.createMoveTarget((ASTNode) tempSt.get(x));
						ListRewrite moveRewrite = rewrite.getListRewrite(elseBlock, Block.STATEMENTS_PROPERTY);
						moveRewrite.insertLast(placeHolder, null);
					}
				}
			}
		}
		
		//將if statement加進try之中
		tryStatement.add(ifStat);
		//替do while新建一個Block,並將Try加進去
		Block doBlock = doWhile.getAST().newBlock();
		doWhile.setBody(doBlock);
		List<Statement> doStat = doBlock.statements();
		doStat.add(ts);		
		return ts;
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
		List<Statement> tryStatement = block.statements();
		List<?> originalStat = original.getBody().statements();
		
		for(int i=0;i<originalStat.size();i++){
			//將原本try的內容複製進來
			//tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) originalStat.get(i)));

			//用移動的方式，才不會刪除註解
			ASTNode placeHolder = rewrite.createMoveTarget((ASTNode) originalStat.get(i));
			ListRewrite moveRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
			moveRewrite.insertLast(placeHolder, null);
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
		List<Statement> doStat = doBlock.statements();
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
		List<CatchClause> catchStatement = ts.catchClauses();
		List<?> originalCatch = original.catchClauses();
	
		//建立新的catch
		for(int i=0;i<originalCatch.size();i++){
			//第一層的catch,但可能有捕捉不只一種型態的例外,所以需要用迴圈
			CatchClause temp = (CatchClause)originalCatch.get(i);
			List<?> catchSt = temp.getBody().statements();
			//取得catch()括號中的例外型態
			SingleVariableDeclaration svd = (SingleVariableDeclaration)temp.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			SingleVariableDeclaration sv = (SingleVariableDeclaration) ASTNode.copySubtree(ast, temp.getException());
			//利用此變數來記錄新建立的catch所要捕捉的例外型態
			String newExType = null;
			if(catchSt != null){
				//假如第一層catch區塊內不是空的,就去找try是否存在
				for(int x=0; x<catchSt.size(); x++){
					if(catchSt.get(x) instanceof TryStatement){
						//尋找同質性的例外
						newExType = findHomogeneousExType(ast,svd,catchSt.get(x));
					}
				}	
			}
			//假如沒有找到,表示可能第二層區塊沒有try-catch block,只需直接複製之前的捕捉的例外型態就可
			if(newExType != null){				
				sv.setType(ast.newSimpleType(ast.newSimpleName(newExType)));
			}
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
			List<Statement> thenStat = thenBlock.statements();
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
			List<Statement> ccStat = cc.getBody().statements();
			ccStat.add(epfe);
			ccStat.add(es);
			ccStat.add(ifStat);
		}
		
		//加入未import的Library(遇到RuntimeException就不用加Library)
		if(!exceptionType.equals("RuntimeException")){
			addImportDeclaration();
			//假如method前面沒有throw東西的話,就加上去
			MethodDeclaration md = currentMethodNode;
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
	 * 我們假設source code中，如果catch clause裡面還有try-catch的例外處理，
	 * 就假設符合spare-handler的bad smell。因此，我們要找出catch clause的裡外，
	 * 和catch clause裡面try-catch的catch clause的例外可以用哪個exception的類別來包含兩者，
	 * 使得兩層try-catch可以合併為一個try-catch。20120907補註解，Charles。
	 * 
	 * 當碰到第一層與第二層捕捉的例外不同時,尋找同質性例外
	 * @param type : 第一層的例外型態變數
	 * @param node : 第二層的try statement
	 */
	private String findHomogeneousExType(AST ast, SingleVariableDeclaration type, Object node) {	
		TryStatement ts = (TryStatement)node;
		//找第二層try block中所有的catch區塊
		List<?> catchSt = ts.catchClauses();		
		if(catchSt != null){
			//先尋找第一層try-catch的例外型態,並往上trace紀錄到List中
			ArrayList<String> exList = new ArrayList<String>();
			ITypeBinding iTB = type.resolveBinding().getType();
			//將java.lang.Exception當作最上層例外
			String topLevelEx = "Exception";
			//當找到例外就存起來,若找到最上層則跳出迴圈
			while(!(iTB.getName().equals(topLevelEx)) ){
				exList.add(iTB.getName());	
				iTB = iTB.getSuperclass();
			}
			exList.add(topLevelEx);
			
			//利用此List來紀錄
			ArrayList<String> secList = new ArrayList<String>();
			for(int i = 0; i< catchSt.size(); i++){
				CatchClause cc = (CatchClause)catchSt.get(i);
				//取得第二層例外的變數型態
				SingleVariableDeclaration svd = (SingleVariableDeclaration)cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
				//TODO :多層catch時的同質性例外之後要想怎麼取
				iTB = svd.resolveBinding().getType();
				//從底層的例外型態往上找,找到最上層後紀錄下來
				while(!(iTB.getName().equals(topLevelEx)) ){
					secList.add(iTB.getName());	
					iTB = iTB.getSuperclass();
				}
				secList.add(topLevelEx);
				break;
			}
			
			//兩個List進行比對,尋找兩個例外的共同父類別
			if(secList != null){		
				for(String ex : secList){
					for(String exType : exList){
						if(ex.equals(exType)){
							//找到共同父類別就回傳
							return ex;
						}							
					}
				}
			}			
		}
		return type.getType().toString();
	}
		
	/**
	 * 判斷是否有未加入的Library,但throw RuntimeException的情況要排除
	 * 因為throw RuntimeException不需import Library
	 */
	private void addImportDeclaration(){
		//判斷是否有import library
		ListRewrite listRewrite = rewrite.getListRewrite(this.actRoot,  CompilationUnit.IMPORTS_PROPERTY);
		List<ImportDeclaration> importList = listRewrite.getRewrittenList();
		boolean isImportLibrary = false;
		for(ImportDeclaration id : importList){
			if(exType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName())){
				isImportLibrary = true;
				break;
			}
		}
		
		//假如沒有import就加入到AST中
		AST rootAst = this.actRoot.getAST(); 
		if(!isImportLibrary){
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(exType.getFullyQualifiedName()));
			listRewrite.insertLast(imp, null);
		}		
	}
	
	private void addAnnotationRoot(AST ast){
		//要建立@Robustness(value={@Tag(level=3, exception=java.lang.RuntimeException.class)})這樣的Annotation
		//建立Annotation root
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));

		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);
		
		MethodDeclaration method = currentMethodNode;		
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
			
			ListRewrite listRewrite = rewrite.getListRewrite(method, method.getModifiersProperty());
			List<IExtendedModifier> modifiers = listRewrite.getRewrittenList();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//找到舊有的annotation後將它移除
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					listRewrite.remove((ASTNode)modifiers.get(i), null);
					break;
				}
			}
		}
		
		if (rlary.expressions().size() > 0) {
			ListRewrite listRewrite = rewrite.getListRewrite(method, method.getModifiersProperty());
			listRewrite.insertAt(root, 0, null);
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
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal,String excption) {
		//要建立@Robustness(value={@Tag(level=1, exception=java.lang.RuntimeException.class)})這樣的Annotation
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName(RTag.class.getSimpleName().toString()));

		// level = 3
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName(RTag.LEVEL));
		//Retry 預設level = 3
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		rl.values().add(level);

		//exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName(RTag.EXCEPTION));
		TypeLiteral exclass = ast.newTypeLiteral();
		//預設為RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);
		return rl;
	}

	private void addImportRLDeclaration() {
		// 判斷是否已經Import Robustness及RL的宣告
		ListRewrite listRewrite = rewrite.getListRewrite(this.actRoot,  CompilationUnit.IMPORTS_PROPERTY);
		List<ImportDeclaration> importList = listRewrite.getRewrittenList();
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
			listRewrite.insertLast(imp, null);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RTag.class.getName()));
			listRewrite.insertLast(imp, null);
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
			TextEdit edits = rewrite.rewriteAST(document,null);
			textFileChange = new TextFileChange(cu.getElementName(), (IFile)cu.getResource());
			textFileChange.setEdit(edits);
		}catch (Exception ex) {
			ex.printStackTrace();
			logger.error("[Rewrite to Edit] EXCEPTION ",ex);
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
