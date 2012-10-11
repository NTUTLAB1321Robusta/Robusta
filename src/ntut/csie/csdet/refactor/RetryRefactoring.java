package ntut.csie.csdet.refactor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.visitor.SpareHandlerVisitor;
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
	
	//�ϥΪ̩ҿ�ܪ�Exception Type
	private IType exType;
	//retry���ܼƦW��
	private String retry;
	//attempt���ܼƦW��
	private String attempt;
	//�̤jretry����	
	private String maxNum;
	//�̤jretry���ƪ��ܼƦW��
	private String maxAttempt;		
	// user �Ҷ�g�n��X��Exception,�w�]�ORunTimeException
	private String exceptionType;
	
	private IJavaProject project;
	
	//�s��n�ഫ��ICompilationUnit������
	private IJavaElement element;
	// �ثemethod��RL Annotation��T
	private List<RLMessage> currentMethodRLList = null;
	//�s��Q�ؿ諸����
	private ITextSelection iTSelection;
	
	private TextFileChange textFileChange;
	
	private String RETRY_TYPE = "";
	
	//�s��ثe�n�ק諸.java��
	private CompilationUnit actRoot;
	
	//�s��ثe�ҭnfix��method node
	private MethodDeclaration currentMethodNode = null;
	
	private ASTRewrite rewrite;
	
	public RetryRefactoring(IJavaProject project,IJavaElement element,ITextSelection sele,String retryType){
		this.project = project;
		this.element = element;
		this.iTSelection = sele;
		this.RETRY_TYPE = retryType;
	}	
	
	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm)
			throws CoreException, OperationCanceledException {	
		//����check final condition
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
		/* 2010.07.20 ���e���g�k�APreview��Token���|�ܦ�
		 * Change[] changes = new Change[] {textFileChange};
		 * CompositeChange change = new CompositeChange("Introduce resourceful try clause", changes);
		 */
		String name = "Introduce resourceful try clause";
		ICompilationUnit unit = (ICompilationUnit) element;
		CompilationUnitChange result = new CompilationUnitChange(name, unit);
		result.setSaveMode(TextFileChange.KEEP_SAVE_STATE);

		// �N�קﵲ�G�]�m�bCompilationUnitChange
		TextEdit edits = textFileChange.getEdit();
		result.setEdit(edits);
		// �N�קﵲ�G�]��Group�A�|��ܦbPreview�W��`�I�C
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
				
		//���o��class�Ҧ���method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		//�Q��ITextSelection��T�Ө��o�ϥΪ̩ҿ�ܭn�ܧ�AST Node
		//�n�ϥγo��method�ݦbxml��import org.eclipse.jdt.astview
		NodeFinder nodeFinder = new NodeFinder(iTSelection.getOffset(), iTSelection.getLength());
		actRoot.accept(nodeFinder);
		ASTNode selectNode = nodeFinder.getCoveredNode();
		if (selectNode == null) {
			status.addFatalError("Selection Error, please retry again!!!");
		} else if(selectNode.getNodeType() != ASTNode.TRY_STATEMENT) {
			status.addFatalError("Selection Error, please retry again!!!");
		} else {
			//���oclass���Ҧ���method
			List<MethodDeclaration> methodList = methodCollector.getMethodList();
			int methodIdx = -1;
			SpareHandlerVisitor visitor = null;
			for(MethodDeclaration method : methodList){
				methodIdx++;
				visitor = new SpareHandlerVisitor(selectNode);
				method.accept(visitor);
				//�ˬd�O�_���ؿ��try statement
				if(visitor.getResult()){
					break;
				}
			}
			
			//���p�ؿ���~�N�����\���c
			if(!visitor.getResult()){
				status.addFatalError("Selection Error, please retry again!!!");
			}
			
			//���o�ثe�n�Q�ק諸method node
			if (methodIdx != -1) {
				//���o�o��method��RL��T
				currentMethodNode = methodList.get(methodIdx);
				ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
				currentMethodNode.accept(exVisitor);
				currentMethodRLList = exVisitor.getMethodRLAnnotationList();
				//�i��Retry Refactoring
				introduceTryClause(selectNode);
			} else {
				status.addFatalError("Selection Error, please retry again!!!");
			}
		}
	}

	/**
	 * �bdo-while���[�Jtry clause
	 * @param selectNode
	 */
	private void introduceTryClause(ASTNode selectNode){
		AST ast = actRoot.getAST();
		rewrite = ASTRewrite.create(actRoot.getAST());

		//�h���oselectNode��parent�`�I,�ôM��selectNode�bparent block������m
		ListRewrite parentRewrite = rewrite.getListRewrite(selectNode.getParent(),Block.STATEMENTS_PROPERTY );
		//�Q�Φ��ܼƨӬ���Try Statement��m
		int replacePos = -1;
		TryStatement original = null;
		for (int i=0;i<parentRewrite.getRewrittenList().size();i++) {
			if (parentRewrite.getRewrittenList().get(i).equals(selectNode)) {
				original = (TryStatement)parentRewrite.getRewrittenList().get(i);
				//���Try Statement�N��L����m�O���U��
				replacePos = i;
			}
		}		

		//���p����쪺��,�N�}�l�i�歫�c
		if(replacePos >= 0){
			//�s�W�ܼ�
			addNewVariable(ast,parentRewrite,replacePos);
			//�s�Wdo-while
			DoStatement doWhile = addDoWhile(ast);
			parentRewrite.replace(original,doWhile,null);
			//�bdo-while�s�Wtry
			TryStatement ts = null;
			//�Q�ζǤJ�ѼƨӧP�_�O���@��Retry
			if (RETRY_TYPE.equals("Alt_Retry")) {
				ts = addTryClause(ast, doWhile, original);
			} else if(RETRY_TYPE.equals("Retry_with_original")) {
				ts = addNoAltTryBlock(ast, doWhile, original);
			} else {
				System.out.println("�iNo Retry Action!!!!Exception Occur!!!�j");
			}
		
			//�b�Ĥ@�h��try�s�Wcatch
			addCatchBlock(ast, original, ts);
			//���pcatch���ᦳfinally,�N�[�i�h
			if(original.getFinally() != null){
				ts.setFinally((Block) ASTNode.copySubtree(ast, original.getFinally()));
			}
			//�إ�RL Annotation
			addAnnotationRoot(ast);
			//�g�^Edit��
			applyChange();
		}

	}
	
	/**
	 * �إ߭��զ��ƪ������ܼ�
	 * @param ast
	 * @param newStat
	 */
	private void addNewVariable(AST ast,ListRewrite parentRewrite,int replacePos){
		//�إ�attempt�ܼ�
		VariableDeclarationFragment vdf = ast.newVariableDeclarationFragment();
		vdf.setName(ast.newSimpleName(this.attempt));
		vdf.setInitializer(ast.newNumberLiteral("0"));
		VariableDeclarationStatement vds = ast.newVariableDeclarationStatement(vdf);
		parentRewrite.insertAt(vds, replacePos, null);

		//�إ�Max Attempt�ܼ�
		VariableDeclarationFragment maxAttempt = ast.newVariableDeclarationFragment();
		maxAttempt.setName(ast.newSimpleName(this.maxAttempt));
		maxAttempt.setInitializer(ast.newNumberLiteral(maxNum));
		VariableDeclarationStatement number = ast.newVariableDeclarationStatement(maxAttempt);
		parentRewrite.insertAt(number, replacePos+1, null);
		
		//�إ�retry�ܼ�
		VariableDeclarationFragment retry = ast.newVariableDeclarationFragment();
		retry.setName(ast.newSimpleName(this.retry));
		VariableDeclarationStatement retryValue = ast.newVariableDeclarationStatement(retry);
		//�إ�boolean���A
		retryValue.setType(ast.newPrimitiveType(PrimitiveType.BOOLEAN));
		
		if(RETRY_TYPE.equals("Alt_Retry")){
			retry.setInitializer(ast.newBooleanLiteral(false));			
		}else if(RETRY_TYPE.equals("Retry_with_original")){
			retry.setInitializer(ast.newBooleanLiteral(true));
		}
		parentRewrite.insertAt(retryValue, replacePos+2, null);
	}
	
	/**
	 * �إ�do-while
	 * @param ast
	 * @param newStat
	 * @return
	 */
	private DoStatement addDoWhile(AST ast){
		DoStatement doWhile = ast.newDoStatement();
		//���إ�attempt <= maxAttempt
		InfixExpression sife = ast.newInfixExpression();
		sife.setLeftOperand(ast.newSimpleName(this.attempt));
		sife.setRightOperand(ast.newSimpleName(this.maxAttempt));
		sife.setOperator(InfixExpression.Operator.LESS_EQUALS);
		InfixExpression bigIfe = ast.newInfixExpression();
		bigIfe.setLeftOperand(sife);
		//�إ�(retry)
		bigIfe.setRightOperand(ast.newSimpleName(this.retry));
		//�إ�(attempt<=maxtAttempt && retry)
		bigIfe.setOperator(InfixExpression.Operator.AND);
		doWhile.setExpression(bigIfe);
//		newStat.add(doWhile);
		return doWhile;
	}
	
	/**
	 * �bdo-while���إ�try 
	 * @param ast
	 * @param doWhile
	 * @param original
	 * @return
	 */
	private TryStatement addTryClause(AST ast,DoStatement doWhile,TryStatement original){
		TryStatement ts = ast.newTryStatement();
		Block block = ts.getBody();
		List<Statement> tryStatement = block.statements();
		
		//�إ�retry = false
		Assignment as = ast.newAssignment();
		as.setLeftHandSide(ast.newSimpleName(this.retry));
		as.setOperator(Assignment.Operator.ASSIGN);
		as.setRightHandSide(ast.newBooleanLiteral(false));
		ExpressionStatement es =ast.newExpressionStatement(as);
		tryStatement.add(es);
		
		//�إ�If statement
		IfStatement ifStat = ast.newIfStatement();
		InfixExpression ife = ast.newInfixExpression();
		
		//�إ�if(....)
		ife.setLeftOperand(ast.newSimpleName(this.attempt));
		ife.setOperator(InfixExpression.Operator.EQUALS);
		ife.setRightOperand(ast.newNumberLiteral("0"));
		ifStat.setExpression(ife);

		//�إ�then statement
		Block thenBlock = ast.newBlock();
//		List<Statement> thenStat = thenBlock.statements();
		List<?> originalStat = original.getBody().statements();
		ifStat.setThenStatement(thenBlock);

		for(int i=0;i<originalStat.size();i++){
			//�N�쥻try�����e�ƻs�i��
			//thenStat.add(ASTNode.copySubtree(ast, (ASTNode) originalStat.get(i)));

			//�β��ʪ��覡�A�~���|�R������
			ASTNode placeHolder = rewrite.createMoveTarget((ASTNode) originalStat.get(i));
			ListRewrite moveRewrite = rewrite.getListRewrite(thenBlock, Block.STATEMENTS_PROPERTY);
			moveRewrite.insertLast(placeHolder, null);
		}
		
		//�إ�else statement
		Block elseBlock = ast.newBlock();
//		List<Statement> elseStat = elseBlock.statements();
		ifStat.setElseStatement(elseBlock);

		//��X�ĤG�htry����m
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
			//�}�l�ƻssecond try statement�����e��else statement
			List<?> secStat = secTs.getBody().statements();
			for (int i =0;i < secStat.size(); i++) {
				//elseStat.add(ASTNode.copySubtree(ast, (ASTNode) secStat.get(i)));
				ASTNode placeHolder = rewrite.createMoveTarget((ASTNode) secStat.get(i));
				ListRewrite moveRewrite = rewrite.getListRewrite(elseBlock, Block.STATEMENTS_PROPERTY);
				moveRewrite.insertLast(placeHolder, null);
			}
		} else {
			//�Ycatch statement���èS���ĤG��try,�h��catch�������G��@alternative
			if(originalCatch != null){
				//�Ncatch�����ecopy�i�h
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
		
		//�Nif statement�[�itry����
		tryStatement.add(ifStat);
		//��do while�s�ؤ@��Block,�ñNTry�[�i�h
		Block doBlock = doWhile.getAST().newBlock();
		doWhile.setBody(doBlock);
		List<Statement> doStat = doBlock.statements();
		doStat.add(ts);		
		return ts;
	}
	
	/**
	 * ���p�O��No alternative Retry refactoring,�h�γo��method�ӭק�try block
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
			//�N�쥻try�����e�ƻs�i��
			//tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) originalStat.get(i)));

			//�β��ʪ��覡�A�~���|�R������
			ASTNode placeHolder = rewrite.createMoveTarget((ASTNode) originalStat.get(i));
			ListRewrite moveRewrite = rewrite.getListRewrite(block, Block.STATEMENTS_PROPERTY);
			moveRewrite.insertLast(placeHolder, null);
		}
		
		//�إ�retry = true
		Assignment as = ast.newAssignment();
		as.setLeftHandSide(ast.newSimpleName(this.retry));
		as.setOperator(Assignment.Operator.ASSIGN);
		as.setRightHandSide(ast.newBooleanLiteral(false));
		ExpressionStatement es =ast.newExpressionStatement(as);
		tryStatement.add(es);
		
		//��do while�s�ؤ@��Block,�ñNTry�[�i�h
		Block doBlock = doWhile.getAST().newBlock();
		doWhile.setBody(doBlock);
		List<Statement> doStat = doBlock.statements();
		doStat.add(ts);		
		return ts;
	}
		
	/**
	 * �btry���إ�catch block
	 * @param ast
	 * @param original
	 * @param ts
	 */
	private void addCatchBlock(AST ast,TryStatement original,TryStatement ts){
		List<CatchClause> catchStatement = ts.catchClauses();
		List<?> originalCatch = original.catchClauses();
	
		//�إ߷s��catch
		for(int i=0;i<originalCatch.size();i++){
			//�Ĥ@�h��catch,���i�঳�������u�@�ث��A���ҥ~,�ҥH�ݭn�ΰj��
			CatchClause temp = (CatchClause)originalCatch.get(i);
			List<?> catchSt = temp.getBody().statements();
			//���ocatch()�A�������ҥ~���A
			SingleVariableDeclaration svd = (SingleVariableDeclaration)temp.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			SingleVariableDeclaration sv = (SingleVariableDeclaration) ASTNode.copySubtree(ast, temp.getException());
			//�Q�Φ��ܼƨӰO���s�إߪ�catch�ҭn�������ҥ~���A
			String newExType = null;
			if(catchSt != null){
				//���p�Ĥ@�hcatch�϶������O�Ū�,�N�h��try�O�_�s�b
				for(int x=0; x<catchSt.size(); x++){
					if(catchSt.get(x) instanceof TryStatement){
						//�M��P��ʪ��ҥ~
						newExType = findHomogeneousExType(ast,svd,catchSt.get(x));
					}
				}	
			}
			//���p�S�����,��ܥi��ĤG�h�϶��S��try-catch block,�u�ݪ����ƻs���e���������ҥ~���A�N�i
			if(newExType != null){				
				sv.setType(ast.newSimpleType(ast.newSimpleName(newExType)));
			}
			CatchClause cc = ast.newCatchClause();		
			cc.setException(sv);
			catchStatement.add(cc);
		}
		
		//�H�U�}�l�إ�catch Statment�������e
	
		for(int x=0;x<catchStatement.size();x++){
			//���إ�attempt++
			PostfixExpression pfe = ast.newPostfixExpression();
			pfe.setOperand(ast.newSimpleName(this.attempt));
			pfe.setOperator(PostfixExpression.Operator.INCREMENT);
			ExpressionStatement epfe= ast.newExpressionStatement(pfe);
				
			//�إ�retry = true
			Assignment as = ast.newAssignment();
			as.setLeftHandSide(ast.newSimpleName(this.retry));
			as.setOperator(Assignment.Operator.ASSIGN);
			as.setRightHandSide(ast.newBooleanLiteral(true));
			ExpressionStatement es =ast.newExpressionStatement(as);
				
			//�إ�if statement,��throw exception
			IfStatement ifStat = ast.newIfStatement();
			InfixExpression ife = ast.newInfixExpression();
			
			//�إ�if(....)
			ife.setLeftOperand(ast.newSimpleName(this.attempt));
			ife.setOperator(InfixExpression.Operator.GREATER);
			ife.setRightOperand(ast.newSimpleName(this.maxAttempt));
			ifStat.setExpression(ife);
				
			CatchClause cc =(CatchClause)catchStatement.get(x);
			CatchClause temp = (CatchClause)originalCatch.get(x);
	
			//�إ�then statement
			Block thenBlock = ast.newBlock();
			List<Statement> thenStat = thenBlock.statements();
			ifStat.setThenStatement(thenBlock);
			//�ۦ�إߤ@��throw statement�[�J
			ThrowStatement tst = ast.newThrowStatement();
			//�Nthrow��variable�ǤJ
			ClassInstanceCreation cic = ast.newClassInstanceCreation();
			//throw new RuntimeException()<--�w�]��
			cic.setType(ast.newSimpleType(ast.newSimpleName(this.exceptionType)));
				
			SingleVariableDeclaration svd = (SingleVariableDeclaration) temp
			.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			//�Nthrow new RuntimeException(ex)�A�����[�J�Ѽ� 
			cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
			tst.setExpression(cic);
			thenStat.add(tst);
			
			//�N�ѤU��statement�[�i�hcatch����
			List<Statement> ccStat = cc.getBody().statements();
			ccStat.add(epfe);
			ccStat.add(es);
			ccStat.add(ifStat);
		}
		
		//�[�J��import��Library(�J��RuntimeException�N���Υ[Library)
		if(!exceptionType.equals("RuntimeException")){
			addImportDeclaration();
			//���pmethod�e���S��throw�F�誺��,�N�[�W�h
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
	 * �ڭ̰��]source code���A�p�Gcatch clause�̭��٦�try-catch���ҥ~�B�z�A
	 * �N���]�ŦXspare-handler��bad smell�C�]���A�ڭ̭n��Xcatch clause���̥~�A
	 * �Mcatch clause�̭�try-catch��catch clause���ҥ~�i�H�έ���exception�����O�ӥ]�t��̡A
	 * �ϱo��htry-catch�i�H�X�֬��@��try-catch�C20120907�ɵ��ѡACharles�C
	 * 
	 * ��I��Ĥ@�h�P�ĤG�h�������ҥ~���P��,�M��P��ʨҥ~
	 * @param type : �Ĥ@�h���ҥ~���A�ܼ�
	 * @param node : �ĤG�h��try statement
	 */
	private String findHomogeneousExType(AST ast, SingleVariableDeclaration type, Object node) {	
		TryStatement ts = (TryStatement)node;
		//��ĤG�htry block���Ҧ���catch�϶�
		List<?> catchSt = ts.catchClauses();		
		if(catchSt != null){
			//���M��Ĥ@�htry-catch���ҥ~���A,�é��Wtrace������List��
			ArrayList<String> exList = new ArrayList<String>();
			ITypeBinding iTB = type.resolveBinding().getType();
			//�Njava.lang.Exception��@�̤W�h�ҥ~
			String topLevelEx = "Exception";
			//����ҥ~�N�s�_��,�Y���̤W�h�h���X�j��
			while(!(iTB.getName().equals(topLevelEx)) ){
				exList.add(iTB.getName());	
				iTB = iTB.getSuperclass();
			}
			exList.add(topLevelEx);
			
			//�Q�Φ�List�Ӭ���
			ArrayList<String> secList = new ArrayList<String>();
			for(int i = 0; i< catchSt.size(); i++){
				CatchClause cc = (CatchClause)catchSt.get(i);
				//���o�ĤG�h�ҥ~���ܼƫ��A
				SingleVariableDeclaration svd = (SingleVariableDeclaration)cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
				//TODO :�h�hcatch�ɪ��P��ʨҥ~����n�Q����
				iTB = svd.resolveBinding().getType();
				//�q���h���ҥ~���A���W��,���̤W�h������U��
				while(!(iTB.getName().equals(topLevelEx)) ){
					secList.add(iTB.getName());	
					iTB = iTB.getSuperclass();
				}
				secList.add(topLevelEx);
				break;
			}
			
			//���List�i����,�M���Өҥ~���@�P�����O
			if(secList != null){		
				for(String ex : secList){
					for(String exType : exList){
						if(ex.equals(exType)){
							//���@�P�����O�N�^��
							return ex;
						}							
					}
				}
			}			
		}
		return type.getType().toString();
	}
		
	/**
	 * �P�_�O�_�����[�J��Library,��throw RuntimeException�����p�n�ư�
	 * �]��throw RuntimeException����import Library
	 */
	private void addImportDeclaration(){
		//�P�_�O�_��import library
		ListRewrite listRewrite = rewrite.getListRewrite(this.actRoot,  CompilationUnit.IMPORTS_PROPERTY);
		List<ImportDeclaration> importList = listRewrite.getRewrittenList();
		boolean isImportLibrary = false;
		for(ImportDeclaration id : importList){
			if(exType.getFullyQualifiedName().equals(id.getName().getFullyQualifiedName())){
				isImportLibrary = true;
				break;
			}
		}
		
		//���p�S��import�N�[�J��AST��
		AST rootAst = this.actRoot.getAST(); 
		if(!isImportLibrary){
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(exType.getFullyQualifiedName()));
			listRewrite.insertLast(imp, null);
		}		
	}
	
	private void addAnnotationRoot(AST ast){
		//�n�إ�@Robustness(value={@Tag(level=3, exception=java.lang.RuntimeException.class)})�o�˪�Annotation
		//�إ�Annotation root
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));

		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);
		
		MethodDeclaration method = currentMethodNode;		
		if(currentMethodRLList.size() == 0){		
			//Retry��RL = 3
			rlary.expressions().add(getRLAnnotation(ast,3,exceptionType));
		}else{	
			//���p���ӴN��annotation�����ª��[�i�h
			for (RLMessage rlmsg : currentMethodRLList) {				
				int pos = rlmsg.getRLData().getExceptionType().toString().lastIndexOf(".");
				String cut = rlmsg.getRLData().getExceptionType().toString().substring(pos+1);

				//�p�G����RL annotation���ƴN���[�i�h
				if((!cut.equals(exceptionType)) && (rlmsg.getRLData().getLevel() != 3)){										
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));					
				}
			}
			//�ª��[������[�s��RL = 3 annotation�i��
			rlary.expressions().add(getRLAnnotation(ast,3,exceptionType));
			
			ListRewrite listRewrite = rewrite.getListRewrite(method, method.getModifiersProperty());
			List<IExtendedModifier> modifiers = listRewrite.getRewrittenList();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//����¦���annotation��N������
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
		//�NRL��library�[�i��
		addImportRLDeclaration();
	}
	
	/**
	 * ����RL Annotation��RL��� 
	 * @param ast :AST Object
	 * @param levelVal :�j���׵���
	 * @param exClass : �ҥ~���O
	 * @return NormalAnnotation AST Node
	 */
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal,String excption) {
		//�n�إ�@Robustness(value={@Tag(level=1, exception=java.lang.RuntimeException.class)})�o�˪�Annotation
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName(RTag.class.getSimpleName().toString()));

		// level = 3
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName(RTag.LEVEL));
		//Retry �w�]level = 3
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		rl.values().add(level);

		//exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName(RTag.EXCEPTION));
		TypeLiteral exclass = ast.newTypeLiteral();
		//�w�]��RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);
		return rl;
	}

	private void addImportRLDeclaration() {
		// �P�_�O�_�w�gImport Robustness��RL���ŧi
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
	 * �N�n�ܧ󪺸�Ƽg�^��Document��
	 */
	private void applyChange(){
		//�g�^Edit��
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
	 * ���oJavaProject
	 */
	public IJavaProject getProject(){
		return project;
	}

	/**
	 * �x�s�nThrow��Exception��m(�nimport�ϥ�)
	 * @param type
	 */
	public void setExType(IType type){		
		this.exType = type;
	}
	
	/**
	 * set attempt�ܼƦW��
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
	 * set maxAttempt�ܼƦW��
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
	 * set �̤j���զ���
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
	 * set Retry�ܼƦW��
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
	 * ����user�ҭnthrow��exception type
	 * @param name : exception type
	 */
	public RefactoringStatus setExceptionName(String name){
		//���p�ϥΪ̨S����g����F��,��RefactoringStatus�]��Error
		if(name.length() == 0){
			return RefactoringStatus.createFatalErrorStatus("Some Field is empty");
		}else{
			//���p���g�N��L�s�U��
			this.exceptionType = name;
			return new RefactoringStatus();
		}		
	}
}
