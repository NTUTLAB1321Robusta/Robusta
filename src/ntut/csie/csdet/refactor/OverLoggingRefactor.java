package ntut.csie.csdet.refactor;

import java.util.ArrayList;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.visitor.ASTCatchCollect;
import ntut.csie.csdet.visitor.OverLoggingDetector;
import ntut.csie.csdet.visitor.OverLoggingVisitor;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
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
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.TextEdit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverLoggingRefactor {
	private static Logger logger = LoggerFactory.getLogger(OverLoggingRefactor.class);
	private IMarker marker = null;
	// Class(CompilationUnit)的相關資訊
	private IOpenable actOpenable = null;
	private CompilationUnit actRoot = null;
	// 存放目前所要fix的method node
	private MethodDeclaration currentMethodNode = null;
	// 存放同一個Class內要fix的method
	private List<MethodDeclaration> methodNodeList = new ArrayList<MethodDeclaration>();
	// 目前Method內的OverLogging
	private List<MarkerInfo> currentLoggingList = new ArrayList<MarkerInfo>();
	// 存放同一個Class內要fix的method內，所出現的OverLogging
	private List<List<MarkerInfo>> loggingList = new ArrayList<List<MarkerInfo>>();
	private List<CompilationUnit> unitList = new ArrayList<CompilationUnit>();
	
	public void refator(IMarker marker) {
		try {
			// 取得Marker的資訊
			String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
			if(obtainResource(marker.getResource())) {
				// 取得Method相關資訊
				bindMethod(Integer.parseInt(methodIdx));
				// 加入至刪除名單
				unitList.add(actRoot);
				// 取得Method的Logging資訊
				traceCallerMethod(methodNodeList.get(0));
				traceCalleeMethod(methodNodeList.get(0));
				// 若更動CompilationUnit後再更動會出錯
				// 所以把所有要刪除的Logging都記錄起來，再一次刪除
				for (CompilationUnit unit : unitList) {
					// 刪除List中的OverLogging (一次以一個Class為單位刪除)
					deleteMessage(unit);
					// 將所要變更的內容寫回Edit中
					applyChange();
				}
			}
		} catch(CoreException e) {
			logger.error("[OLQuickFix] EXCEPTION ", e);
		}
	}

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
					actOpenable = (IOpenable) javaElement;
				
				// Create AST to parse
				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);
				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				actRoot = (CompilationUnit) parser.createAST(null);
				
				return true;
			}catch (Exception e) {
				logger.error("[Find OL Method] EXCEPTION ", e);
			}
		}
		return false;
	}
	
	/**
	 * 取得Method相關資訊(已知MethodIndex)
	 * @param methodIdx
	 */
	public void bindMethod(int methodIdx) {			
		// 取得該class所有的method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		
		// 取得目前要被修改的method node
		MethodDeclaration tempNode = methodList.get(methodIdx);
		currentMethodNode = tempNode;

		// 尋找該method內的OverLogging
		OverLoggingDetector loggingDetector = new OverLoggingDetector(actRoot, tempNode);
		loggingDetector.detect();
		// 取得專案中OverLogging
		List<MarkerInfo> overLogggingTemp = loggingDetector.getOverLoggingList();
		currentLoggingList = overLogggingTemp;

		// 若沒有OverLogging則不記錄
		if (overLogggingTemp.size() != 0 && !isExistence(tempNode)) {
			methodNodeList.add(tempNode);
			loggingList.add(overLogggingTemp);
		}
	}
	
	/**
	 * 往上一層Trace，找出Caller的OverLogging資訊
	 * @param currentNode
	 */
	private void traceCallerMethod(MethodDeclaration methodDeclaration) {
		// 從MethodDeclaration取得IMthod
		IMethod method = (IMethod) methodDeclaration.resolveBinding().getJavaElement();
		// 取得Method的Caller
		IMember[] methodArray = new IMember[] {method};
		MethodWrapper[] callerMethodWrapper = CallHierarchy.getDefault().getCallerRoots(methodArray);
		MethodWrapper[] callers = null;
		
		if (callerMethodWrapper.length == 1)
			callers = callerMethodWrapper[0].getCalls(new NullProgressMonitor());
		
		if (callerMethodWrapper.length == 1 && callers.length != 0) {
			for (MethodWrapper methodWrapper : callers) {
				// 取得caller的IMethod
				IMethod callerMethod = (IMethod) methodWrapper.getMember();
				boolean isOK = obtainResource(callerMethod.getResource());
				if(isOK)
					bindMethod(callerMethod);

				// 是否有OverLogging，沒有就不處理 (即之後就不做刪除動作)
				if (isLoggingExist() && isOK) {
					// 此Class是否已經存在於List中
					addFixList();
				}

				// 偵測是否繼續Trace上一層Caller
				// 若Exception又傳到上一層，則繼續偵測
				if (getIsKeepTrace(method, callerMethod))
					traceCallerMethod(currentMethodNode);
			}
		}
	}
	
	/**
	 * 取得Method相關資訊(只知道哪個Method)
	 * @param method
	 */
	public void bindMethod(IMethod method) {
		// 取得該class所有的method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		
		int methodIdx = -1;
		// 若不曉得methodIndex就從methodList中去比較來取得
		methodIdx = findMethodIndex(method, methodList);

		// 取得目前要被修改的method node
		MethodDeclaration tempNode = methodList.get(methodIdx);
		currentMethodNode = tempNode;
		
		// 尋找該method內的OverLogging
		OverLoggingDetector loggingDetector = new OverLoggingDetector(actRoot, tempNode);
		loggingDetector.detect();
		// 取得專案中OverLogging
		List<MarkerInfo> overLogggingTemp = loggingDetector.getOverLoggingList();
		currentLoggingList = overLogggingTemp;

		// 若沒有OverLogging則不記錄
		if (overLogggingTemp.size() != 0 && !isExistence(tempNode)) {
			methodNodeList.add(tempNode);
			loggingList.add(overLogggingTemp);
		}
	}
	
	/**
	 * 取得Method的Index
	 * @param callerMethod	被比較的Method
	 * @param methodList	該class中全部的Method
	 * @return				位於class中第幾個Method
	 */
	private int findMethodIndex(IMethod callerMethod, List<MethodDeclaration> methodList) {
		// 轉換成MethodDeclaration
		ASTNode methodNode = transMethodNode(callerMethod);

		// 計算他位於Method List中哪一個index
		int methodIndex = -1;
		if (methodNode != null) {
			for (MethodDeclaration method : methodList){
				methodIndex++;
				// 與要找的程式是否相同
				if (method.toString().equals(methodNode.toString()))
					return methodIndex;
			}
		}
		return -1;
	}

	/**
	 * IMethod轉換成ASTNode MethodDeclaration
	 * @param method	IMethod
	 * @return			MethodDeclaration(ASTNode)
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;

		try {
			// Parser Jar檔時，會取不到ICompilationUnit
			if (method.getCompilationUnit() == null)
				return null;

			// 產生AST
			ASTParser parserAST = ASTParser.newParser(AST.JLS3);
			parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
			parserAST.setSource(method.getCompilationUnit());
			parserAST.setResolveBindings(true);
			ASTNode unit = parserAST.createAST(null);

			// 取得AST的Method部份
			NodeFinder nodeFinder = new NodeFinder(method.getSourceRange().getOffset(), method.getSourceRange().getLength());
			unit.accept(nodeFinder);
			ASTNode methodNode = nodeFinder.getCoveredNode();

			// 若此ASTNode屬於MethodDeclaration，則轉型
			if(methodNode instanceof MethodDeclaration)
				md = (MethodDeclaration) methodNode;
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] JavaModelException ", e);
		}
		return md;
	}
	
	/**
	 * 往下一層Trace，找出Caller的OverLogging資訊
	 * @param methodDeclaration
	 */
	private void traceCalleeMethod(MethodDeclaration methodDeclaration) {
		// 從MethodDeclaration取得IMthod
		IMethod method = (IMethod) methodDeclaration.resolveBinding().getJavaElement();
		// 取得Method的Callee
		IMember[] methodArray = new IMember[] {method};
		MethodWrapper[] calleeMethodWrapper = CallHierarchy.getDefault().getCalleeRoots(methodArray);
		MethodWrapper[] callers = null, callees = null;
		
		if (calleeMethodWrapper.length == 1)
			callees = calleeMethodWrapper[0].getCalls(new NullProgressMonitor());
		/* Eclipse3.3:
		 * MethodWrapper currentMW = new CallHierarchy().getCallerRoot(method);
		 * MethodWrapper[] calls = currentMW.getCalls(new NullProgressMonitor());
		 */
		if (calleeMethodWrapper.length == 1 && callees.length != 0) {
			for (MethodWrapper methodWrapper : callees) {
				IMethod calleeMethod = (IMethod) methodWrapper.getMember();
				try {
					String[] calleeType = calleeMethod.getExceptionTypes();
					// 若Callee有throws Exception
					for (String type : calleeType) {
						boolean isOK = obtainResource(calleeMethod.getResource());
						if(isOK)
							bindMethod(calleeMethod);

						// 是否有OverLogging，沒有就不處理 (即之後就不做刪除動作)
						if (isLoggingExist() && isOK) {
							// 此Class是否已經存在於List中
							addFixList();
						}

						// 繼續下一層Trace
						traceCalleeMethod(currentMethodNode);
					}
				} catch (JavaModelException e) {
					logger.error("[Java Model Exception] JavaModelException ", e);
				}
			}
		}
	}
	
	/**
	 * 刪除Message
	 */
	public void deleteMessage(CompilationUnit unit) {
		try {
			actRoot = unit;
			actRoot.recordModifications();
			actOpenable = (IOpenable)actRoot.getJavaElement();
			// 若有複數個Method要修改
			for (int i=0; i < methodNodeList.size(); i++) {
				// 收集該method所有的catch clause
				ASTCatchCollect catchCollector = new ASTCatchCollect();
				methodNodeList.get(i).accept(catchCollector);
				List<CatchClause> catchList = catchCollector.getMethodList();
				// 刪除該Method內的Catch中的Logging Statement
				for (MarkerInfo msg : loggingList.get(i)) {
					// 去比對startPosition,找出要修改的節點
					for (CatchClause cc : catchList) {
						if (cc.getStartPosition() == msg.getPosition()) {
							// 刪除Catch內的Logging Statement
							deleteCatchStatement(cc, msg);
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("[Delete Message] EXCEPTION ", e);
		}
	}
	
	/**
	 * 刪除Catch內有OverLogging Marker的Logging動作
	 * @param cc
	 * @param msg
	 */
	private void deleteCatchStatement(CatchClause cc, MarkerInfo msg) {
		// 取得CatchClause所有的statement，將OverLogging資訊的東西移除
		List<?> statementList = cc.getBody().statements();
		
		// 比對Catch內所有的Statement
		for (int i=0; i < statementList.size(); i++) {
			if (statementList.get(i) instanceof ExpressionStatement ) {
				ExpressionStatement statement = (ExpressionStatement) statementList.get(i);

				// 若為選擇的行數，則刪除此行
				int line = actRoot.getLineNumber(statement.getStartPosition());
				if (line == msg.getLineNumber())
					statementList.remove(i);
			}
		}
	}
	
	/**
	 * 將所要變更的內容寫回Edit中
	 */
	private void applyChange() {
		try {
			ICompilationUnit icu = (ICompilationUnit) actOpenable;
			Document document = new Document(icu.getBuffer().getContents());

			TextEdit edits = actRoot.rewrite(document, icu.getJavaProject().getOptions(true));
			edits.apply(document);

			icu.getBuffer().setContents(document.get());
		} catch (Exception e) {
			// BadLocationException & JavaModelException
			logger.error("[Delete Statement Exception] EXCEPTION ", e);
		}
	}
	
	/**
	 * 偵測是否繼續Trace上一層Caller(Exception是否傳到上一層)
	 * @param methodDeclaration
	 * @param callerMethod
	 * @return					是否繼續Trace
	 */
	private boolean getIsKeepTrace(IMethod method, IMethod callerMethod) {
		MethodDeclaration methodNode = transMethodNode(callerMethod);
		// 偵測是否將Exception傳到上一層
		OverLoggingVisitor visitor = new OverLoggingVisitor(actRoot, method.getElementName());				
		methodNode.accept(visitor);

		return visitor.getIsKeepTrace();
	}
	
	/**
	 * 確認此Class是否已經存在於List中。
	 * 若存在則在CompilationUnit中新增欲修改的Method；若不存在則新增CompilationUnit至List
	 */
	private void addFixList() {
		boolean isExistence = false;

		for (CompilationUnit unit: unitList) {
			// 判斷CompilationUnit是否與List中的相同
			if (unit.toString().equals(actRoot.toString())) {
				isExistence = true;
				break;
			}
		}
		// 若沒加入，則新加入至List
		if (!isExistence)
			unitList.add(actRoot);
	}
	
	private boolean isExistence(MethodDeclaration newNode) {
		boolean isExistence = false;
		
		if(methodNodeList.size() == 0)
			return isExistence;
		
		for(MethodDeclaration node : methodNodeList) {
			if(node.toString().equals(newNode.toString())) {
				isExistence = true;
				break;
			}
		}
		return isExistence;
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
