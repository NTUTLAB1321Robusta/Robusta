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
 * ���ѵ�Careless CleanUp���Ѫk
 * @author chenyimin
 */
public class CCUQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(CCUQuickFix.class);

	private String label;

	private ASTRewrite rewrite;

	private TryStatement tryStatement;

	//���ק諸�{���X��T
	private String moveLine;

	// Careless CleanUp��Smell Message
	private MarkerInfo smellMessage = null;

	//�ĴX��tryBlock
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
				//���o�ثe�n�Q�ק諸method node
				findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));

				//���n�Q�ק諸�{���X��T
				moveLine = findMoveLine(msgIdx);

				/* ���qSmellMessage�̭��h��Xsmall��position()�A
				 * �~�i�H�ϥ�fineTryStatement()
				 */
				findTryStatement();
				
				//�P�_�O�_�ݭn�NVariableDeclaration�ŧi�btry�~��	
				findOutTheVariableInTry();
				/* �Ytry Statement�̤w�g��Finally Block,�N�����N�Ӧ�{���X����Finally Block��
				 * �_�h���إ�Finally Block��,�A����Finally Block 
				 */
				moveToFinallyBlock();
				
				//�N�n�ܧ󪺸�Ƽg�^
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
				moveInstance(tryStatement, mi);	//����VariableDeclaration
			}
		}
	}

	/**
	 * �����ק諸�{���X��T
	 * @return String
	 */
	private String findMoveLine(String msgIdx) {
		CarelessCleanupVisitor ccVisitor = new CarelessCleanupVisitor(actRoot);
		currentMethodNode.accept(ccVisitor);
		//��try block���A�~������quick fix
		smellMessage = ccVisitor.getCarelessCleanupList().get(Integer.parseInt(msgIdx));
		return smellMessage.getStatement();
	}
	
	/**
	 * �M��Try Block
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
				/* �P�_�o��smell�b����try block�̭��C
				 * �ΨӨ�����try block�̭��A��n���@�˪��ܼƦW��
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
	 * �NInstance����Try Catch�~
	 * @param ast
	 * @param tryStatement
	 * @param mi �n���ʫŧi��m��mi
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
					// �Y�ѼƫئbTry Block��
					if(fragment.getName().toString().equals(mi.arguments().get(0).toString())) {
						/* �N   InputStream fos = new ImputStream();
						 * �אּ fos = new InputStream();
						 * */
						Assignment assignment = ast.newAssignment();
						assignment.setOperator(Assignment.Operator.ASSIGN);
						// fos
						assignment.setLeftHandSide(ast.newSimpleName(fragment.getName().toString()));
						// new InputStream
						Expression init = fragment.getInitializer();
						ASTNode copyNode = ASTNode.copySubtree(init.getAST(), init);
						assignment.setRightHandSide((Expression) copyNode);

						// �Nfos = new ImputStream(); ������쥻���{����
						if(assignment.getRightHandSide().getNodeType() != ASTNode.NULL_LITERAL) {
							ExpressionStatement expressionStatement = ast.newExpressionStatement(assignment);
							tsRewrite.replace(variable, expressionStatement, null);
						} else {
							tsRewrite.remove((ASTNode)oList.get(i), null);
						}
						// InputStream fos = null
						// �Nnew�ʧ@������null
						// �[�ܭ쥻�{���X���e
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
	 * �N���ק諸�{���X����Finally Block��
	 */
	private void moveToFinallyBlock() {
		ListRewrite tsRewrite = rewrite.getListRewrite(tryStatement.getBody(), Block.STATEMENTS_PROPERTY);
		List<?> tsList = tsRewrite.getOriginalList();

		//���Try Statement�̬O�_�������ʪ��{���X
		Statement moveLineEs = moveLineStatement(tsList);
		
		// �p�GTry Statement�̭��䤣��A��ܬO�bCatch Clauses�̭�
		if(moveLineEs == null) {
			//���Catch Clauses�̬O�_�������ʪ��{���X
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
		
		// �p�G�S��finally block�N�����[�W�h
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
	 * �b���wblock���A���ݦ��S��moveLine��Statement
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