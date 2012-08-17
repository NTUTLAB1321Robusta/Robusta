package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UMQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(UMQuickFix.class);

	// 記錄該Quick Fix的說明
	private String label;
	
	private ASTRewrite rewrite;

	public UMQuickFix(String label) {
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
			if (problem != null && problem.equals(RLMarkerAttribute.CS_UNPROTECTED_MAIN)) {
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				// 先找出要被修改的main function,如果存在就開始進行fix
				if(findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx))) {
					addBigOuterTry();
					this.applyChange(rewrite);
				}
			}
		} catch (CoreException e) {		
			logger.error("[UMQuickFix] EXCEPTION ",e);
		}		
	}
	
	/**
	 * 針對Code smell進行修改,增加一個Big try block
	 */
	private void addBigOuterTry() {
		rewrite = ASTRewrite.create(actRoot.getAST());
		
		// 取得main function block中所有的statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();

		boolean isTryExist = false;
		int pos = -1;

		for (int i = 0; i < statement.size(); i++) {
			if (statement.get(i) instanceof TryStatement) {
				// try block的位置
				pos = i;
				// 假如Main function中有try就標示為true
				isTryExist = true;
				break;
			}
		}

		ListRewrite listRewrite = rewrite.getListRewrite(mdBlock, Block.STATEMENTS_PROPERTY);
		
		if (isTryExist) {
			/*------------------------------------------------------------------------*
			-	假如Main function中已經有try block了,那就要去確認try block是位於main的一開始
				還是中間,或者是在最後面,並依據這三種情況將程式碼都塞進去try block裡面  
	        *-------------------------------------------------------------------------*/	
			moveTryBlock(statement, pos, listRewrite);
		} else {
			/*------------------------------------------------------------------------*
			-	假如Main function中沒有try block了,那就自己增加一個try block,再把main中所有的
				程式全部都塞進try block中
	        *-------------------------------------------------------------------------*/
			addNewTryBlock(listRewrite);
		}
	}
	
	/**
	 * 新增一個Try block,並把相關的程式碼加進去Try中
	 * @param ast
	 * @param listRewrite
	 */
	@SuppressWarnings("unchecked")
	private void addNewTryBlock(ListRewrite listRewrite) {
		AST ast = actRoot.getAST();
		TryStatement ts = ast.newTryStatement();
		
		// 替try 加入一個Catch clause
		List<CatchClause> catchStatement = ts.catchClauses();
		CatchClause cc = ast.newCatchClause();
		ListRewrite catchRewrite = rewrite.getListRewrite(cc.getBody(), Block.STATEMENTS_PROPERTY);
		
		// 建立catch的type為 catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		
		// 在Catch中加入TODO的註解
		StringBuffer comment = new StringBuffer();
		comment.append("// TODO: handle exception");
		ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.EMPTY_STATEMENT);
		catchRewrite.insertLast(placeHolder, null);
		catchStatement.add(cc);
		
		// 將原本在try block之外的程式都移動進來
		ListRewrite tryStatement = rewrite.getListRewrite(ts.getBody(), Block.STATEMENTS_PROPERTY);
		int listSize = listRewrite.getRewrittenList().size();
		tryStatement.insertLast(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
								(ASTNode) listRewrite.getRewrittenList().get(listSize - 1)), null);
		// 將新增的try statement加進來
		listRewrite.insertLast(ts, null);		
	}

	/**
	 * 新增一個Try block區塊,並將在try之外的程式加入至Try block中
	 * 
	 * @param ast
	 * @param statement
	 * @param pos			: try的位置
	 * @param listRewrite
	 */
	private void moveTryBlock(List<?> statement, int pos, ListRewrite listRewrite) {
		TryStatement original = (TryStatement)statement.get(pos);
		ListRewrite originalRewrite = rewrite.getListRewrite(original.getBody(), Block.STATEMENTS_PROPERTY);
	
		// 假如Try block之後還有程式碼,就複製進去try block之內
		int totalSize = statement.size();
		if (pos == 0) {
			// 假如try block在最一開始
			if (totalSize > 1) {
				// 將try block之後的程式碼move到try中
				originalRewrite.insertLast(	listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(1), 
											(ASTNode) listRewrite.getRewrittenList().get(totalSize - 1)), null);
			}			
		} else if (pos == listRewrite.getRewrittenList().size() - 1) {
			// 假如try block在結尾	
			// 將try block之前的程式碼move到try中
			originalRewrite.insertFirst(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
										(ASTNode) listRewrite.getRewrittenList().get(pos - 1)), null);
		} else {
			// 假如try block在中間，先將try block之前的程式碼move到try中
			originalRewrite.insertFirst(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
										(ASTNode) listRewrite.getRewrittenList().get(pos-1)), null);
			// 將try block之後的程式碼move到try中，由於在把try之前的東西移進新的try block之後，
			// list的位置會更動,try的位置會跑到最前面,所以從1開始複製			
			totalSize = totalSize - pos;
			originalRewrite.insertLast(	listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(1), 
										(ASTNode) listRewrite.getRewrittenList().get(totalSize-1)), null);
		}		
		addCatchBody(original);
	}
	
	/**
	 * 如果沒有catch(Exception e)的話要加進去
	 * @param ast
	 * @param original
	 */
	@SuppressWarnings("unchecked")
	private void addCatchBody(TryStatement original) {
		AST ast = actRoot.getAST();
		// 利用此變數來判斷原本main中的catch exception型態是否為catch(Exception e...)
		boolean isExceptionType = false;
		List<CatchClause> catchStatement = original.catchClauses();
		for (int i = 0; i < catchStatement.size(); i++) {
			CatchClause temp = (CatchClause) catchStatement.get(i);
			SingleVariableDeclaration svd = (SingleVariableDeclaration) temp.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			if (svd.getType().toString().equals("Exception")) {
				// 假如有找到符合的型態,就把變數設成true
				isExceptionType = true;
				// 在Catch中加入TODO的註解
				StringBuffer comment = new StringBuffer();
				comment.append("// TODO: handle exception");
				ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.RETURN_STATEMENT);
				ListRewrite todoRewrite = rewrite.getListRewrite(temp.getBody(), Block.STATEMENTS_PROPERTY);
				todoRewrite.insertLast(placeHolder, null);
			}
		}

		if (!isExceptionType) {
			// 建立新的catch(Exception ex)
			ListRewrite catchRewrite = rewrite.getListRewrite(original, TryStatement.CATCH_CLAUSES_PROPERTY);
			CatchClause cc = ast.newCatchClause();		
			SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
			sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
			sv.setName(ast.newSimpleName("exxxxx"));
			cc.setException(sv);
			catchRewrite.insertLast(cc, null);
			// 在Catch中加入todo的註解
			StringBuffer comment = new StringBuffer();
			comment.append("// TODO: handle exception");
			ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.RETURN_STATEMENT);
			ListRewrite todoRewrite = rewrite.getListRewrite(cc.getBody(), Block.STATEMENTS_PROPERTY);
			todoRewrite.insertLast(placeHolder, null);
		}
	}
}
