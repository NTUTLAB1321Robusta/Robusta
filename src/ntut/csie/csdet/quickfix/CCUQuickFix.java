package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.visitor.CarelessCleanupVisitor;
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
	private MarkerInfo smellMessage = null;

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
				findTryStatement();
				
				//判斷是否需要將VariableDeclaration宣告在try外面	
				findOutTheVariableInTry();
				/* 若try Statement裡已經有Finally Block,就直接將該行程式碼移到Finally Block中
				 * 否則先建立Finally Block後,再移到Finally Block 
				 */
				moveToFinallyBlock();
				
				//將要變更的資料寫回
				this.applyChange(rewrite);
			}
		} catch (CoreException e) {
			logger.error("[CCUQuickFix] EXCEPTION ",e);
		}
	}
	private void findOutTheVariableInTry() {
		AST ast = actRoot.getAST();
		rewrite = ASTRewrite.create(ast);
		ExpressionStatement expSt = (ExpressionStatement)moveLineStatement(tryStatement.getBody().statements());
		if(expSt != null) {
			MethodInvocation mi = (MethodInvocation)expSt.getExpression();
			SimpleName sn = (SimpleName)mi.arguments().get(0);
			if(isVariableDeclareInTry(sn.getFullyQualifiedName())){
				moveInstance(tryStatement, mi);	//移動VariableDeclaration
			}
		}
	}

	/**
	 * 找到欲修改的程式碼資訊
	 * @return String
	 */
	private String findMoveLine(String msgIdx) {
		CarelessCleanupVisitor ccVisitor = new CarelessCleanupVisitor(actRoot);
		currentMethodNode.accept(ccVisitor);
		//有try block的，才有提供quick fix
		smellMessage = ccVisitor.getCarelessCleanupList().get(Integer.parseInt(msgIdx));
		return smellMessage.getStatement();
	}
	
	/**
	 * 尋找Try Block
	 * @return
	 */
	private void findTryStatement() {
		MethodDeclaration md = (MethodDeclaration) currentMethodNode;
		List<?> statement = md.getBody().statements();
		for (int i =0; i < statement.size(); i++) {
			if (statement.get(i) instanceof TryStatement) {
				TryStatement trystat = (TryStatement) statement.get(i);
				
				// find in try block
				List<?> tryStatements = trystat.getBody().statements();
				if(containTargetLine(trystat, tryStatements, i)) {
					return;
				}
				// find in catch clause
				List<?> ccList = trystat.catchClauses();
				for (int j =0;j < ccList.size(); j++) {
					CatchClause cc = (CatchClause) ccList.get(j);
					List<?> catchStatements = cc.getBody().statements();
					if(containTargetLine(trystat, catchStatements, i)) {
						return;
					}
				}
				// find in finally if it has finally block
				if (trystat.getFinally() != null) {
					List<?> finallyStatements = trystat.getFinally().statements();
					if(containTargetLine(trystat, finallyStatements, i)) {
						return;
					}
				}
			}
		}
	}
	
	private boolean containTargetLine(TryStatement tryStatement, List<?> statements, int index) {
		for (int j =0; j < statements.size(); j++) {
			String tryString = statements.get(j).toString();
			if(tryString.contains(moveLine)){
				/* 判斷這個smell在哪個try block裡面。
				 * 用來防止兩個try block裡面，剛好有一樣的變數名稱
				 */
				if(tryStatement.getStartPosition() <= smellMessage.getPosition() &&
						tryStatement.getStartPosition() + tryStatement.getLength()
					  >= smellMessage.getPosition()){
					tryIndex = index;
					this.tryStatement = tryStatement;
					return true;
				}
			}
		}
		return false;
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
	private void moveInstance(TryStatement tryStatement, MethodInvocation mi) {
		AST ast = actRoot.getAST();
		// traverse try statements
		ListRewrite tsRewrite = rewrite.getListRewrite(tryStatement.getBody(), Block.STATEMENTS_PROPERTY);
		List<?> oList = tsRewrite.getOriginalList();
		for(int i = 0; i < oList.size(); i++) {
			if(oList.get(i) instanceof VariableDeclarationStatement) {
				VariableDeclarationStatement variable = (VariableDeclarationStatement)oList.get(i);
				List<?> fragments = variable.fragments();
				if(fragments.size() == 1) {
					VariableDeclarationFragment fragment = (VariableDeclarationFragment)fragments.get(0);
					// 若參數建在Try Block內
					if(fragment.getName().toString().equals(mi.arguments().get(0).toString())) {
						/* 將   InputStream fos = new ImputStream();
						 * 改為 fos = new InputStream();
						 * */
						Assignment assignment = ast.newAssignment();
						assignment.setOperator(Assignment.Operator.ASSIGN);
						// fos
						assignment.setLeftHandSide(ast.newSimpleName(fragment.getName().toString()));
						// new InputStream
						Expression init = fragment.getInitializer();
						ASTNode copyNode = ASTNode.copySubtree(init.getAST(), init);
						assignment.setRightHandSide((Expression) copyNode);

						// 將fos = new ImputStream(); 替換到原本的程式裡
						if(assignment.getRightHandSide().getNodeType() != ASTNode.NULL_LITERAL) {
							ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
							tsRewrite.replace(variable, expressionStatement, null);
						} else {
							tsRewrite.remove((ASTNode)oList.get(i), null);
						}
						// InputStream fos = null
						// 將new動作替換成null
						// 加至原本程式碼之前
						MethodDeclaration md = (MethodDeclaration)currentMethodNode;
						ASTNode placeHolder = rewrite.createMoveTarget(variable);
						ListRewrite moveRewrite = rewrite.getListRewrite(md.getBody(), Block.STATEMENTS_PROPERTY);
						moveRewrite.insertFirst(placeHolder, null);
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
		ListRewrite tsRewrite = rewrite.getListRewrite(tryStatement.getBody(), Block.STATEMENTS_PROPERTY);
		List<?> tsList = tsRewrite.getOriginalList();

		//比對Try Statement裡是否有欲移動的程式碼
		Statement moveLineEs = moveLineStatement(tsList);
		
		// 如果Try Statement裡面找不到，表示是在Catch Clauses裡面
		if(moveLineEs == null) {
			//比對Catch Clauses裡是否有欲移動的程式碼
			List<?> ccList = tryStatement.catchClauses();
			for (int j =0; j < ccList.size(); j++) {
				CatchClause cc = (CatchClause) ccList.get(j);
				ListRewrite ccRewrite = rewrite.getListRewrite(cc.getBody(), Block.STATEMENTS_PROPERTY);
				List<?> ccbody = ccRewrite.getOriginalList();
				moveLineEs = moveLineStatement(ccbody);
				if(moveLineEs != null)
					break;
			}
		}
		
		// 如果沒有finally block就幫它加上去
		Block finallyBody = null;
		if (tryStatement.getFinally() == null) {
			finallyBody = actRoot.getAST().newBlock();
			rewrite.set(tryStatement, TryStatement.FINALLY_PROPERTY, finallyBody, null);
		}
		else {
			finallyBody = tryStatement.getFinally();
		}
		
		ASTNode placeHolder = rewrite.createMoveTarget(moveLineEs);
		ListRewrite moveRewrite = rewrite.getListRewrite(finallyBody, Block.STATEMENTS_PROPERTY);
		moveRewrite.insertLast(placeHolder, null);
	}
	
	/**
	 * 在指定block中，找找看有沒有moveLine的Statement
	 * @param rewriteOriginalList
	 * @return
	 */
	private Statement moveLineStatement(List<?> rewriteOriginalList){
		for(int i = 0; i<rewriteOriginalList.size(); i++){
			Statement statement = (Statement)rewriteOriginalList.get(i);
			if(statement.toString().contains(moveLine)){
				if(statement.getStartPosition() == smellMessage.getPosition()) {
					return statement;
				}
			}
		}
		return null;
	}
}