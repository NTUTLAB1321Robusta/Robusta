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

	// �O����Quick Fix������
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
				// ����X�n�Q�ק諸main function,�p�G�s�b�N�}�l�i��fix
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
	 * �w��Code smell�i��ק�,�W�[�@��Big try block
	 */
	private void addBigOuterTry() {
		rewrite = ASTRewrite.create(actRoot.getAST());
		
		// ���omain function block���Ҧ���statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List<?> statement = mdBlock.statements();

		boolean isTryExist = false;
		int pos = -1;

		for (int i = 0; i < statement.size(); i++) {
			if (statement.get(i) instanceof TryStatement) {
				// try block����m
				pos = i;
				// ���pMain function����try�N�Хܬ�true
				isTryExist = true;
				break;
			}
		}

		ListRewrite listRewrite = rewrite.getListRewrite(mdBlock, Block.STATEMENTS_PROPERTY);
		
		if (isTryExist) {
			/*------------------------------------------------------------------------*
			-	���pMain function���w�g��try block�F,���N�n�h�T�{try block�O���main���@�}�l
				�٬O����,�Ϊ̬O�b�̫᭱,�è̾ڳo�T�ر��p�N�{���X����i�htry block�̭�  
	        *-------------------------------------------------------------------------*/	
			moveTryBlock(statement, pos, listRewrite);
		} else {
			/*------------------------------------------------------------------------*
			-	���pMain function���S��try block�F,���N�ۤv�W�[�@��try block,�A��main���Ҧ���
				�{����������itry block��
	        *-------------------------------------------------------------------------*/
			addNewTryBlock(listRewrite);
		}
	}
	
	/**
	 * �s�W�@��Try block,�ç�������{���X�[�i�hTry��
	 * @param ast
	 * @param listRewrite
	 */
	@SuppressWarnings("unchecked")
	private void addNewTryBlock(ListRewrite listRewrite) {
		AST ast = actRoot.getAST();
		TryStatement ts = ast.newTryStatement();
		
		// ��try �[�J�@��Catch clause
		List<CatchClause> catchStatement = ts.catchClauses();
		CatchClause cc = ast.newCatchClause();
		ListRewrite catchRewrite = rewrite.getListRewrite(cc.getBody(), Block.STATEMENTS_PROPERTY);
		
		// �إ�catch��type�� catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		
		// �bCatch���[�JTODO������
		StringBuffer comment = new StringBuffer();
		comment.append("// TODO: handle exception");
		ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.EMPTY_STATEMENT);
		catchRewrite.insertLast(placeHolder, null);
		catchStatement.add(cc);
		
		// �N�쥻�btry block���~���{�������ʶi��
		ListRewrite tryStatement = rewrite.getListRewrite(ts.getBody(), Block.STATEMENTS_PROPERTY);
		int listSize = listRewrite.getRewrittenList().size();
		tryStatement.insertLast(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
								(ASTNode) listRewrite.getRewrittenList().get(listSize - 1)), null);
		// �N�s�W��try statement�[�i��
		listRewrite.insertLast(ts, null);		
	}

	/**
	 * �s�W�@��Try block�϶�,�ñN�btry���~���{���[�J��Try block��
	 * 
	 * @param ast
	 * @param statement
	 * @param pos			: try����m
	 * @param listRewrite
	 */
	private void moveTryBlock(List<?> statement, int pos, ListRewrite listRewrite) {
		TryStatement original = (TryStatement)statement.get(pos);
		ListRewrite originalRewrite = rewrite.getListRewrite(original.getBody(), Block.STATEMENTS_PROPERTY);
	
		// ���pTry block�����٦��{���X,�N�ƻs�i�htry block����
		int totalSize = statement.size();
		if (pos == 0) {
			// ���ptry block�b�̤@�}�l
			if (totalSize > 1) {
				// �Ntry block���᪺�{���Xmove��try��
				originalRewrite.insertLast(	listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(1), 
											(ASTNode) listRewrite.getRewrittenList().get(totalSize - 1)), null);
			}			
		} else if (pos == listRewrite.getRewrittenList().size() - 1) {
			// ���ptry block�b����	
			// �Ntry block���e���{���Xmove��try��
			originalRewrite.insertFirst(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
										(ASTNode) listRewrite.getRewrittenList().get(pos - 1)), null);
		} else {
			// ���ptry block�b�����A���Ntry block���e���{���Xmove��try��
			originalRewrite.insertFirst(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
										(ASTNode) listRewrite.getRewrittenList().get(pos-1)), null);
			// �Ntry block���᪺�{���Xmove��try���A�ѩ�b��try���e���F�貾�i�s��try block����A
			// list����m�|���,try����m�|�]��̫e��,�ҥH�q1�}�l�ƻs			
			totalSize = totalSize - pos;
			originalRewrite.insertLast(	listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(1), 
										(ASTNode) listRewrite.getRewrittenList().get(totalSize-1)), null);
		}		
		addCatchBody(original);
	}
	
	/**
	 * �p�G�S��catch(Exception e)���ܭn�[�i�h
	 * @param ast
	 * @param original
	 */
	@SuppressWarnings("unchecked")
	private void addCatchBody(TryStatement original) {
		AST ast = actRoot.getAST();
		// �Q�Φ��ܼƨӧP�_�쥻main����catch exception���A�O�_��catch(Exception e...)
		boolean isExceptionType = false;
		List<CatchClause> catchStatement = original.catchClauses();
		for (int i = 0; i < catchStatement.size(); i++) {
			CatchClause temp = (CatchClause) catchStatement.get(i);
			SingleVariableDeclaration svd = (SingleVariableDeclaration) temp.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			if (svd.getType().toString().equals("Exception")) {
				// ���p�����ŦX�����A,�N���ܼƳ]��true
				isExceptionType = true;
				// �bCatch���[�JTODO������
				StringBuffer comment = new StringBuffer();
				comment.append("// TODO: handle exception");
				ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.RETURN_STATEMENT);
				ListRewrite todoRewrite = rewrite.getListRewrite(temp.getBody(), Block.STATEMENTS_PROPERTY);
				todoRewrite.insertLast(placeHolder, null);
			}
		}

		if (!isExceptionType) {
			// �إ߷s��catch(Exception ex)
			ListRewrite catchRewrite = rewrite.getListRewrite(original, TryStatement.CATCH_CLAUSES_PROPERTY);
			CatchClause cc = ast.newCatchClause();		
			SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
			sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
			sv.setName(ast.newSimpleName("exxxxx"));
			cc.setException(sv);
			catchRewrite.insertLast(cc, null);
			// �bCatch���[�Jtodo������
			StringBuffer comment = new StringBuffer();
			comment.append("// TODO: handle exception");
			ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.RETURN_STATEMENT);
			ListRewrite todoRewrite = rewrite.getListRewrite(cc.getBody(), Block.STATEMENTS_PROPERTY);
			todoRewrite.insertLast(placeHolder, null);
		}
	}
}
