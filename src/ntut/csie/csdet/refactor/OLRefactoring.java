package ntut.csie.csdet.refactor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.preference.SmellSettings.UserDefinedConstraintsType;
import ntut.csie.csdet.visitor.OverLoggingVisitor;
import ntut.csie.rleht.builder.ASTMethodCollector;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.rleht.common.EditorUtils;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.internal.corext.callhierarchy.CallHierarchy;
import org.eclipse.jdt.internal.corext.callhierarchy.MethodWrapper;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在Marker上面的Refactor中，加入刪除與此Method有Reference關係的Logging
 * @author Shiau
 */
public class OLRefactoring implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(OLRefactoring.class);

	//紀錄code smell的type
	private String problem;
	// Compilation Unit
	private CompilationUnit actRoot;
	//code smell的訊息
	private String label;
	//OverLogging設定 (是否要偵測Exception轉型)
	private boolean detectTransExSet = false;
	//OverLogging設定 (使用者要偵測的Logging規則)
	private TreeMap<String, UserDefinedConstraintsType> libMap = new TreeMap<String, UserDefinedConstraintsType>();
	//要刪除OverLogging的List(一個CompilationUnit為單位)
	private List<OLRefactorAction> fixList = new ArrayList<OLRefactorAction>();

	public OLRefactoring(String label){
		//取得訊息
		this.label = label;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		try {
			//觸發Marker是否為OverLogging
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem != null && (problem.equals(RLMarkerAttribute.CS_OVER_LOGGING))) {
				//將User對於OverLogging的設定存下來
//				getOverLoggingSettings();

				//取得Marker的資訊
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);
				
				//取得Class的相關資訊 (觸發的Method)
				OLRefactorAction baseMethod = new OLRefactorAction();
				boolean isok = baseMethod.obtainResource(marker.getResource());
				if(isok) {
					//取得Method相關資訊
					baseMethod.bindMethod(Integer.parseInt(methodIdx));
					actRoot = baseMethod.getActRoot();
					//取得欲反白的行數
					int selectLine = baseMethod.getCurrentLoggingList().get(Integer.parseInt(msgIdx)).getLineNumber();
					//加入至刪除名單
					fixList.add(baseMethod);

					//取得Caller Method的Logging資訊
					traceCallerMethod(baseMethod.getMethodNode(0));
					//取得Callee Method的Logging資訊
					traceCalleeMethod(baseMethod.getMethodNode(0));

					/// 若更動的CompilationUnit後再更動會出錯                   ///
					/// 所以把所有要刪除的Logging都記錄起來，再一次刪除 ///
					for (OLRefactorAction temp : fixList)
						//刪除List中的OverLogging (一次以一個Class為單位刪除)
						temp.deleteMessage();

					//開啟點選的java檔的Editor
					//把頁面切回來，才能做定位功能
					openEditor(marker.getResource());
					//游標定位 
					setSelectLine(baseMethod.getActOpenable(), selectLine -1);
				}
			}
		} catch (CoreException e) {
			logger.error("[OLQuickFix] EXCEPTION ",e);
		}
	}

	/**
	 * 往上一層Trace，找出Caller的OverLogging資訊
	 * @param currentNode
	 */
	private void traceCallerMethod(ASTNode currentNode) {
		//TODO caller中有兩種OverLogging(不同try catch的Marker)，會一併刪除
		if (currentNode instanceof MethodDeclaration) {
			//從MethodDeclaration取得IMthod
			MethodDeclaration methodDeclaration = (MethodDeclaration) currentNode;
			IMethod method = (IMethod) methodDeclaration.resolveBinding().getJavaElement();

			// 取得Method的Caller
			IMember[] methodArray = new IMember[] {method};
			MethodWrapper[] currentMW = CallHierarchy.getDefault().getCallerRoots(methodArray);
			if (currentMW.length != 1)	return;
			MethodWrapper[] calls = currentMW[0].getCalls(new NullProgressMonitor());
			/* Eclipse3.3:
			 * MethodWrapper currentMW = new CallHierarchy().getCallerRoot(method);
			 * MethodWrapper[] calls = currentMW.getCalls(new NullProgressMonitor());
			 */

			// 若有Caller
			if (calls.length != 0) {
				for (MethodWrapper mw : calls) {
					// 取得caller的IMethod
					IMethod callerMethod = (IMethod) mw.getMember();

					OLRefactorAction tempAction = new OLRefactorAction();
					boolean isOK = tempAction.obtainResource(callerMethod.getResource());
					tempAction.bindMethod(callerMethod);

					//是否有OverLogging，沒有就不處理 (即之後就不做刪除動作)
					if (tempAction.isLoggingExist() && isOK)
						//此Class是否已經存在於List中
						addFixList(callerMethod, tempAction);

					//偵測是否繼續Trace上一層Caller
					boolean isTrace = getIsKeepTrace(methodDeclaration, method, callerMethod);

					//若Exception又傳到上一層，則繼續偵測
					if (isTrace)
						traceCallerMethod(tempAction.getCurrentMethodNode());
				}
			}
		}
	}

	/**
	 * 偵測是否繼續Trace上一層Caller(Exception是否傳到上一層)
	 * @param methodDeclaration
	 * @param method
	 * @param callerMethod
	 * @return					是否繼續Trace
	 */
	private boolean getIsKeepTrace(MethodDeclaration methodDeclaration,	IMethod method, IMethod callerMethod) {
		//取得LoggingAnalyzer要的資訊
		String classInfo = methodDeclaration.resolveBinding().getDeclaringClass().getQualifiedName();
		MethodDeclaration methodNode = transMethodNode(callerMethod);
		//偵測是否將Exception傳到上一層
		OverLoggingVisitor visitor = new OverLoggingVisitor(actRoot, method.getElementName());				
		methodNode.accept(visitor);

		return visitor.getIsKeepTrace();
	}	
	
	/**
	 * 往下一層Trace，找出Caller的OverLogging資訊
	 * @param currentMethodNode
	 */
	private void traceCalleeMethod(ASTNode currentMethodNode) {
		if (currentMethodNode instanceof MethodDeclaration) {
			//從MethodDeclaration取得IMthod
			MethodDeclaration methodDeclaration = (MethodDeclaration) currentMethodNode;
			IMethod method = (IMethod) methodDeclaration.resolveBinding().getJavaElement();

			// 取得Method的Caller
			IMember[] methodArray = new IMember[] {method};
			MethodWrapper[] currentMW = CallHierarchy.getDefault().getCalleeRoots(methodArray);
			if (currentMW.length != 1)	return;
			MethodWrapper[] calls = currentMW[0].getCalls(new NullProgressMonitor());
			/* Eclipse3.3:
			 * MethodWrapper currentMW = new CallHierarchy().getCalleeRoot(method);
			 * MethodWrapper[] calls = currentMW.getCalls(new NullProgressMonitor());
			 */

			// 若有Callee
			if (calls.length != 0) {
				for (MethodWrapper mw : calls) {
					IMember calleeMember = (IMember) mw.getMember();
					if (calleeMember instanceof IMethod) {
						IMethod calleeMethod = (IMethod) calleeMember;

						//CalleeMethod不為NULL或 其資源副檔名不為java(濾掉jar檔之類)
						if (calleeMethod.getResource() != null &&
							calleeMethod.getResource().getName().endsWith(".java")) {
							try {
								String[] calleeType = calleeMethod.getExceptionTypes();
								//若Callee有throws Exception
								if (calleeType != null && calleeType.length != 0) {
									for (String type : calleeType) {
										//取得Exception(取得的資料為"QException;"刪除頭尾的Q和;)
										type = type.substring(1, type.length()-1);
	
										OLRefactorAction tempAction = new OLRefactorAction();
										boolean isOK = tempAction.obtainResource(calleeMethod.getResource());
										tempAction.bindMethod(calleeMethod);
		
										//是否有OverLogging，沒有就不處理 (即之後就不做刪除動作)
										if (tempAction.isLoggingExist() && isOK)
											//此Class是否已經存在於List中
											addFixList(calleeMethod, tempAction);

										//繼續下一層Trace
										traceCalleeMethod(tempAction.getCurrentMethodNode());
									}
								}
							} catch (JavaModelException e) {
									logger.error("[Java Model Exception] JavaModelException ", e);
							}
						}
					}
				}
			}
		}
	}

	/**
	 * IMethod轉換成ASTNode MethodDeclaration
	 * @param method	IMethod
	 * @return			MethodDeclaration(ASTNode)
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

	/**
	 * 確認此Class是否已經存在於List中。
	 * 若存在則在CompilationUnit中新增欲修改的Method；若不存在則新增CompilationUnit至List
	 * @param callerMethod
	 * @param refactorAction
	 */
	private void addFixList(IMethod callerMethod, OLRefactorAction refactorAction) {
		boolean isExist = false;

		//開啟此Method的java檔的Editor
		openEditor(callerMethod.getResource());

		for (OLRefactorAction oldAction : fixList) {
			//判斷CompilationUnit是否與List中的相同
			if (oldAction.getActRoot().getJavaElement()
				.equals(refactorAction.getActRoot().getJavaElement())) {
				//若已加入，則去取得新的Method資訊
				oldAction.bindMethod(callerMethod);
				isExist = true;
				break;
			}
		}
		//若沒加入，則新加入至List
		if (!isExist)
			fixList.add(refactorAction);
	}

	/**
	 * 游標定位
	 * @param iopenable	資源
	 * @param line		行數
	 */
	private void setSelectLine(IOpenable iopenable, int line) {
		try {
			ICompilationUnit icu = (ICompilationUnit) iopenable;
			Document document = new Document(icu.getBuffer().getContents());

			//取得目前的EditPart
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;

			//取得行數的資料
			IRegion lineInfo = document.getLineInformation(line); 

			//反白該行 在Quick fix完之後,可以將游標定位在Quick Fix那行
			editor.selectAndReveal(lineInfo.getOffset(), 0);
		} catch (JavaModelException jme) {
			logger.error("[BadLocation] EXCEPTION ", jme);
		} catch (BadLocationException ble) {
			logger.error("[BadLocation] EXCEPTION ", ble);
		}
	}
	
	/**
	 * 開啟Resource的Editor
	 * @param resource	要被打開的資源
	 */
	private void openEditor(IResource resource) {
		String projectName = resource.getProject().getName();
		IPath javaPath = resource.getFullPath().removeFirstSegments(1);
		
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IFile javaFile = project.getFile(javaPath);

		if (javaFile != null) {
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
	
			IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(javaFile.getName());

			IEditorPart edit = null;
			try {
				edit = page.openEditor(new FileEditorInput(javaFile), desc.getId());
			} catch (PartInitException e) {
				logger.error("[PartInitException] EXCEPTION ",e);
			}
		}
	}
}
