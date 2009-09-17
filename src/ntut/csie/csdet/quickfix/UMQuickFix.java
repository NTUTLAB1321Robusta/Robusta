package ntut.csie.csdet.quickfix;

import java.util.List;

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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IExtendedModifier;
import org.eclipse.jdt.core.dom.ImportDeclaration;
import org.eclipse.jdt.core.dom.MemberValuePair;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NormalAnnotation;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
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
import agile.exception.Smell;
import agile.exception.SuppressSmell;

public class UMQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(UMQuickFix.class);
	
	private String label;
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	// 目前method的RL Annotation資訊
	private List<RLMessage> currentMethodRLList = null;
	
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	
	private IOpenable actOpenable;
	
//	private String exType = "Exception";
	private String exType = "java.lang.Exception";
	
	private ASTRewrite rewrite;
	
	public UMQuickFix(String label){
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
			if(problem != null && problem.equals(RLMarkerAttribute.CS_UNPROTECTED_MAIN)){
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				//String exception = marker.getAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION).toString();
				boolean isok = unprotectedMain(marker.getResource(), Integer.parseInt(methodIdx));
				//先找出要被修改的main function,如果存在就開始進行fix
				if(isok) {
					addBigOuterTry(Integer.parseInt(msgIdx));
					//反白Annotation
					selectSourceLine(marker, methodIdx);
				}
			}
		} catch (CoreException e) {		
			logger.error("[UMQuickFix] EXCEPTION ",e);
		}		
	}
	
	/**
	 * 找出要被修改的main function
	 * @param resource
	 * @param methodIdx
	 * @return
	 */
	private boolean unprotectedMain(IResource resource, int methodIdx){
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
					return true;
				}
				
			}catch(Exception ex){
				ex.printStackTrace();
				logger.error("[UMQuickFix] EXCEPTION ",ex);
			}
		}		
		return false;
	}
	
	/**
	 * 針對Code smell進行修改,增加一個Big try block
	 * @param msgIdx
	 */
	private void addBigOuterTry(int msgIdx){
		AST ast = actRoot.getAST();
		rewrite = ASTRewrite.create(actRoot.getAST());
		//actRoot.recordModifications();
		
		//取得main function block中所有的statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		Block mdBlock = md.getBody();
		List statement = mdBlock.statements();

		boolean isTryExist = false;
		int pos = -1;

		for(int i=0;i<statement.size();i++){
			if(statement.get(i) instanceof TryStatement){
				//假如Main function中有try就標示為true
				pos = i; //try block的位置
				isTryExist = true;
				break;
			}
		}
		
		ListRewrite listRewrite = rewrite.getListRewrite(mdBlock,Block.STATEMENTS_PROPERTY);
		
		if(isTryExist){
			/*------------------------------------------------------------------------*
			-	假如Main function中已經有try block了,那就要去確認try block是位於main的一開始
				還是中間,或者是在最後面,並依據這三種情況將程式碼都塞進去try block裡面  
	        *-------------------------------------------------------------------------*/	
			moveTryBlock(ast,statement,pos,listRewrite);
			
		}else{
			/*------------------------------------------------------------------------*
			-	假如Main function中沒有try block了,那就自己增加一個try block,再把main中所有的
				程式全部都塞進try block中
	        *-------------------------------------------------------------------------*/
			addNewTryBlock(ast,listRewrite);
			
		}	
		addAnnotationRoot(ast);		
		applyChange();
	}
	
	/**
	 * 新增一個Try block,並把相關的程式碼加進去Try中
	 * @param ast
	 * @param listRewrite
	 */
	private void addNewTryBlock(AST ast,ListRewrite listRewrite){
		TryStatement ts = ast.newTryStatement();
		
		//替try 加入一個Catch clause
		List catchStatement = ts.catchClauses();
		CatchClause cc = ast.newCatchClause();
		ListRewrite catchRewrite = rewrite.getListRewrite(cc.getBody(),Block.STATEMENTS_PROPERTY);
		
		//建立catch的type為 catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		
		//在Catch中加入todo的註解
		StringBuffer comment = new StringBuffer();
		comment.append("//TODO: handle exception");
		ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.EMPTY_STATEMENT);
		catchRewrite.insertLast(placeHolder, null);
		catchStatement.add(cc);
		
		//將原本在try block之外的程式都移動進來
		ListRewrite tryStatement = rewrite.getListRewrite(ts.getBody(),Block.STATEMENTS_PROPERTY);
		int listSize = listRewrite.getRewrittenList().size();
		tryStatement.insertLast(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
									(ASTNode) listRewrite.getRewrittenList().get(listSize-1)), null);
		//將新增的try statement加進來
		listRewrite.insertLast(ts, null);		
	}
	
	/**	
	 * 新增一個Try block區塊,並將在try之外的程式加入至Try block中
	 * @param ast
	 * @param statement
	 * @param pos : try的位置
	 * @param listRewrite
	 */
	private void moveTryBlock(AST ast,List statement,int pos,ListRewrite listRewrite){
		TryStatement original = (TryStatement)statement.get(pos);
		ListRewrite originalRewrite = rewrite.getListRewrite(original.getBody(),Block.STATEMENTS_PROPERTY);
	
		//假如Try block之後還有程式碼,就複製進去try block之內
		int totalSize = statement.size();
		if(pos == 0){
			//假如try block在最一開始
			if(totalSize > 1){
				//將try block之後的程式碼move到try中
				originalRewrite.insertLast(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(1), 
						(ASTNode) listRewrite.getRewrittenList().get(totalSize-1)), null);
			}			
		}else if(pos == listRewrite.getRewrittenList().size()-1){
			//假如try block在結尾	
			if(pos > 0 ){
				//將try block之前的程式碼move到try中
				originalRewrite.insertFirst(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
						(ASTNode) listRewrite.getRewrittenList().get(pos-1)), null);
			}
		}else{
			//假如try block在中間
			//先將try block之前的程式碼move到try中
			originalRewrite.insertFirst(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
					(ASTNode) listRewrite.getRewrittenList().get(pos-1)), null);
			//將try block之後的程式碼move到try中
			//由於在把try之前的東西移進新的try block之後,list的位置會更動,try的位置會跑到最前面,所以從1開始複製			
			totalSize = totalSize - pos;
			originalRewrite.insertLast(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(1), 
					(ASTNode) listRewrite.getRewrittenList().get(totalSize-1)), null);
		}		
		addCatchBody(ast,original);
	}
	
	/**
	 * 如果沒有catch(Exception e)的話要加進去
	 * @param ast
	 * @param original
	 */
	private void addCatchBody(AST ast,TryStatement original){
		//利用此變數來判斷原本main中的catch exception型態是否為catch(Exception e...)
		boolean isException = false;
		List catchStatement = original.catchClauses();
		for(int i=0;i<catchStatement.size();i++){
			CatchClause temp = (CatchClause)catchStatement.get(i);
			SingleVariableDeclaration svd = (SingleVariableDeclaration) temp
			.getStructuralProperty(CatchClause.EXCEPTION_PROPERTY);
			if(svd.getType().toString().equals("Exception")){
				//假如有找到符合的型態,就把變數設成true
				isException = true;			
				//在Catch中加入todo的註解
				StringBuffer comment = new StringBuffer();
				comment.append("//TODO: handle exception");
				ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.RETURN_STATEMENT);
				ListRewrite todoRewrite = rewrite.getListRewrite(temp.getBody(),Block.STATEMENTS_PROPERTY);
				todoRewrite.insertLast(placeHolder, null);
			}
		}
		
		if(!isException){
			//建立新的catch(Exception ex)	
			ListRewrite catchRewrite = rewrite.getListRewrite(original, TryStatement.CATCH_CLAUSES_PROPERTY);
			CatchClause cc = ast.newCatchClause();		
			SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
			sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
			sv.setName(ast.newSimpleName("exxxxx"));
			cc.setException(sv);
			catchRewrite.insertLast(cc, null);
			//在Catch中加入todo的註解
			StringBuffer comment = new StringBuffer();
			comment.append("//TODO: handle exception");
			ASTNode placeHolder = rewrite.createStringPlaceholder(comment.toString(), ASTNode.RETURN_STATEMENT);
			ListRewrite todoRewrite = rewrite.getListRewrite(cc.getBody(),Block.STATEMENTS_PROPERTY);
			todoRewrite.insertLast(placeHolder, null);
		}
	}

	private void addAnnotationRoot(AST ast){
		//要建立@Robustness(value={@RL(level=1, exception=java.lang.Exception.class)})這樣的Annotation
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
				if((!rlmsg.getRLData().getExceptionType().toString().equals(exType)) && (rlmsg.getRLData().getLevel() == 1)){	
					rlary.expressions().add(
							getRLAnnotation(ast, rlmsg.getRLData().getLevel(), rlmsg.getRLData().getExceptionType()));	
				}				
			}
			rlary.expressions().add(getRLAnnotation(ast,1,exType));
			
			ListRewrite listRewrite = rewrite.getListRewrite(method, method.getModifiersProperty());
			List<IExtendedModifier> modifiers = listRewrite.getRewrittenList();
			for(IExtendedModifier mdf : modifiers){
				if(mdf.isAnnotation() && mdf.toString().indexOf("Robustness") != -1){
					listRewrite.remove((ASTNode)mdf, null);
				}
			}
		}
		
		if (rlary.expressions().size() > 0) {
			ListRewrite listRewrite = rewrite.getListRewrite(method, method.getModifiersProperty());
			listRewrite.insertAt(root, 0, null);
		}
		//將RL的library加進來
		addImportDeclaration();
	}
	
	/**
	 * 產生RL Annotation之RL資料
	 * @param ast:AST Object
	 * @param levelVal:強健度等級
	 * @param exClass:例外類別
	 * @return NormalAnnotation AST Node
	 */
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

		//exception=java.lang.RuntimeException.class
		MemberValuePair exception = ast.newMemberValuePair();
		exception.setName(ast.newSimpleName("exception"));
		TypeLiteral exclass = ast.newTypeLiteral();
		//預設為Exception
		exclass.setType(ast.newSimpleType(ast.newName(excption)));
		exception.setValue(exclass);
		rl.values().add(exception);

		return rl;
	}
	
	private void addImportDeclaration() {
		// 判斷是否已經Import Robustness及RL的宣告
		ListRewrite listRewrite = rewrite.getListRewrite(this.actRoot, this.actRoot.IMPORTS_PROPERTY);
		List<ImportDeclaration> importList = listRewrite.getRewrittenList();
		
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
			listRewrite.insertLast(imp, null);
		}
		if (!isImportRLClass) {
			ImportDeclaration imp = rootAst.newImportDeclaration();
			imp.setName(rootAst.newName(RL.class.getName()));
			listRewrite.insertLast(imp, null);
		}
	}
	
	/**
	 * 將要變更的資料寫回至Document中
	 */
	private void applyChange(){
		//寫回Edit中
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
			Document document = new Document(cu.getBuffer().getContents());
			TextEdit edits = rewrite.rewriteAST(document,null);
			edits.apply(document);
			cu.getBuffer().setContents(document.get());
		}catch (Exception ex) {
			logger.error("[UMQuickFix] EXCEPTION ",ex);
		}
	}
	
	/**
	 * 反白Annotation那行
	 * @param document
	 */
	private void selectSourceLine(IMarker marker, String methodIdx) {
		//重新取得Method資訊
		boolean isOK = unprotectedMain(marker.getResource(), Integer.parseInt(methodIdx));
		if (isOK) {
			try {
				ICompilationUnit cu = (ICompilationUnit) actOpenable;
				Document document = new Document(cu.getBuffer().getContents());
				//取得目前的EditPart
				IEditorPart editorPart = EditorUtils.getActiveEditor();
				ITextEditor editor = (ITextEditor) editorPart;
		
				//取得Method的起點位置
				int srcPos = currentMethodNode.getStartPosition();
				//用Method起點位置取得Method位於第幾行數，行數起始位置從0開始(不是1)，所以減1
				//又第一行"必定"是Robustness，所以取下一行
				int selectLine = this.actRoot.getLineNumber(srcPos);

				//取得行數的資料
				IRegion lineInfo = document.getLineInformation(selectLine);
	
				//反白該行 在Quick fix完之後,可以將游標定位在Quick Fix那行
				editor.selectAndReveal(lineInfo.getOffset(), lineInfo.getLength());
			} catch (JavaModelException e) {
				logger.error("[Rethrow checked Exception] EXCEPTION ",e);
			} catch (BadLocationException e) {
				logger.error("[BadLocation] EXCEPTION ",e);
			}
		}
	}
}
