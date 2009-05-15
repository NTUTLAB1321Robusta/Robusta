package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.EditorUtils;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.CatchClause;
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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agile.exception.RL;
import agile.exception.Robustness;


/**
 * 在Marker上面的Quick Fix中加入Throw Checked Exception的功能
 * @author Shiau
 */
public class TEQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(TEQuickFix.class);

	private String label;
	//紀錄code smell的type
	private String problem;

	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	// 目前method的RL Annotation資訊
	private List<RLMessage> currentMethodRLList = null;
	
	//是否要加RL Annotation，否則已存在
	private boolean isAddAnnotation = true;
	
	boolean isImportRobustnessClass = false;
	boolean isImportRLClass = false;
	//按下QuickFix該行的程式起始位置(Catch位置)
	private String srcPos;
	//刪掉的Statement數目
	private int delStatement = 0;

	private IOpenable actOpenable;
	
	public TEQuickFix(String label)
	{
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
								  (problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION))){
				//如果碰到dummy handler,則將exception rethrow
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				String exception = marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION).toString();
				//儲存按下QuickFix該行的程式起始位置
				this.srcPos = marker.getAttribute(RLMarkerAttribute.RL_INFO_SRC_POS).toString();

				boolean isok = findDummyMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok)
					rethrowException(exception,Integer.parseInt(msgIdx));
			}
			
		} catch (CoreException e) {
			logger.error("[TEQuickFix] EXCEPTION ",e);
		}
	}
	
	private boolean findDummyMethod(IResource resource, int methodIdx) {
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			try {
				IJavaElement javaElement = JavaCore.create(resource);
	
				if (javaElement instanceof IOpenable) {
					this.actOpenable = (IOpenable) javaElement;
				}
				//Create AST to parse
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
	
				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				this.actRoot = (CompilationUnit) parser.createAST(null);

				//取得該class所有的method
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				actRoot.accept(methodCollector);
				List<ASTNode> methodList = methodCollector.getMethodList();
				
				//取得目前要被修改的method node
				this.currentMethodNode = methodList.get(methodIdx);
				if(currentMethodNode != null){
					//取得這個method的RL資訊
					ExceptionAnalyzer exVisitor = new ExceptionAnalyzer(this.actRoot, currentMethodNode.getStartPosition(), 0);
					currentMethodNode.accept(exVisitor);
					currentMethodRLList = exVisitor.getMethodRLAnnotationList();
				}
				return true;
			}catch (Exception ex) {
				logger.error("[Find DH Method] EXCEPTION ",ex);
			}
		}
		return false;
	}

	/**
	 * 將該method Throw Checked Exception
	 * @param exception
	 */
	private void rethrowException(String exception, int msgIdx) {
		
		actRoot.recordModifications();
		AST ast = currentMethodNode.getAST();

		//準備在Catch Caluse中加入throw exception
		//收集該method所有的catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<ASTNode> catchList = catchCollector.getMethodList();
		
		for (ASTNode cc : catchList){
			//找到該Catch(如果Catch的位置與按下Quick那行的起始位置相同)
			if (cc.getStartPosition() == Integer.parseInt(srcPos))
			{
				//建立RL Annotation
				addAnnotationRoot(exception,ast);
				//在catch clause中建立throw statement
				addThrowStatement(cc, ast);
				//檢查在method前面有沒有throw exception
				checkMethodThrow(ast,exception);
				//寫回Edit中
				applyChange(cc);
				break;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void addAnnotationRoot(String exception,AST ast) {
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
		
		if(currentMethodRLList.size() == 0){		
			rlary.expressions().add(getRLAnnotation(ast,1,exception));
		}else{
			isAddAnnotation = false;
			for (RLMessage rlmsg : currentMethodRLList) {
				//把舊的annotation加進去
				//判斷如果遇到重複的就不要加annotation
				int pos = rlmsg.getRLData().getExceptionType().toString().lastIndexOf(".");
				String cut = rlmsg.getRLData().getExceptionType().toString().substring(pos+1);

				if((!cut.equals(exception)) && (rlmsg.getRLData().getLevel() == 1)){					
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

	private void addImportDeclaration() {
		// 判斷是否已經Import Robustness及RL的宣告
		List<ImportDeclaration> importList = this.actRoot.imports();

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
	 * 在catch中增加throw new RuntimeException(..)
	 * @param cc
	 * @param ast
	 */
	private void addThrowStatement(ASTNode cc, AST ast) {
		//取得該catch()中的exception variable
		SingleVariableDeclaration svd = 
			(SingleVariableDeclaration) cc.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);

		CatchClause clause = (CatchClause)cc;

		//自行建立一個throw statement加入
		ThrowStatement ts = ast.newThrowStatement();

		//取得Catch後Exception的變數
		SimpleName name = ast.newSimpleName(svd.resolveBinding().getName());		
		
		//加到throw statement
		ts.setExpression(name);

		//取得CatchClause所有的statement,將相關print例外資訊的東西移除
		List statement = clause.getBody().statements();

		delStatement = statement.size();
		if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)){	
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
	private void deleteStatement(List<Statement> statementTemp){
		// 從Catch Clause裡面剖析兩種情形
		if(statementTemp.size() != 0){
			for(int i=0;i<statementTemp.size();i++){			
				if(statementTemp.get(i) instanceof ExpressionStatement ){
					ExpressionStatement statement = (ExpressionStatement) statementTemp.get(i);
					// 遇到System.out.print or printStackTrace就把他remove掉
					if(statement.getExpression().toString().contains("System.out.print")||
							statement.getExpression().toString().contains("printStackTrace")){	
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
	 * @param ast
	 * @param exception 
	 */
	private void checkMethodThrow(AST ast, String exception){
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		List thStat = md.thrownExceptions();
		boolean isExist = false;
		for(int i=0;i<thStat.size();i++){
			if(thStat.get(i) instanceof SimpleName){
				SimpleName sn = (SimpleName)thStat.get(i);
				if(sn.getIdentifier().equals(exception)){
					isExist = true;
					break;
				}
			}
		}
		if(!isExist)
			thStat.add(ast.newSimpleName(exception));
	}

	/**
	 * 將所要變更的內容寫回Edit中
	 * @param node
	 */
	private void applyChange(ASTNode node) {
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
	
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
//			TextEdit edits = rewrite.rewriteAST(document,null);
			edits.apply(document);
			
			cu.getBuffer().setContents(document.get());
			
			//取得目前的EditPart
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;
			
			CatchClause cc = (CatchClause)node;
			List catchSt = cc.getBody().statements();
			if(catchSt != null){
				int numLine=0;
				//throw之前沒有statement表示ignore
				if (catchSt.size()<2)
					//加在catch之後一格
					numLine = this.actRoot.getLineNumber(cc.getStartPosition());
				else
				{
					//取得throw的前一筆Statement
					ASTNode throwNode = (ASTNode)catchSt.get(catchSt.size()-2);

					//TODO throw定位會抓不準，1.throw前有註解，2.throw前一筆到throw之間有要被刪除的statement
					//用Method起點位置取得Method位於第幾行數(起始行數從0開始，不是1，所以減1)
					numLine = this.actRoot.getLineNumber(throwNode.getStartPosition());
				}

				//如果有import Robustness或RL的宣告行數就加1
				if(!isImportRobustnessClass)
					numLine++;
				if(!isImportRLClass)
					numLine++;
				//若有加Annotation則行數加1
				if(isAddAnnotation)
					numLine++;

				//取得行數的資料
				IRegion lineInfo = null;
				try {
					lineInfo = document.getLineInformation(numLine - delStatement);
				} catch (BadLocationException e) {
					logger.error("[BadLocation] EXCEPTION ",e);
				}
				//反白該行 在Quick fix完之後,可以將游標定位在Quick Fix那行
				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			}
		} catch (BadLocationException e) {
			logger.error("[Rethrow Exception] EXCEPTION ",e);
		} catch (JavaModelException ex) {
			logger.error("[Rethrow Exception] EXCEPTION ",ex);
		}
	}
}
