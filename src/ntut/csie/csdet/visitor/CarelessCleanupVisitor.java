package ntut.csie.csdet.visitor;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

import org.eclipse.compare.internal.NullViewer;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;

public class CarelessCleanupVisitor extends ASTVisitor {
	/** AST��root */
	private CompilationUnit root;
	/** �x�s��쪺�ҥ~�B�z�a���D�{���X�Ҧb����ƥH�ε{���X���q...�� */
	private List<MarkerInfo> carelessCleanupList;
	private boolean isDetectingCarelessCleanupSmell;
	
	public CarelessCleanupVisitor(CompilationUnit compilationUnit){
		super();
		this.root = compilationUnit;
		carelessCleanupList = new ArrayList<MarkerInfo>();
		SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
		isDetectingCarelessCleanupSmell = smellSettings.isDetectingSmell(SmellSettings.SMELL_CARELESSCLEANUP);
	}
	
	public List<MarkerInfo> getCarelessCleanupList() {
		return carelessCleanupList;
	}
	
	/**
	 * �ھڳ]�w�ɪ���T�A�M�w�n���n���X��ʾ�C
	 */
	public boolean visit(MethodDeclaration node) {
		return isDetectingCarelessCleanupSmell;
	}
	
	/**********************************************************
	 * �ˬdIfStatement�PTryStatement�G
	 * �p�G���ݪ�MethodDeclaration�S���ߥX�ҥ~�F
	 * If�S��else���ԭz�A�BIf�u���@��Statement�F
	 * Try�S��finally���ԭz�Bcatch�S���ߥX�ҥ~�ATry�̭��]�u���@��Statement�F
	 * �h���|�h�ˬd�]�t�b�̭���MethodInvocation�A
	 * ��Y���޸̭���Statement�O�ƻ�A�����|�Q�ˬd�C
	 **********************************************************/

	// FIXME - for, while, do-while, parenthesis
	
	/**
	 * �W�[���ˬdclose������:If
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public boolean visit(IfStatement node) {
		if(isMethodDeclarationThrowException(node)) {
			return true;
		}
		
		// else���{���X�A�����if�N�n�Q����
		if(node.getElseStatement() != null) {
			return true;
		}
		
		/*
		 *  if �̭��u���@��try statement ���y�k����ؼg�k�C
		 *  �@�ӬO����A�����A�A�|�bthen statement���Block�C
		 *  �@�ӬO�S����A�����A�A�|�������try statement�C
		 */
		Statement thenStatement = node.getThenStatement();
		if(thenStatement.getNodeType() == ASTNode.EXPRESSION_STATEMENT) {
			return false;
		}
		
		if(thenStatement.getNodeType() != ASTNode.BLOCK) {
			return true;
		}
		
		Block thenBlock = (Block) thenStatement;
		if(thenBlock.statements().size() != 1) {
			return true;
		} else if(thenBlock.statements().size() == 1) {
			// �p�G�o��IfStatement�QTryStatement�]�ۡA�h���W���h�|���TryStatement
			ASTNode parentTryStatement = node.getParent().getParent();
			if (parentTryStatement.getNodeType() != ASTNode.TRY_STATEMENT) {
				return true;
			} else {
				// �p�G�]�۳o��if��try���ߥX�ҥ~�A���N�n�ˬdif�̭����S��careless cleanup
				if(isTryStatementThrowException((TryStatement)parentTryStatement)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * �W�[���ˬdclose������:Try
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public boolean visit(TryStatement node) {
		if(isMethodDeclarationThrowException(node)) {
			return true;
		}
		
		// Try�̭��W�L�@��Statement(�N���i�ण�uclose���ʧ@)
		if(node.getBody().statements().size() != 1) {
			return true;
		} else if (node.getFinally() != null) {
			return true;
		} else {
			// �n�`�N�o�@��Statement�O���Oif
			if(((Statement)node.getBody().statements().get(0)).getNodeType() == ASTNode.IF_STATEMENT) {
				return true;
			}
		}
		
		// �p�GCatch�̭����ߥX�ҥ~�A�N���O�i�H�s��bFinally���A������y���ʧ@
		if(isTryStatementThrowException(node)) {
			return true;
		}
		return false;
	}
	
	/**
	 * �ˬd�o��TryStatement�O���O���ߥX�ҥ~
	 * @param node
	 * @return true���ߥX�ҥ~�Afalse�S�ߥX�ҥ~
	 */
	private boolean isTryStatementThrowException(TryStatement node) {
		// �p�GCatch�̭����ߥX�ҥ~�A�N���O�i�H�s��bFinally���A������y���ʧ@
		for (int i = 0; i < node.catchClauses().size(); i++) {
			CatchClause cc = (CatchClause)node.catchClauses().get(i);
			for(int j = 0; j< cc.getBody().statements().size(); j++){
				Statement statement = (Statement)cc.getBody().statements().get(j);
				if(statement.getNodeType() == ASTNode.THROW_STATEMENT) {
					return true;
				}
			}
		}		
		return false;
	}
	
	/**
	 * �ˬd�o��MethodDeclaration�O�_���ߥX�ҥ~
	 * (RuntimeException�S��k�Q�γo�Ӥ�k�ˬd)
	 * @param node
	 * @return
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	private boolean isMethodDeclarationThrowException(ASTNode node) {
		if(node.getNodeType() == ASTNode.COMPILATION_UNIT) {
			throw new RuntimeException("Abatract Syntax Tree traversing error. by Charles.");
		}
		
		if(node.getNodeType() == ASTNode.METHOD_DECLARATION) {
			if(((MethodDeclaration)node).thrownExceptions().size() == 0)
				return false;
			else
				return true;  
		}
		
		return(isMethodDeclarationThrowException(node.getParent()));
	}
	
	private boolean visitDefault(MethodInvocation node) {
		// �M��method name��close
		if(!node.getName().toString().equals("close")) {
			return false;
		}

		/*
		 *	�M��o��close�O���O��@Closeable 
		 */
		if(node.resolveMethodBinding() == null) {
			System.out.println("aa");
		}
		if (!NodeUtils.isITypeBindingImplemented(node.resolveMethodBinding()
				.getDeclaringClass(), Closeable.class)) {
			return true;
		}
		
		// �g�L�W�z�ˬd��A��쪺close�N�n�Q�[�WMarker
		collectSmell(node);
		return false;
	}
	
	@Robustness(value = { @RTag(level = 1, exception = java.lang.RuntimeException.class) })
	public boolean visit(MethodInvocation node) {
		boolean userDefinedLibResult = true;
		boolean userDefinedResult = true;
		boolean userDefinedExtraRule = true;
		boolean defaultResult = true;
		
		UserDefinedMethodAnalyzer userDefinedMethodAnalyzer = new UserDefinedMethodAnalyzer(SmellSettings.SMELL_CARELESSCLEANUP);
		if(userDefinedMethodAnalyzer.analyzeLibrary(node)) {
			collectSmell(node);
			userDefinedLibResult = false;
		}
		
		if(userDefinedMethodAnalyzer.analyzeMethods(node)) {
			collectSmell(node);
			userDefinedResult = false;
		}
		
		if(userDefinedMethodAnalyzer.analyzeExtraRule(node, root)) {
			collectSmell(node);
			userDefinedExtraRule = false;
		}
		
		if(userDefinedMethodAnalyzer.getEnable()) {
			defaultResult = visitDefault(node);
		}
		
		if(userDefinedLibResult || userDefinedResult || userDefinedExtraRule || defaultResult) {
			return true;
		}
		
		return false;
	}
	
	private void collectSmell(MethodInvocation node) {
		ASTNode parentNode = NodeUtils.getSpecifiedParentNode(node, ASTNode.TRY_STATEMENT);
		MarkerInfo markerInfo = new MarkerInfo(RLMarkerAttribute.CS_CARELESS_CLEANUP,
			(node.getExpression() != null)? node.getExpression().resolveTypeBinding() : null,
			node.toString(), node.getStartPosition(),
			root.getLineNumber(node.getStartPosition()),
			(node.getExpression() != null)? node.getExpression().resolveTypeBinding().toString() : null);
		markerInfo.setIsInTry((parentNode != null)? true:false);
		carelessCleanupList.add(markerInfo);
	}
}
