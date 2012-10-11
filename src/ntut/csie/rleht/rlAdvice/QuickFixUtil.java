package ntut.csie.rleht.rlAdvice;

import java.util.List;

import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

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


/**
 * 蒐集了quickfix時，常用的修改動作
 * @author Charles
 * @version 0.0.1
 */
public class QuickFixUtil {
	public static final String runtimeException = "RuntimeException";
	
	public static final String[] dummyHandlerStrings = {"System.out.print", "printStackTrace"};

	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#deleteStatement
	 * 清除特定的ExpressionStatement
	 * 目前最常用來清除System.out.print / printStackTrace
	 * @param statements
	 * @param delStrings
	 */
	public void deleteStatement(List<Statement> statements, String[] delStrings) {
		if(statements.size() != 0) {
			for(int i = 0; i<statements.size(); i++) {
				//TODO 想辦法別用instanceof
				if(statements.get(i) instanceof ExpressionStatement) {
					ExpressionStatement expStatement = (ExpressionStatement) statements.get(i);
					for(int j=0; j<delStrings.length; j++) {
						if(expStatement.getExpression().toString().contains(delStrings[j])) {
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
	 * 在Method 上面，增加RL Annotation（一併把class import進來）
	 * @param actRoot
	 * @param currentMethodDeclarationNode 必須是MethodDeclaration的ASTNode
	 * @param rlValue robustness level的值
	 * @param exceptionClass 例外的類別
	 */
	@SuppressWarnings("unchecked")
	public void addAnnotationRoot(CompilationUnit actRoot,
			ASTNode currentMethodDeclarationNode, int rlValue,
			String exceptionClass) {
		// 要建立@Robustness(value={@Tag(level=1, exception=java.lang.RuntimeException.class)})這樣的Annotation
		// 建立Annotation root
		
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
				// 把舊的annotation加進去
				// 判斷如果遇到重複的就不要加annotation
				
				if((!rlmsg.getRLData().getExceptionType().toString().contains(exceptionClass)) && (rlmsg.getRLData().getLevel() == rlValue))				
					rlary.expressions().add(getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
			}
			rlary.expressions().add(getRLAnnotation(ast, rlValue, exceptionClass));
			
			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				// 找到舊有的annotation後將它移除
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}
		}
		if (rlary.expressions().size() > 0) {
			method.modifiers().add(0, root);
		}
		// 將RL的library加進來
		addImportDeclaration(actRoot);
	}
	
	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#addImportDeclaration
	 * import Robustness & Tag class，以利annotation使用
	 * @param actRoot
	 */
	@SuppressWarnings("unchecked")
	private void addImportDeclaration(CompilationUnit actRoot) {
		// 判斷是否已經Import Robustness及RL的宣告
		List<ImportDeclaration> importList = actRoot.imports();
		// 是否已存在Robustness及RL的宣告
		boolean isImportRobustnessClass = false;
		boolean isImportRLClass = false;

		for (ImportDeclaration id : importList) {
			if (RLData.CLASS_ROBUSTNESS.equals(id.getName().getFullyQualifiedName()))
				isImportRobustnessClass = true;
			if (RLData.CLASS_RL.equals(id.getName().getFullyQualifiedName()))
				isImportRLClass = true;
		}

		AST rootAst = actRoot.getAST();
		if (!isImportRobustnessClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(Robustness.class.getName()));
			actRoot.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RTag.class.getName()));
			actRoot.imports().add(imp);
		}
	}
	
	/**
	 * @see ntut.csie.csdet.quickfix.DHQuickFix#getRLAnnotation
	 * 產生RL Annotation之RL資料
	 * @param ast: AST Object
	 * @param levelVal: 強健度等級
	 * @param excption: 例外類別
	 * @return NormalAnnotation AST Node
	 */
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal,String excption) {
		//要建立@Robustness(value={@Tag(level=1, exception=java.lang.RuntimeException.class)})這樣的Annotation
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("Tag"));

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
	 * 在CatchClause裡面，增加throw xxxException
	 * @param cc
	 * @param currentMethodDeclarationNode
	 * @param exceptionClass
	 */
	public void addThrowStatement(ASTNode cc, AST currentMethodDeclarationNode, String exceptionClass) {
		// 取得該catch()中的exception variable
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		CatchClause clause = (CatchClause) cc;
		// 自行建立一個throw statement加入
		ThrowStatement throwStatement = currentMethodDeclarationNode.newThrowStatement();
		
		// 將throw的variable傳入
		ClassInstanceCreation cic = currentMethodDeclarationNode.newClassInstanceCreation();
		// throw new RuntimeException()
		cic.setType(currentMethodDeclarationNode.newSimpleType(currentMethodDeclarationNode.newSimpleName(exceptionClass)));
		// 將throw new RuntimeException(ex)括號中加入參數
		cic.arguments().add(currentMethodDeclarationNode.newSimpleName(svd.resolveBinding().getName()));

		// 將新建立的節點寫回
		throwStatement.setExpression(cic);
		clause.getBody().statements().add(throwStatement);
	}
	
	public void reThrowException(int rlValue, String exception, int msgIdx, CompilationUnit actRoot, ASTNode currentMethodNode) {
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();

		// 準備在Catch Caluse中加入throw exception
		// 收集該method所有的catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<CatchClause> catchList = catchCollector.getMethodList();

		for (int i = 0; i < catchList.size(); i++) {
			//找到該Catch(如果Catch的位置與按下Quick那行的起始位置相同)
//			if (catchList.get(i).getStartPosition() == Integer.parseInt(srcPos)) {
				//建立RL Annotation
				addAnnotationRoot(actRoot, currentMethodNode, rlValue, exception);
				
				//在catch clause中建立throw statement
				addThrowStatement(catchList.get(i), ast);
				//檢查在method前面有沒有throw exception
				addThrownException(ast, exception, currentMethodNode);

//			}
		}
	}
	
	/**
	 * 在catchClause裡面，加上throw e
	 * @param cc
	 * @param ast
	 */
	private void addThrowStatement(CatchClause cc, AST ast) {
		// 取得該catch()中的exception variable
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

		// 自行建立一個throw statement加入
		ThrowStatement ts = ast.newThrowStatement();

		// 取得Catch後Exception的變數
		SimpleName name = ast.newSimpleName(svd.resolveBinding().getName());		
		
		// 加到throw statement
		ts.setExpression(name);

		// 將新建立的節點寫回
		cc.getBody().statements().add(ts);
	}
	
	/**
	 * 在Method後面加上throws exception
	 * @param ast
	 * @param exception
	 * @param currentMethodNode
	 */
	@SuppressWarnings("unchecked")
	private void addThrownException(AST ast, String exception, ASTNode currentMethodNode) {
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		boolean isExist = false;
		for(int i=0;i<md.thrownExceptions().size();i++) {
			if(md.thrownExceptions().get(i) instanceof SimpleName) {
				SimpleName sn = (SimpleName)md.thrownExceptions().get(i);
				if(sn.getIdentifier().equals(exception)) {
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
