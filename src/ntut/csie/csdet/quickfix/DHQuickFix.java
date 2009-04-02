package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
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
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import agile.exception.RL;
import agile.exception.Robustness;

/**
 * 提供給Ignore Ex與Dummy handler的解法
 * @author chewei
 */

public class DHQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(DHQuickFix.class);
	
	private String label;
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	
	private IOpenable actOpenable;
	
	//紀錄所找到的code smell list
	private List<CSMessage> currentExList = null;
	
	// 目前method的RL Annotation資訊
	private List<RLMessage> currentMethodRLList = null;
	
	//紀錄code smell的type
	private String problem;
	
	private String exType = "RuntimeException";
	
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
					(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION))){
				//如果碰到dummy handler,則將exception rethrow
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				String exception = marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION).toString();
				boolean isok = findDummyMethod(marker.getResource(), Integer.parseInt(methodIdx));
				if(isok)
					rethrowException(exception,Integer.parseInt(msgIdx));
			}
			
		} catch (CoreException e) {
			logger.error("[DHQuickFix] EXCEPTION ",e);
		}
		
	}
	
	private boolean findDummyMethod(IResource resource, int methodIdx){
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
					//找出這個method的code smell
					CodeSmellAnalyzer visitor = new CodeSmellAnalyzer(this.actRoot);
					currentMethodNode.accept(visitor);
					if(problem.equals(RLMarkerAttribute.CS_INGNORE_EXCEPTION)){
						currentExList = visitor.getIgnoreExList();	
					}else{
						currentExList = visitor.getDummyList();
					}
				}				
				return true;			
			}catch (Exception ex) {
				logger.error("[Find DH Method] EXCEPTION ",ex);
			}
		}
		return false;
	}
	
	/**
	 * 將該method rethrow unchecked exception
	 * @param exception
	 */
	private void rethrowException(String exception,int msgIdx){
		try {
		
			actRoot.recordModifications();
			AST ast = currentMethodNode.getAST();
		
			//準備在Catch Caluse中加入throw exception
			//取得Code smell的資訊
			CSMessage msg = currentExList.get(msgIdx);
			System.out.println("【Msg Idx exception type】===>"+msg.getExceptionType());
			//收集該method所有的catch clause
			ASTCatchCollect catchCollector = new ASTCatchCollect();
			currentMethodNode.accept(catchCollector);
			List<ASTNode> catchList = catchCollector.getMethodList();
			
			//去比對startPosition,找出要修改的節點			
			for (ASTNode cc : catchList){
				System.out.println("【All Catch Line num】===>"+this.actRoot.getLineNumber(cc.getStartPosition()));
				if(cc.getStartPosition() == msg.getPosition()){					
					//建立RL Annotation
					addAnnotationRoot(ast);					
					//在catch clause中建立throw statement
					addThrowStatement(cc, ast);
					break;
				}
			}

			//寫回Edit中
			applyChange(msg);
		}catch (Exception ex) {
			logger.error("[Rethrow Exception] EXCEPTION ",ex);
		}
		
	}
	
	/**
	 * 在catch中增加throw new RuntimeException(..)
	 * @param cc
	 * @param ast
	 */
	private void addThrowStatement(ASTNode cc,AST ast){
		//取得該catch()中的exception variable
		SingleVariableDeclaration svd = (SingleVariableDeclaration) cc
		.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
		CatchClause clause = (CatchClause)cc;
		//自行建立一個throw statement加入
		ThrowStatement ts = ast.newThrowStatement();
		//將throw的variable傳入
		ClassInstanceCreation cic = ast.newClassInstanceCreation();
		//throw new RuntimeException()
		cic.setType(ast.newSimpleType(ast.newSimpleName(exType)));
		//將throw new RuntimeException(ex)括號中加入參數 
		cic.arguments().add(ast.newSimpleName(svd.resolveBinding().getName()));
		
		//取得CatchClause所有的statement,將相關print例外資訊的東西移除
		List<Statement> statement = clause.getBody().statements();
		if(problem.equals(RLMarkerAttribute.CS_DUMMY_HANDLER)){	
			//假如要fix的code smell是dummy handler,就要把catch中的列印資訊刪除
			deleteStatement(statement);
		}
		//將新建立的節點寫回
		ts.setExpression(cic);
		statement.add(ts);	
	}
	
	
	@SuppressWarnings("unchecked")
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
		if(currentMethodRLList.size() == 0){		
			rlary.expressions().add(getRLAnnotation(ast,1,exType));
		}else{
			for (RLMessage rlmsg : currentMethodRLList) {
				//把舊的annotation加進去
				//判斷如果遇到重複的就不要加annotation
				if((!rlmsg.getRLData().getExceptionType().toString().contains(exType)) && (rlmsg.getRLData().getLevel() != 1)){					
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
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
	 * 將所要變更的內容寫回Edit中
	 * @param msg
	 */
	private void applyChange(CSMessage msg){
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
	
			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			edits.apply(document);
			
			cu.getBuffer().setContents(document.get());
			
			//取得目前的EditPart
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;
	
			//利用document取得定位點(要加1是因為取到的那行是標marker那行)
			int offset = document.getLineOffset(msg.getLineNumber());
			//在Quick fix完之後,可以將游標定位在Quick Fix那行
			//TODO 可以將Fix的那行給highlight起來,但要先取得length,暫時先把長度固定
			EditorUtils.selectInEditor(editor,offset,40);
		} catch (BadLocationException e) {
			logger.error("[Rethrow Exception] EXCEPTION ",e);
		} catch (JavaModelException ex) {
			logger.error("[Rethrow Exception] EXCEPTION ",ex);
		}	
	}
	
	@SuppressWarnings("unchecked")
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
			imp.setName(rootAst.newName(RL.class.getName()));
			this.actRoot.imports().add(imp);
		}
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
}
