package ntut.csie.csdet.quickfix;

import java.util.List;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.MainAnalyzer;
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
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IMarkerResolution;

public class UMQuickFix implements IMarkerResolution{

	private String label;
	//存放目前要修改的.java檔
	private CompilationUnit actRoot;
	
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	
	private IOpenable actOpenable;
	
	private List<CSMessage> currentExList = null;

	
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
		actRoot.recordModifications();
		
		//取得main function block中所有的statement
		MethodDeclaration md = (MethodDeclaration)currentMethodNode;
		List statement = md.getBody().statements();
		
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

		if(isTryExist){
			 /*------------------------------------------------------------------------*
	        -  假如Main function中已經有try block了,那就要去確認try block是位於main的一開始
	            還是中間,或者是在最後面,並依據這三種情況將程式碼都塞進去try block裡面  
	        *-------------------------------------------------------------------------*/	
			moveTryBlock(ast,statement,pos);
		}else{
			addNewTryBlock(ast,statement);
		}		
		applyChange();
	}
	
	/**
	 * 新增一個Try block區塊,並將在try之外的程式加入至Try block中
	 * @param ast
	 * @param statement
	 */
	private void addNewTryBlock(AST ast,List statement){
		 /*------------------------------------------------------------------------*
        -  假如Main function中沒有try block了,那就自己增加一個try block,再把main中所有的
            程式全部都塞進try block中
        *-------------------------------------------------------------------------*/
		TryStatement ts = ast.newTryStatement();
		//替try 加入一個Catch clause
		List catchStatement = ts.catchClauses();
		CatchClause cc = ast.newCatchClause();
		//建立catch的type為 catch(Exception ex)
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		catchStatement.add(cc);
		
		//取得剛剛建立的try block中的statement,並將在try之外的程式都移至try block之中
		List tryStatement = ASTNode.copySubtrees(ast, statement);	

		Block block = ts.getBody();
		for(int i=0;i<tryStatement.size();i++){
			//將每一行的程式加入至try block中
			block.statements().add(i, tryStatement.get(i));
		}
		//最後把原本在try block之外的程式都移除掉,因為都已經複製進try block中了
		statement.clear();
		statement.add(ts);
		

	}
	
	/**
	 * 假如原先main function就有try block,但有程式並未移至try block內的話就執行這個method
	 * @param ast
	 * @param statement
	 * @param pos
	 */
	private void moveTryBlock(AST ast,List statement,int pos){
		List copy = ASTNode.copySubtrees(ast, statement);
		//建立新的try 
		TryStatement ts = ast.newTryStatement(); 		
		Block block = ts.getBody();
		List tryStatement = block.statements();
		List catchStatement = ts.catchClauses();
		//建立新的catch
		CatchClause cc = ast.newCatchClause();		
		SingleVariableDeclaration sv = ast.newSingleVariableDeclaration();
		sv.setType(ast.newSimpleType(ast.newSimpleName("Exception")));
		sv.setName(ast.newSimpleName("ex"));
		cc.setException(sv);
		//將新建的catch加到try block中
		catchStatement.add(cc);
		
		//取得要copy的try
		TryStatement original = (TryStatement)copy.get(pos);
		List originalBlock = original.getBody().statements();
		statement.remove(pos);
		//取得要copy的catch
		List catchBlock = original.catchClauses();
		//取得要copy的finally
		Block FinalBlock = original.getFinally();
		if(pos == 0){
			System.out.println("【Enter pos=0】");
			//先新增原本try括號中的內容
			for(int i=0;i<originalBlock.size();i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) originalBlock.get(i)));
			}
			//再將本來在try之外的程式也複製一份到try的括號中
			for(int i=0;i<statement.size();i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) statement.get(i)));
			}
			
			//再將catch之內的內容也複製過來
			for(int i=0;i<catchBlock.size();i++){
				System.out.println("【Catch Block】===>"+catchBlock.get(i).toString());
				CatchClause cx =(CatchClause)catchBlock.get(i);
				List cxList = cx.getBody().statements();
				for(int x=0;x<cxList.size();x++){
					System.out.println("【Block Content】===>"+cxList.get(x).toString());
					cc.getBody().statements().add(ASTNode.copySubtree(cx.getBody().getAST(), (ASTNode) cxList.get(x)));	
				}				
			}
			
			//判斷原本main中的try是否有finally block,有的話就新增一個finally節點
			if(FinalBlock != null){
				Block realBlock = (Block) ASTNode.copySubtree(ast, FinalBlock);	
				ts.setFinally(realBlock);
			}
			
		}else if(pos == (copy.size()-1)){
			System.out.println("【Enter pos=size-1】");
			//假設main function最後一個程式是try block
			//先將try之前的程式都copy到新的try block中
			for(int i=0;i<statement.size();i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) statement.get(i)));
			}
			
			//將原本的try block的內容還原
			for(int i=0;i<originalBlock.size();i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) originalBlock.get(i)));
			}
			
			//再將catch之內的內容也複製過來
			for(int i=0;i<catchBlock.size();i++){
				System.out.println("【Catch Block】===>"+catchBlock.get(i).toString());
				CatchClause cx =(CatchClause)catchBlock.get(i);
				List cxList = cx.getBody().statements();
				for(int x=0;x<cxList.size();x++){
					System.out.println("【Block Content】===>"+cxList.get(x).toString());
					cc.getBody().statements().add(ASTNode.copySubtree(cx.getBody().getAST(), (ASTNode) cxList.get(x)));	
				}				
			}
			
			//判斷原本main中的try是否有finally block,有的話就新增一個finally節點
			if(FinalBlock != null){
				Block realBlock = (Block) ASTNode.copySubtree(ast, FinalBlock);	
				ts.setFinally(realBlock);
			}

		}else{

			//把Try block之前的程式copy近來
			for(int i=0;i<=statement.size()-pos;i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) statement.get(i)));
			}
			//將原本的try block的內容還原
			for(int i=0;i<originalBlock.size();i++){
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) originalBlock.get(i)));
			}

			//再將catch之內的內容也複製過來
			for(int i=0;i<catchBlock.size();i++){
				System.out.println("【Catch Block】===>"+catchBlock.get(i).toString());
				CatchClause cx =(CatchClause)catchBlock.get(i);
				List cxList = cx.getBody().statements();
				for(int x=0;x<cxList.size();x++){
					System.out.println("【Block Content】===>"+cxList.get(x).toString());
					cc.getBody().statements().add(ASTNode.copySubtree(cx.getBody().getAST(), (ASTNode) cxList.get(x)));	
				}			
			}
			
			//判斷原本main中的try是否有finally block,有的話就新增一個finally節點
			if(FinalBlock != null){
				Block realBlock = (Block) ASTNode.copySubtree(ast, FinalBlock);	
				ts.setFinally(realBlock);
			}
			//將Try block之後的內容繼續複製
			for(int i=statement.size()-pos+1;i<statement.size();i++){
//				System.out.println("Content==>"+statement.get(i).toString());
				tryStatement.add(ASTNode.copySubtree(ast, (ASTNode) statement.get(i)));
			}
		}
		//將原本main中的東西都清除掉
		statement.clear();
		//把新建立的try加入
		statement.add(ts);
	}
	
	
	/**
	 * 將要變更的資料寫回至Document中
	 */
	private void applyChange(){
		//寫回Edit中
		try {
			ICompilationUnit cu = (ICompilationUnit) actOpenable;
//			ICompilationUnit cu = (ICompilationUnit) actRoot.getJavaElement();
			Document document = new Document(cu.getBuffer().getContents());

			TextEdit edits = actRoot.rewrite(document, cu.getJavaProject().getOptions(true));
			edits.apply(document);

			cu.getBuffer().setContents(document.get());

		}catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
