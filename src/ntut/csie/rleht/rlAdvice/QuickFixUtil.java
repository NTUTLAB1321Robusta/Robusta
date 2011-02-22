package ntut.csie.rleht.rlAdvice;

import java.util.List;

import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;

import agile.exception.RL;
import agile.exception.Robustness;

/**
 * �`���Fquickfix�ɡA�`�Ϊ��ק�ʧ@
 * @author Charles
 * @version 0.0.1
 */
public class QuickFixUtil {
	public static final String runtimeException = "RuntimeException";
	
	public static final String[] dummyHandlerStrings = {"System.out.print", "printStackTrace"};

	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#deleteStatement
	 * �M���S�w��ExpressionStatement
	 * �ثe�̱`�ΨӲM��System.out.print / printStackTrace
	 * @param statements
	 * @param delStrings
	 */
	public void deleteStatement(List<Statement> statements, String[] delStrings){
		if(statements.size() != 0){
			for(int i = 0; i<statements.size(); i++){
				//TODO �Q��k�O��instanceof
				if(statements.get(i) instanceof ExpressionStatement){
					ExpressionStatement expStatement = (ExpressionStatement) statements.get(i);
					for(int j=0; j<delStrings.length; j++){
						if(expStatement.getExpression().toString().contains(delStrings[j])){
							statements.remove(i);
							deleteStatement(statements, delStrings);
							break;
						}
					}
				}
			}
		}
	}
	
	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#addAnnotationRoot
	 * �bMethod �W���A�W�[RL Annotation�]�@�֧�class import�i�ӡ^
	 * @param actRoot
	 * @param currentMethodDeclarationNode �����OMethodDeclaration��ASTNode
	 * @param rlValue robustness level����
	 * @param exceptionClass �ҥ~�����O
	 */
	@SuppressWarnings("unchecked")
	public void addAnnotationRoot(CompilationUnit actRoot,
			ASTNode currentMethodDeclarationNode, int rlValue,
			String exceptionClass) {
		//�n�إ�@Robustness(value={@RL(level=1, exception=java.lang.RuntimeException.class)})�o�˪�Annotation
		//�إ�Annotation root
		
		AST ast = currentMethodDeclarationNode.getAST();
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));
		
		ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(actRoot, currentMethodDeclarationNode.getStartPosition(), 0);
		List<RLMessage> currentMethodRLList = exVisitor.getMethodRLAnnotationList();
		
		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);
		
		MethodDeclaration method = (MethodDeclaration) currentMethodDeclarationNode;		
		if (currentMethodRLList.size() == 0) {
			rlary.expressions().add(getRLAnnotation(ast, rlValue, exceptionClass));
		} else {
			for (RLMessage rlmsg : currentMethodRLList) {
				//���ª�annotation�[�i�h
				//�P�_�p�G�J�쭫�ƪ��N���n�[annotation
				
				if((!rlmsg.getRLData().getExceptionType().toString().contains(exceptionClass)) && (rlmsg.getRLData().getLevel() == rlValue)){					
					rlary.expressions().add(getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
				}
			}
			rlary.expressions().add(getRLAnnotation(ast, rlValue, exceptionClass));
			
			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//����¦���annotation��N������
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}
		}
		if (rlary.expressions().size() > 0) {
			method.modifiers().add(0, root);
		}
		//�NRL��library�[�i��
		addImportDeclaration(actRoot);
	}
	
	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#addImportDeclaration
	 * import Robustness & RL class�A�H�Qannotation�ϥ�
	 * @param actRoot
	 */
	@SuppressWarnings("unchecked")
	private void addImportDeclaration(CompilationUnit actRoot) {
		// �P�_�O�_�w�gImport Robustness��RL���ŧi
		List<ImportDeclaration> importList = actRoot.imports();
		//�O�_�w�s�bRobustness��RL���ŧi
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

		AST rootAst = actRoot.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			actRoot.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RL.class.getName()));
			actRoot.imports().add(imp);
		}
	}
	
	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#getRLAnnotation
	 * ����RL Annotation��RL���
	 * @param ast: AST Object
	 * @param levelVal: �j���׵���
	 * @param excption: �ҥ~���O
	 * @return NormalAnnotation AST Node
	 */
	@SuppressWarnings("unchecked")
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal,String excption) {
		//�n�إ�@Robustness(value={@RL(level=1, exception=java.lang.RuntimeException.class)})�o�˪�Annotation
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("RL"));

		// level = 1
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName("level"));
		
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		rl.values().add(level);

		// exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);

		return rl;
	}
	

	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#addThrowStatement
	 * �bCatchClause�̭��A�W�[throw xxxException
	 * @param cc
	 * @param currentMethodDeclarationNode
	 * @param exceptionClass
	 */
	@SuppressWarnings("unchecked")
	public void addThrowStatement(ASTNode cc, AST currentMethodDeclarationNode, String exceptionClass){
		// ���o��catch()����exception variable
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		CatchClause clause = (CatchClause) cc;
		// �ۦ�إߤ@��throw statement�[�J
		ThrowStatement throwStatement = currentMethodDeclarationNode.newThrowStatement();
		
		// �Nthrow��variable�ǤJ
		ClassInstanceCreation cic = currentMethodDeclarationNode.newClassInstanceCreation();
		// throw new RuntimeException()
		cic.setType(currentMethodDeclarationNode.newSimpleType(currentMethodDeclarationNode.newSimpleName(exceptionClass)));
		// �Nthrow new RuntimeException(ex)�A�����[�J�Ѽ�
		cic.arguments().add(currentMethodDeclarationNode.newSimpleName(svd.resolveBinding().getName()));

		// �N�s�إߪ��`�I�g�^
		throwStatement.setExpression(cic);
		clause.getBody().statements().add(throwStatement);
	}
	
	public void reThrowException(int rlValue, String exception, int msgIdx, CompilationUnit actRoot, ASTNode currentMethodNode) {
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();

		//�ǳƦbCatch Caluse���[�Jthrow exception
		//������method�Ҧ���catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<ASTNode> catchList = catchCollector.getMethodList();

		for (int i = 0; i < catchList.size(); i++) {
			//����Catch(�p�GCatch����m�P���UQuick���檺�_�l��m�ۦP)
//			if (catchList.get(i).getStartPosition() == Integer.parseInt(srcPos)) {
				//�إ�RL Annotation
				addAnnotationRoot(actRoot, currentMethodNode, rlValue, exception);
				
				//�bcatch clause���إ�throw statement
				addThrowStatement(catchList.get(i), ast);
				//�ˬd�bmethod�e�����S��throw exception
				addThrownException(ast,exception, currentMethodNode);

//			}
		}
	}
	
	/**
	 * �bcatchClause�̭��A�[�Wthrow e
	 * @param cc
	 * @param ast
	 */
	@SuppressWarnings("unchecked")
	private void addThrowStatement(ASTNode cc, AST ast) {
		//���o��catch()����exception variable
		SingleVariableDeclaration svd = 
			(SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

		CatchClause clause = (CatchClause)cc;

		//�ۦ�إߤ@��throw statement�[�J
		ThrowStatement ts = ast.newThrowStatement();

		//���oCatch��Exception���ܼ�
		SimpleName name = ast.newSimpleName(svd.resolveBinding().getName());		
		
		//�[��throw statement
		ts.setExpression(name);

		//�N�s�إߪ��`�I�g�^
		clause.getBody().statements().add(ts);
	}
	
	/**
	 * �bMethod�᭱�[�Wthrows exception
	 * @param ast
	 * @param exception
	 * @param currentMethodNode
	 */
	@SuppressWarnings("unchecked")
	private void addThrownException(AST ast, String exception, ASTNode currentMethodNode){
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		boolean isExist = false;
		for(int i=0;i<md.thrownExceptions().size();i++){
			if(md.thrownExceptions().get(i) instanceof SimpleName){
				SimpleName sn = (SimpleName)md.thrownExceptions().get(i);
				if(sn.getIdentifier().equals(exception)){
					isExist = true;
					break;
				}
			}
		}
		if (!isExist) {
			md.thrownExceptions().add(ast.newSimpleName(exception));
		}
	}
}
