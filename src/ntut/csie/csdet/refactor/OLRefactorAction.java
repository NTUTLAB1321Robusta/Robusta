package ntut.csie.csdet.refactor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.OverLoggingDetector;
import ntut.csie.rleht.builder.ASTMethodCollector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 取得OverLogging資訊 和 刪除OverLogging 的動作
 * @author Shiau
 */
public class OLRefactorAction {
	private static Logger logger = LoggerFactory.getLogger(OLRefactorAction.class);

	//Class(CompilationUnit)的相關資訊
	private IOpenable actOpenable = null;
	private CompilationUnit actRoot = null;
	//存放目前所要fix的method node
	private ASTNode currentMethodNode = null;
	//存放目前要進入的CatchNode
	private ASTNode currentCatchNode = null;
	//存放同一個Class內要fix的method
	private List<ASTNode> methodNodeList = new ArrayList<ASTNode>();
	//目前Method內的OverLogging
	private List<MarkerInfo> currentLoggingList = new ArrayList<MarkerInfo>();
	//存放同一個Class內要fix的method內，所出現的OverLogging
	private List<List<MarkerInfo>> loggingList = new ArrayList<List<MarkerInfo>>();
	
	/**
	 * 取得Class(CompilationUnit)的相關資訊
	 * @param resource
	 * @return			是否成功取得
	 */
	public boolean obtainResource(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {
			try {
				IJavaElement javaElement = JavaCore.create(resource);
				
				if (javaElement instanceof IOpenable)
					this.actOpenable = (IOpenable) javaElement;
				
				//Create AST to parse
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				this.actRoot = (CompilationUnit) parser.createAST(null);
				
				return true;
			}catch (Exception ex) {
				logger.error("[Find OL Method] EXCEPTION ",ex);
			}
		}
		return false;
	}
	
	/**
	 * 取得Method相關資訊(已知MethodIndex)
	 * @param methodIdx
	 */
	public void bindMethod(int methodIdx) {				
		//取得該class所有的method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		List<ASTNode> methodList = methodCollector.getMethodList();
		
		//取得目前要被修改的method node
		ASTNode tempNode = methodList.get(methodIdx);
		currentMethodNode = tempNode;

		if(tempNode != null) {
			//尋找該method內的OverLogging
			OverLoggingDetector loggingDetector = new OverLoggingDetector(this.actRoot, tempNode);
			loggingDetector.detect();
			//取得專案中OverLogging
			List<MarkerInfo> overLogggingTemp = loggingDetector.getOverLoggingList();
			currentLoggingList = overLogggingTemp;

			//若沒有OverLogging則不記錄
			if (overLogggingTemp.size() != 0) {
				this.methodNodeList.add(tempNode);
				this.loggingList.add(overLogggingTemp);
			}
		}
		
		//收集該method所有的catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<ASTNode> catchList = catchCollector.getMethodList();
		//刪除該Method內的Catch中的Logging Statement
		for (MarkerInfo msg : currentLoggingList) {
			//去比對startPosition,找出要修改的節點
			for (ASTNode cc : catchList) {
				if (cc.getStartPosition() == msg.getPosition()) {
					currentCatchNode = cc;
					break;
				}
			}
		}
	}
	
	/**
	 * 取得Method相關資訊(只知道哪個Method)
	 * @param method
	 */
	public void bindMethod(IMethod method) {
		//取得該class所有的method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		List<ASTNode> methodList = methodCollector.getMethodList();
		
		int methodIdx = -1;
		//若不曉得methodIndex就從methodList中去比較來取得
		if (method != null)
			methodIdx = findMethodIndex(method, methodList);

		//取得目前要被修改的method node
		ASTNode tempNode = methodList.get(methodIdx);
		currentMethodNode = tempNode;
		
		if(tempNode != null) {
			//尋找該method內的OverLogging
			OverLoggingDetector loggingDetector = new OverLoggingDetector(this.actRoot, tempNode);
			loggingDetector.detect();
			//取得專案中OverLogging
			List<MarkerInfo> overLogggingTemp = loggingDetector.getOverLoggingList();
			currentLoggingList = overLogggingTemp;

			//若沒有OverLogging則不記錄
			if (overLogggingTemp.size() != 0) {
				this.methodNodeList.add(tempNode);
				this.loggingList.add(overLogggingTemp);
			}
		}
	}
	
	/**
	 * 刪除Message
	 */
	public void deleteMessage(){
		try {
			//記錄修改
			actRoot.recordModifications();
			//若有複數個Method要修改
			for (int i=0; i < methodNodeList.size(); i++) {
				ASTNode currentMethodNode = methodNodeList.get(i);

				//收集該method所有的catch clause
				ASTCatchCollect catchCollector = new ASTCatchCollect();
				currentMethodNode.accept(catchCollector);
				List<ASTNode> catchList = catchCollector.getMethodList();
				//刪除該Method內的Catch中的Logging Statement
				for (MarkerInfo msg : loggingList.get(i)) {
					//去比對startPosition,找出要修改的節點
					for (ASTNode cc : catchList) {
						if (cc.getStartPosition() == msg.getPosition()) {
							//刪除Catch內的Logging Statement
							deleteCatchStatement(cc, msg);
							//寫回Edit中
							break;
						}
					}
				}
			}
			//將所要變更的內容寫回Edit中
			applyChange();			
		} catch (Exception ex) {
			logger.error("[Delete Message] EXCEPTION ",ex);
		}
	}
	
	/**
	 * 刪除Catch內有OverLogging Marker的Logging動作
	 * @param cc
	 * @param msg
	 */
	private void deleteCatchStatement(ASTNode cc, MarkerInfo msg){		
		CatchClause clause = (CatchClause)cc;
		//取得CatchClause所有的statement,將相關print例外資訊的東西移除
		List statementList = clause.getBody().statements();
		
		if (statementList.size() != 0) {
			//比對Catch內所有的Statement
			for (int i=0; i < statementList.size(); i++) {
				if (statementList.get(i) instanceof ExpressionStatement ) {
					ExpressionStatement statement = (ExpressionStatement) statementList.get(i);

					//若為選擇的行數，則刪除此行
					int line = actRoot.getLineNumber(statement.getStartPosition());
					if (line == msg.getLineNumber()) {
						statementList.remove(i);
					}
				}
			}
		}
	}
	
	/**
	 * 將所要變更的內容寫回Edit中
	 */
	private void applyChange(){
		try {
			ICompilationUnit icu = (ICompilationUnit) actOpenable;
			Document document = new Document(icu.getBuffer().getContents());

			TextEdit edits = actRoot.rewrite(document, icu.getJavaProject().getOptions(true));
			
			edits.apply(document);

			icu.getBuffer().setContents(document.get());
		} catch (BadLocationException e) {
			logger.error("[Delete Statement Exception] EXCEPTION ",e);
		} catch (JavaModelException ex) {
			logger.error("[Delete Statement Exception] EXCEPTION ",ex);
		}
	}
	
	/**
	 * 取得Method的Index
	 * @param callerMethod	被比較的Method
	 * @param methodList	該class中全部的Method
	 * @return				位於class中第幾個Method
	 */
	private int findMethodIndex(IMethod callerMethod, List<ASTNode> methodList) {
		//轉換成MethodDeclaration
		ASTNode methodNode = transMethodNode(callerMethod);

		//計算他位於Method List中哪一個index
		int methodIndex = -1;
		if (methodNode != null) {
			for (ASTNode method : methodList){
				methodIndex++;
				//與要找的程式是否相同
				if (method.toString().equals(methodNode.toString()))
					break;
			}
		}
		return methodIndex;
	}
	
	/**
	 * 轉換成ASTNode MethodDeclaration
	 * @param method
	 * @return
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;
		
		try {
			//Parser Jar檔時，會取不到ICompilationUnit
			if (method.getCompilationUnit() == null)
				return null;

			//產生AST
			ASTParser parserAST = ASTParser.newParser(AST.JLS3);
			parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
			parserAST.setSource(method.getCompilationUnit());
			parserAST.setResolveBindings(true);
			ASTNode ast = parserAST.createAST(null);

			//取得AST的Method部份
			ASTNode methodNode = NodeFinder.perform(ast, method.getSourceRange().getOffset(),
														method.getSourceRange().getLength());

			//若此ASTNode屬於MethodDeclaration，則轉型
			if(methodNode instanceof MethodDeclaration) {
				md = (MethodDeclaration) methodNode;
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] JavaModelException ", e);
		}

		return md;
	}

	//Class(CompilationUnit)的相關資訊
	public IOpenable getActOpenable() { return actOpenable; }
	public CompilationUnit getActRoot() { return actRoot; }
	//取得Method的相關資訊
	public ASTNode getCurrentCatchNode() {
		return currentCatchNode;
	}
	public ASTNode getCurrentMethodNode() {
		return currentMethodNode;
	}
	public ASTNode getMethodNode(int index) {
		return methodNodeList.get(index);
	}
	public List<MarkerInfo> getCurrentLoggingList() {
		return currentLoggingList;
	}
	public List<MarkerInfo> getLoggingList(int index) {
		return loggingList.get(index);
	}
	/**
	 * 現在這個Method內是否有OverLogging
	 * @return	是否有OverLogging
	 */
	public boolean isLoggingExist() {
		if (currentLoggingList == null || currentLoggingList.size() == 0)
			return false;
		return true;
	}
}
