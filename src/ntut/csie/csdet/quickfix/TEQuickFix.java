package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;
import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CatchClause;
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
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 在Marker上面的Quick Fix中加入直接Throw Checked Exception的功能
 * @author Shiau
 */
public class TEQuickFix extends BaseQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(TEQuickFix.class);

	private String label;
	//紀錄code smell的type
	private String problem;

	// 目前method的RL Annotation資訊
	private List<RLMessage> currentMethodRLList = null;

	//按下QuickFix該行的程式起始位置(Catch位置)
	private String srcPos;
	//刪掉的Statement數目
	private int delStatement = 0;

	public TEQuickFix(String label) {
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
				String exception = marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION).toString();
				//儲存按下QuickFix該行的程式起始位置
				srcPos = marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString();

				boolean isok = this.findCurrentMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok) {
					currentMethodRLList = findRLList();
					
					//將Method加入Throw Exception，並回傳Catch的Index
					int catchIdx = rethrowException(exception,Integer.parseInt(msgIdx));

					//調整RL Annotation順序 TODO 待修正
					//new RLOrderFix().run(marker.getResource(), methodIdx, msgIdx);
					//反白指定行數 (暫時不需要反白行數)
					//selectSourceLine(marker, methodIdx, catchIdx);
				}
			}
		} catch (CoreException e) {
			logger.error("[TEQuickFix] EXCEPTION ",e);
		}
	}

	/**
	 * 取得RL Annotation List
	 * @param resource		來源
	 * @param methodIdx		Method的Index
	 * @return				是否成功
	 */
	private List<RLMessage> findRLList() {
		if (currentMethodNode != null) {
			//取得這個method的RL資訊
			ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
			currentMethodNode.accept(exVisitor);
			return exVisitor.getMethodRLAnnotationList();
		}
		return null;
	}
	
	/**
	 * 將該method Throw Checked Exception
	 * @param exception
	 * @param msgIdx
	 * @return				
	 */
	private int rethrowException(String exception, int msgIdx) {
		AST ast = currentMethodNode.getAST();

		//準備在Catch Caluse中加入throw exception
		//收集該method所有的catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<CatchClause> catchList = catchCollector.getMethodList();

		for (int i = 0; i < catchList.size(); i++) {
			//找到該Catch(如果Catch的位置與按下Quick那行的起始位置相同)
			if (catchList.get(i).getStartPosition() == Integer.parseInt(srcPos)) {
				//建立RL Annotation
				addAnnotationRoot(exception, ast);
				//在catch clause中建立throw statement
				addThrowStatement(catchList.get(i), ast);
				//檢查在method前面有沒有throw exception
				checkMethodThrow(ast, exception);
				//寫回Edit中
				this.applyChange();
				return i;
			}
		}
		return -1;
	}

	@SuppressWarnings("unchecked")
	private void addAnnotationRoot(String exception,AST ast) {
		//要建立@Robustness(value={@Tag(level=1, exception=java.lang.RuntimeException.class)})這樣的Annotation
		//建立Annotation root
		NormalAnnotation root = ast.newNormalAnnotation();
		root.setTypeName(ast.newSimpleName("Robustness"));

		MemberValuePair value = ast.newMemberValuePair();
		value.setName(ast.newSimpleName("value"));
		root.values().add(value);
		ArrayInitializer rlary = ast.newArrayInitializer();
		value.setValue(rlary);

		MethodDeclaration method = (MethodDeclaration) currentMethodNode;		
		
		if(currentMethodRLList.size() == 0) {		
			rlary.expressions().add(getRLAnnotation(ast,1,exception));
		}else{
			for (RLMessage rlmsg : currentMethodRLList) {
				//把舊的annotation加進去
				//判斷如果遇到重複的就不要加annotation
				int pos = rlmsg.getRLData().getExceptionType().toString().lastIndexOf(".");
				String cut = rlmsg.getRLData().getExceptionType().toString().substring(pos+1);

				if((!cut.equals(exception)) && (rlmsg.getRLData().getLevel() == 1)) {					
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
				}
			}
			rlary.expressions().add(getRLAnnotation(ast,1,exception));

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
	private NormalAnnotation getRLAnnotation(AST ast, int levelVal,String excption) {
		//要建立@Robustness(value={@Tag(level=1, exception=java.lang.RuntimeException.class)})這樣的Annotation
		NormalAnnotation rl = ast.newNormalAnnotation();
		rl.setTypeName(ast.newSimpleName("Tag"));

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

	private void addImportDeclaration() {
		// 判斷是否已經Import Robustness及RL的宣告
		List<ImportDeclaration> importList = this.actRoot.imports();
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
			imp.setName(rootAst.newName(RTag.class.getName()));
			this.actRoot.imports().add(imp);
		}
	}

	/**
	 * 在catch中增加throw checked exception
	 * 
	 * @param cc
	 * @param ast
	 */
	private void addThrowStatement(CatchClause cc, AST ast) {
		//取得該catch()中的exception variable
		SingleVariableDeclaration svd = 
			(SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

		//自行建立一個throw statement加入
		ThrowStatement ts = ast.newThrowStatement();

		//取得Catch後Exception的變數
		SimpleName name = ast.newSimpleName(svd.resolveBinding().getName());		
		
		//加到throw statement
		ts.setExpression(name);

		//取得CatchClause所有的statement,將相關print例外資訊的東西移除
		List statement = cc.getBody().statements();

		delStatement = statement.size();
		if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)) {	
			//假如要fix的code smell是dummy handler,就要把catch中的列印資訊刪除
			deleteStatement(statement);
		}
		delStatement -= statement.size();

		//將新建立的節點寫回
		statement.add(ts);
	}

	/**
	 * 在Rethrow之前,先將相關的print字串都清除掉
	 */
	private void deleteStatement(List<Statement> statementTemp) {
		// 從Catch Clause裡面剖析兩種情形
		if(statementTemp.size() != 0){
			for(int i=0;i<statementTemp.size();i++) {			
				if(statementTemp.get(i) instanceof ExpressionStatement ) {
					ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);
					// 遇到System.out.print or printStackTrace就把他remove掉
					if (statement.getExpression().toString().contains("System.out.print") ||
						statement.getExpression().toString().contains("printStackTrace")) {

						statementTemp.remove(i);
						//移除完之後ArrayList的位置會重新調整過,所以利用遞回來繼續往下找符合的條件並移除
						deleteStatement(statementTemp);
					}
				}
			}
		}
	}

	/**
	 * 檢查在method前面有沒有throw exception
	 * 
	 * @param ast
	 * @param exception 
	 */
	private void checkMethodThrow(AST ast, String exception) {
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		List thStat = md.thrownExceptions();
		boolean isExist = false;
		for(int i=0;i<thStat.size();i++) {
			if(thStat.get(i) instanceof SimpleName) {
				SimpleName sn = (SimpleName)thStat.get(i);
				if(sn.getIdentifier().equals(exception)) {
					isExist = true;
					break;
				}
			}
		}
		if (!isExist) {
			thStat.add(ast.newSimpleName(exception));
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
//				}
//			}
//		}
//		return selectLine;
//	}
}
