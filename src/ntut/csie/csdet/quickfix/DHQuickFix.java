package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agile.exception.RL;
import agile.exception.Robustness;

/**
 * 提供給Ignore checked Exception與Dummy handler的解法
 * @author chewei
 */
public class DHQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(DHQuickFix.class);
	
	private String label;

	// 紀錄所找到的code smell list
	private List<CSMessage> currentExList = null;
	// 目前method的RL Annotation資訊
	private List<RLMessage> currentMethodRLList = null;

	// 紀錄code smell的type
	private String problem;
	// 修正後的Exception型態
	private String exType = "RuntimeException";

	//刪掉的Statement數目
	private int delStatement = 0;
	// 反白的行數
	//int selectLine = -1;
	
	public DHQuickFix(String label){
		this.label = label;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		try {
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);

			if(problem != null && (problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) || 
								  (problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION))) {
				//如果碰到dummy handler,則將exception rethrow
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				
				boolean isok = this.findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok) {
					currentExList = findEHSmellList(problem);
					// 檢查是否可取得EH Smell List
					if (currentExList == null)
						return;
					//將Method加入Throw Exception，並回傳Catch的Index
					int catchIdx = rethrowException(Integer.parseInt(msgIdx));

					// 調整RL Annotation順序 TODO 待修正
					//new RLOrderFix().run(marker.getResource(), methodIdx, msgIdx);
					// 取得反白的行數 (暫時不需要反白行數)
					//selectSourceLine(marker, methodIdx, catchIdx);
				}
			}
		} catch (CoreException e) {
			logger.error("[DHQuickFix] EXCEPTION ",e);
		}
	}

	/**
	 * 取得Method相關資訊
	 * @param resource		來源
	 * @param methodIdx		Method的Index
	 * @return				是否成功
	 */
	private List<CSMessage> findEHSmellList(String problem) {
		if (currentMethodNode != null) {
			// 取得這個method的RL資訊
			ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
			currentMethodNode.accept(exVisitor);
			currentMethodRLList = exVisitor.getMethodRLAnnotationList();

			// 找出這個method的code smell
			CodeSmellAnalyzer visitor = new CodeSmellAnalyzer(this.actRoot);
			currentMethodNode.accept(visitor);
			if (problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)) {
				return visitor.getIgnoreExList();
			} else {
				return visitor.getDummyList();
			}
		}
		return null;
	}

	/**
	 * 將該method rethrow unchecked exception
	 * @param msgIdx	marker的Index
	 * @return			marker位於的Catch Index
	 */
	private int rethrowException(int msgIdx){
		try {
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
		
			// 準備在Catch Clause中加入throw exception
			// 取得EH smell的資訊
			CSMessage msg = currentExList.get(msgIdx);

			//收集該method所有的catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();
			
			// 去比對startPosition,找出要修改的節點
			for (int i = 0; i < catchList.size(); i++) {
				if (catchList.get(i).getStartPosition() == msg.getPosition()) {
					// 建立RL Annotation
					addAnnotationRoot(ast);
					// 在catch clause中建立throw statement
					addThrowStatement(catchList.get(i), ast);
					// 寫回Edit中
					this.applyChange();

					return i;
				}
			}
		}catch (Exception ex) {
			logger.error("[Rethrow Exception] EXCEPTION ",ex);
		}
		return -1;
	}
	
	/**
	 * 在catch中增加throw new RuntimeException(..)
	 * @param cc
	 * @param ast
	 */
	private void addThrowStatement(ASTNode cc, AST ast) {
		// 取得該catch()中的exception variable
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		CatchClause clause = (CatchClause) cc;
		// 自行建立一個throw statement加入
		ThrowStatement ts = ast.newThrowStatement();
		
		// 將throw的variable傳入
		ClassInstanceCreation cic = ast.newClassInstanceCreation();
		// throw new RuntimeException()
		cic.setType(ast.newSimpleType(ast.newSimpleName(exType)));
		// 將throw new RuntimeException(ex)括號中加入參數
		cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));

		// 取得CatchClause所有的statement,將相關print例外資訊的東西移除
		List statement = clause.getBody().statements();

		delStatement = statement.size();
		if (problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
			// 假如要fix的code smell是dummy handler,就要把catch中的列印資訊刪除
			deleteStatement(statement);
		}
		delStatement -= statement.size();
		// 將新建立的節點寫回
		ts.setExpression(cic);
		statement.add(ts);
	}
	
	/**
	 * 產生Annotation
	 * @param ast
	 */
	private void addAnnotationRoot(AST ast){
		//要建立@Robustness(value={@RL(level=1, exception=java.lang.RuntimeException.class)})這樣的Annotation
		//建立Annotation root
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));

		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);

		MethodDeclaration method = (MethodDeclaration) currentMethodNode;		
		if (currentMethodRLList.size() == 0) {
			rlary.expressions().add(getRLAnnotation(ast,1,exType));
		} else {
			for (RLMessage rlmsg : currentMethodRLList) {
				//把舊的annotation加進去
				//判斷如果遇到重複的就不要加annotation
				
				if((!rlmsg.getRLData().getExceptionType().toString().contains(exType)) && (rlmsg.getRLData().getLevel() == 1)){					
					rlary.expressions().add(getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
				}
			}
			rlary.expressions().add(getRLAnnotation(ast,1,exType));
			
			List<IExtendedModifier> modifiers = method.modifiers();
			for (int i = 0, size = modifiers.size(); i < size; i++) {
				//找到舊有的annotation後將它移除
				if (modifiers.get(i).isAnnotation() && modifiers.get(i).toString().indexOf("Robustness") != -1) {
					method.modifiers().remove(i);
					break;
				}
			}
		}
		if (rlary.expressions().size() > 0) {
			method.modifiers().add(0, root);
		}
		//將RL的library加進來
		addImportDeclaration();
	}
	
	/**
	 * 產生RL Annotation之RL資料
	 * @param ast: AST Object
	 * @param levelVal:強健度等級
	 * @param exClass:例外類別
	 * @return NormalAnnotation AST Node
	 */
	@SuppressWarnings("unchecked")
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal,String excption) {
		//要建立@Robustness(value={@RL(level=1, exception=java.lang.RuntimeException.class)})這樣的Annotation
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("RL"));

		// level = 1
		MemberValuePair level = ast.newMemberValuePair();
		level.setName(ast.newSimpleName("level"));
		//throw statement 預設level = 1
		level.setValue(ast.newNumberLiteral(String.valueOf(levelVal)));
		rl.values().add(level);

		// exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		// 預設為RuntimeException
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);

		return rl;
	}
	
	/**
	 * 產生import Robustness Library
	 */
	private void addImportDeclaration() {
		// 判斷是否已經Import Robustness及RL的宣告
		List<ImportDeclaration> importList = this.actRoot.imports();
		//是否已存在Robustness及RL的宣告
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
			this.actRoot.imports().add(imp);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RL.class.getName()));
			this.actRoot.imports().add(imp);
		}
	}

	/**
	 * 在Rethrow之前,先將相關的print字串都清除掉
	 * @param statementTemp
	 */
	private void deleteStatement(List<Statement> statementTemp){
		// 從Catch Clause裡面剖析兩種情形
		if(statementTemp.size() != 0){
			for (int i = 0; i < statementTemp.size(); i++) {
				if (statementTemp.get(i) instanceof ExpressionStatement) {
					ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);
					// 遇到System.out.print or printStackTrace就把他remove掉
					if (statement.getExpression().toString().contains("System.out.print") ||
						statement.getExpression().toString().contains("printStackTrace")) {

						statementTemp.remove(i);
						// 移除完之後ArrayList的位置會重新調整過,所以利用遞回來繼續往下找符合的條件並移除
						deleteStatement(statementTemp);
					}
				}
			}
		}
	}

//	/**
//	 * 反白指定行數
//	 * @param marker		欲反白Statement的Resource
//	 * @param methodIdx		欲反白Statement的Method Index
//	 * @param catchIdx		欲反白Statement的Catch Index
//	 */
//	private void selectSourceLine(IMarker marker, String methodIdx, int catchIdx) {
//		//重新取得Method資訊
//		boolean isOK = this.findCurrentMethod(marker.getResource(),Integer.parseInt(methodIdx));
//		if (isOK) {
//			try {
//				ICompilationUnit cu = (ICompilationUnit) actOpenable;
//				Document document = new Document(cu.getBuffer().getContents());
//				//取得目前的EditPart
//				IEditorPart editorPart = EditorUtils.getActiveEditor();
//				ITextEditor editor = (ITextEditor) editorPart;
//
//				//取得反白Statement的行數
//				int selectLine = getThrowStatementSourceLine(catchIdx);
//				//若反白行數為
//				if (selectLine == -1) {
//					//取得Method的起點位置
//					int srcPos = currentMethodNode.getStartPosition();
//					//用Method起點位置取得Method位於第幾行數(起始行數從0開始，不是1，所以減1)
//					selectLine = this.actRoot.getLineNumber(srcPos)-1;
//				}
//				//取得反白行數在SourceCode的行數資料
//				IRegion lineInfo = document.getLineInformation(selectLine);
//
//				//反白該行 在Quick fix完之後,可以將游標定位在Quick Fix那行
//				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
//			} catch (JavaModelException e) {
//				logger.error("[Rethrow checked Exception] EXCEPTION ",e);
//			} catch (BadLocationException e) {
//				logger.error("[BadLocation] EXCEPTION ",e);
//			}
//		}
//	}
	
//	/**
//	 * 取得Throw Statement行數
//	 * @param catchIdx	catch的index
//	 * @return			反白行數
//	 */
//	private int getThrowStatementSourceLine(int catchIdx) {
//		//反白行數
//		int selectLine = -1;
//
//		if (catchIdx != -1) {
//			ASTCatchCollect catchCollector = new ASTCatchCollect();
//			currentMethodNode.accept(catchCollector);
//			List<ASTNode> catchList = catchCollector.getMethodList();
//			//取得指定的Catch
//			CatchClause clause = (CatchClause) catchList.get(catchIdx);
//			//尋找Throw statement的行數
//			List catchStatements = clause.getBody().statements();
//			for (int i = 0; i < catchStatements.size(); i++) {
//				if (catchStatements.get(i) instanceof ThrowStatement) {
//					ThrowStatement statement = (ThrowStatement) catchStatements.get(i);
//					selectLine = this.actRoot.getLineNumber(statement.getStartPosition()) -1;
//					return selectLine;
//				}
//			}
//		}
//		return selectLine;
//	}
}
