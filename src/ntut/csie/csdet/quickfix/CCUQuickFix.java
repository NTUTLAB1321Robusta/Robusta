package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.CarelessCleanUpAnalyzer;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 提供給Careless CleanUp的解法
 * @author chenyimin
 */
public class CCUQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(CCUQuickFix.class);

	private String label;

	private ASTRewrite rewrite;

	private TryStatement tryStatement;

	//欲修改的程式碼資訊
	private String moveLine;

	// Careless CleanUp的Smell Message
	private CSMessage smellMessage = null;

	//第幾個tryBlock
	private int tryIndex = -1;
	
	public CCUQuickFix(String label){
		this.label = label;
	}
	@Override
	public String getLabel() {
		return label;
	}
	
	@Override
	public void run(IMarker marker) {
		try {
			String problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem != null && (problem.equals(RLMarkerAttribute.CS_CARELESS_CLEANUP))){
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				//取得目前要被修改的method node
				findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));

				//找到要被修改的程式碼資訊
				moveLine = findMoveLine(msgIdx);

				/* 先從SmellMessage裡面去找出small的position()，
				 * 才可以使用fineTryStatement()
				 */
				findSmellMessage(marker);
				
				findTryStatement();		

				//判斷是否需要將VariableDeclaration宣告在try外面	
				ExpressionStatement expSt = null;
				if(moveLineStatement(tryStatement.getBody().statements()) instanceof ExpressionStatement){
					expSt = (ExpressionStatement)moveLineStatement(tryStatement.getBody().statements());
				}
				MethodInvocation mi = (MethodInvocation)expSt.getExpression();
				SimpleName sn = (SimpleName)mi.arguments().get(0);
				if(isVariableDeclareInTry(sn.getFullyQualifiedName())){
					actRoot.recordModifications();	//AST 2.0紀錄方式
					moveInstance(mi.getAST(), tryStatement, mi);	//移動VariableDeclaration
					this.applyChange();
				}

				findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));

				//找到要被修改的程式碼資訊
				moveLine = findMoveLine(msgIdx);

				findTryStatement();
				
				/* 若try Statement裡已經有Finally Block,就直接將該行程式碼移到Finally Block中
				 * 否則先建立Finally Block後,再移到Finally Block 
				 */
				//TODO: 做一次quickfix會存檔三次，造成要，undo三次。這個之後要記得修改
				if (hasFinallyBlock()) {
					moveToFinallyBlock();
				} else {
					addNewFinallyBlock();
					findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
					findTryStatement();
					moveToFinallyBlock();
				}
				//將要變更的資料寫回
				this.applyChange(rewrite);
				findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
				findTryStatement();
			}
		} catch (CoreException e) {
			logger.error("[CCUQuickFix] EXCEPTION ",e);
		}
	}

	/**
	 * 找到欲修改的程式碼資訊
	 * @return String
	 */
	private String findMoveLine(String msgIdx) {
		CarelessCleanUpAnalyzer ccVisitor = new CarelessCleanUpAnalyzer(this.actRoot); 
		currentMethodNode.accept(ccVisitor);
		//有try block的，才有可能提供quick fix
		List<CSMessage> ccList = ccVisitor.getCarelessCleanUpList(true);
		CSMessage csMsg = ccList.get(Integer.parseInt(msgIdx));
		return csMsg.getStatement();
	}
	
	/**
	 * 尋找Try Block
	 * @return
	 */
	private boolean findTryStatement() {
		MethodDeclaration md = (MethodDeclaration) currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();
		for (int i =0; i < statement.size(); i++) {
			if (statement.get(i) instanceof TryStatement) {
				TryStatement trystat = (TryStatement) statement.get(i);
				
				boolean isFound = false;

				Block tryBlock = trystat.getBody();
				List<?> tryStatements = tryBlock.statements();
				for (int j =0; j < tryStatements.size(); j++) {
					String tryString = tryStatements.get(j).toString();
					if(tryString.contains(moveLine)){
						/* 判斷這個smell在哪個try block裡面。
						 * 用來防止兩個try block裡面，剛好有一樣的變數名稱
						 */
						if(trystat.getStartPosition() <= smellMessage.getPosition() &&
								trystat.getStartPosition() + trystat.getLength()
							  >= smellMessage.getPosition()){
							isFound = true;
							tryIndex = i;
						}
					}
				}

				List<?> ccList = trystat.catchClauses();
				for (int j =0;j < ccList.size(); j++) {
					CatchClause cc = (CatchClause) ccList.get(j);
					Block catchBlock = cc.getBody();
					List<?> catchStatements = catchBlock.statements();
					for(int k = 0; k < catchStatements.size(); k++) {
						String catchString = catchStatements.get(k).toString();
						if (catchString.contains(moveLine))
							isFound = true;
					}
				}

				if (trystat.getFinally() != null) {
					Block finallyBlock = trystat.getFinally();
					List<?> finallyStatements = finallyBlock.statements();
					for (int j=0; j < finallyStatements.size(); j++) {
						String finallyString = finallyStatements.get(j).toString();
						if (finallyString.contains(moveLine))
							isFound = true;
					}
				}
				if (!isFound) {
					continue;
				} else {
					tryStatement = trystat;
					return true;
				}
			}
		}
		return false;
	}
	
	private void findSmellMessage(IMarker marker) {
		try {
			String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
			CarelessCleanUpAnalyzer visitor = new CarelessCleanUpAnalyzer(this.actRoot);
			currentMethodNode.accept(visitor);
			smellMessage = visitor.getCarelessCleanUpList().get(Integer.parseInt(msgIdx));
		} catch (CoreException e) {
			logger.error("[Find CS Method] EXCEPTION ", e);
		}
	}
	
	/**
	 * 判斷Try Statement是否有Finally Block
	 * @return boolean
	 */
	private boolean hasFinallyBlock() {
		Block finallyBlock = tryStatement.getFinally();
		if (finallyBlock != null) {
			//假如有Finally Block就標示為true
			return true;
		}
		return false;
	}

	/**
	 * 在Try Statement裡建立Finally Block
	 */
	private void addNewFinallyBlock(){
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();
		Block block = ast.newBlock();
		tryStatement.setFinally(block);
		this.applyChange();
	}
	
	private boolean isVariableDeclareInTry(String variableName){
		List<?> lstStats = tryStatement.getBody().statements();
		for (int i = 0; i < lstStats.size(); i++) {
			if(lstStats.get(i) instanceof VariableDeclarationStatement){
				VariableDeclarationStatement vds = (VariableDeclarationStatement)lstStats.get(i);
				List<?>  lstVdf = vds.fragments();
				for(int j = 0; j < lstVdf.size(); j++){
					VariableDeclarationFragment vdf = (VariableDeclarationFragment)lstVdf.get(j);
					if(vdf.getName().toString().equals(variableName)){
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * 將Instance移至Try Catch外
	 * @param ast
	 * @param tryStatement
	 * @param mi 要移動宣告位置的mi
	 */
	private void moveInstance(AST ast, TryStatement tryStatement, MethodInvocation mi) {			
		// traverse try statements
		List<?> tryList = tryStatement.getBody().statements();
		for (int i=0; i < tryList.size(); i++) {
			if (tryList.get(i) instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variable = (VariableDeclarationStatement) tryList.get(i);
				List<?> fragmentsList = variable.fragments();
				if (fragmentsList.size() == 1) {
					VariableDeclarationFragment fragment = (VariableDeclarationFragment) fragmentsList.get(0);
					// 若參數宣告在Try Block內
					if (fragment.getName().toString().equals(mi.arguments().get(0).toString())) {
						/* 將   InputStream fos = new ImputStream();
						 * 改為 fos = new InputStream();
						 */
						Assignment assignment = ast.newAssignment();
						assignment.setOperator(Assignment.Operator.ASSIGN);
						// fos
						assignment.setLeftHandSide(ast.newSimpleName(fragment.getName().toString()));
						// new InputStream
						Expression init = fragment.getInitializer();
						ASTNode copyNode = ASTNode.copySubtree(init.getAST(), init);
						assignment.setRightHandSide((Expression) copyNode);

						// 將fos = new ImputStream(); 替換到原本的程式裡
						if(assignment.getRightHandSide().getNodeType() != ASTNode.NULL_LITERAL){
							ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
							tryStatement.getBody().statements().set(i, expressionStatement);
						}else{
							//如果本來的程式碼是設定instance初始為null，那就直接移除掉 (ex: fos = null;)
							tryStatement.getBody().statements().remove(i);
						}
						// InputStream fos = null
						// 將new動作替換成null
						fragment.setInitializer(ast.newNullLiteral());
						// 加至原本程式碼之前(看原本的程式碼在md裡面，是第幾個statement，插到那個位置)
						MethodDeclaration md = (MethodDeclaration) currentMethodNode;
						md.getBody().statements().add(tryIndex, variable);
						break;
					}
				}
			}
		}
	}

	/**
	 * 將欲修改的程式碼移到Finally Block中
	 */
	private void moveToFinallyBlock() {
		Statement moveLineEs = null;

		rewrite = ASTRewrite.create(actRoot.getAST());
		
		ListRewrite tsRewrite = rewrite.getListRewrite(tryStatement.getBody(),Block.STATEMENTS_PROPERTY);
		List<?> tsList = tsRewrite.getOriginalList();

		//比對Try Statement裡是否有欲移動的程式碼,若有則移除
		for (int j=0; j<tsList.size(); j++) {
			String temp = tsList.get(j).toString();
			if (temp.contains(moveLine))
				moveLineEs = (Statement) tsList.get(j);
		}
//		moveLineEs = moveLineStatement(tsList);	//以上那麼多行，換這行
		
		//比對Catch Clauses裡是否有欲移動的程式碼,若有則移除
		//TODO: 這樣的語法，不就只會紀錄最後一個catchClause
		List<?> ccList = tryStatement.catchClauses();
		for (int j =0; j < ccList.size(); j++) {
			CatchClause cc = (CatchClause) ccList.get(j);
			ListRewrite ccRewrite = rewrite.getListRewrite(cc.getBody(),Block.STATEMENTS_PROPERTY);
			List<?> ccbody = ccRewrite.getOriginalList();
			for (int k =0; k < ccbody.size(); k++) {
				String ccStat = ccbody.get(k).toString();
				if (ccStat.contains(moveLine))
					moveLineEs = (Statement) ccbody.get(k);
			}
//			moveLineEs = moveLineStatement(ccbody); //以上那麼多行，換這行
		}

		Block finallyBlock = tryStatement.getFinally();
		ASTNode placeHolder = rewrite.createMoveTarget(moveLineEs);
		ListRewrite moveRewrite = rewrite.getListRewrite(finallyBlock, Block.STATEMENTS_PROPERTY);
		moveRewrite.insertLast(placeHolder, null);
	}
	
	/**
	 * 在指定block中，找找看有沒有moveLine的Statement
	 * @param rewriteOriginalList
	 * @return
	 */
	private Statement moveLineStatement(List<?> rewriteOriginalList){
		Statement movelineSt = null;
		for(int i = 0; i<rewriteOriginalList.size(); i++){
			if(rewriteOriginalList.get(i).toString().contains(moveLine)){
				movelineSt = (Statement)rewriteOriginalList.get(i);
				return movelineSt;
			}
		}
		return null;
	}
}