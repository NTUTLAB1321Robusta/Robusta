package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.rewrite.ASTRewrite;
import org.eclipse.jdt.core.dom.rewrite.ListRewrite;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IMarkerResolution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UMQuickFix implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(UMQuickFix.class);
	
	private String label;
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	
	private IOpenable actOpenable;
	
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
				if(isok)
					addBigOuterTry(Integer.parseInt(msgIdx));
			}
		} catch (CoreException e) {		
			e.printStackTrace();
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
	        -  假如Main function中已經有try block了,那就要去確認try block是位於main的一開始
	            還是中間,或者是在最後面,並依據這三種情況將程式碼都塞進去try block裡面  
	        *-------------------------------------------------------------------------*/	
			moveTryBlock(ast,statement,pos,listRewrite);
		}else{
			 /*------------------------------------------------------------------------*
	        -  假如Main function中沒有try block了,那就自己增加一個try block,再把main中所有的
	            程式全部都塞進try block中
	        *-------------------------------------------------------------------------*/
			addNewTryBlock(ast,listRewrite);
		}		
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
			//將try block之後的程式碼move到try中
			originalRewrite.insertFirst(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(0), 
					(ASTNode) listRewrite.getRewrittenList().get(pos-1)), null);
			//將try block之前的程式碼move到try中
			originalRewrite.insertLast(listRewrite.createMoveTarget((ASTNode) listRewrite.getRewrittenList().get(pos+1), 
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
			ex.printStackTrace();
			logger.error("[UMQuickFix] EXCEPTION ",ex);
		}
	}
}
