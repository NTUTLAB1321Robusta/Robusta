package ntut.csie.csdet.smell.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.visitor.DummyHandlerVisitor;
import ntut.csie.csdet.visitor.IgnoreExceptionVisitor;
import ntut.csie.csdet.visitor.NestedTryStatementVisitor;
import ntut.csie.csdet.visitor.UnprotectedMainProgramVisitor;
import ntut.csie.jdt.util.NodeUtils;
import ntut.csie.rleht.RLEHTPlugin;
import ntut.csie.rleht.common.ASTHandler;
import ntut.csie.rleht.views.ExceptionAnalyzer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.astview.NodeFinder;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EHSmellModel {
	private static Logger logger = LoggerFactory.getLogger(EHSmellModel.class);

	//紀錄找到的DummyHandler Smell
	private List<MarkerInfo> dummyHandlerList = null;
	//紀錄找到的IgnoreCheckedException Smell
	private List<MarkerInfo> ignoreExList = null;
	//紀錄找到的NestedTryBlock Smell
	private List<MarkerInfo> nestedTryList = null;
	//紀錄找到的UnprotectedMain Smell
	private List<MarkerInfo> unprotectedMainList = null;
	//紀錄所有的Smell
	private List<MarkerInfo> smellList = new ArrayList<MarkerInfo>();

	// 目前的Method AST Node
	private ASTNode methodNode = null;	

	private ASTHandler astHandler = null;
	
	private CompilationUnit actRoot;

	private IOpenable actOpenable;
	//記錄現在行數
	private int currentLine = 0;
	
	public EHSmellModel() {
		astHandler = new ASTHandler();
	}
	
	public void clear(){
		//清除List
		if(dummyHandlerList != null) dummyHandlerList.clear();
		if(ignoreExList != null) ignoreExList.clear();
		if(nestedTryList != null) nestedTryList.clear();
		if(unprotectedMainList != null) unprotectedMainList.clear();
		if(smellList != null) smellList.clear();

		methodNode = null;
		astHandler = null;
		actRoot = null;
		actOpenable = null;
		System.gc();
	}

	/**
	 * @param openable
	 * @param pos
	 * @return
	 * @throws CoreException
	 */
	public boolean createAST(IOpenable openable, int pos) throws CoreException {
		if (openable == null) {
			System.err.println("編輯器內容不是java程式！");
			throw createCoreException("編輯器內容不是java程式！", null);
		}

		this.actOpenable = openable;

		// 判斷Java源碼的版本是否為1.5以上(因為要使用Annotation)
		IJavaProject project = (IJavaProject) ((IJavaElement) actOpenable).getAncestor(IJavaElement.JAVA_PROJECT);
		String option = project.getOption(JavaCore.COMPILER_SOURCE, true);
		if (!JavaCore.VERSION_1_5.equals(option) && !JavaCore.VERSION_1_6.equals(option)) {
			throw createCoreException("java程式不是1.5以上版本！", null);
		}
		try {
			this.actRoot = astHandler.createAST(actOpenable, pos);
			return (this.actRoot != null);
		} catch (RuntimeException ex) {
			logger.error("[createAST] EXCEPTION ", ex);
			throw createCoreException("無法產生AST:\n" + ex.getMessage(), ex);
		}
	}
	
	private CoreException createCoreException(String msg, Throwable cause) {
		return new CoreException(new Status(IStatus.ERROR, RLEHTPlugin.PLUGIN_ID, IStatus.ERROR, msg, cause));
	}
	
	/**
	 * 解析出Smell的List
	 * @param offset
	 * @param length
	 */
	public void parseDocument(int offset, int length) {
		this.methodNode = NodeUtils.getSpecifiedParentNode(NodeFinder.perform(actRoot, offset, length), ASTNode.METHOD_DECLARATION);
		
		//防止沒點method出現NullPoint錯誤
		if (methodNode != null) {
			//找出這個method的code smell
			DummyHandlerVisitor dhVisitor = new DummyHandlerVisitor(actRoot);
			IgnoreExceptionVisitor ieVisitor = new IgnoreExceptionVisitor(actRoot);
			this.methodNode.accept(dhVisitor);
			this.methodNode.accept(ieVisitor);
			dummyHandlerList = dhVisitor.getDummyList();
			ignoreExList = ieVisitor.getIgnoreList();
	
			//取得專案中的Nested Try Block
			NestedTryStatementVisitor ntVisitor = new NestedTryStatementVisitor(actRoot);
			methodNode.accept(ntVisitor);
			nestedTryList = ntVisitor.getNestedTryStatementList();
			
			//尋找該method內的unprotected main program
			UnprotectedMainProgramVisitor mainVisitor = new UnprotectedMainProgramVisitor(actRoot);
			methodNode.accept(mainVisitor);
			unprotectedMainList = mainVisitor.getUnprotedMainList();
		}

		//清除Smell
		smellList.clear();
		//把所有Smell的List加在一起
		if (dummyHandlerList != null) {
			/* FIXME - 暫時轉換用，等全部都換成MarkerInfo就不需要這個LOOP */
			List<MarkerInfo> tempList = new ArrayList<MarkerInfo>();
			for(int i = 0; i < dummyHandlerList.size(); i++) {
				MarkerInfo message = new MarkerInfo(dummyHandlerList.get(i).getCodeSmellType(), dummyHandlerList.get(i).getTypeBinding(), dummyHandlerList.get(i).getStatement(), dummyHandlerList.get(i).getPosition(), dummyHandlerList.get(i).getLineNumber(), dummyHandlerList.get(i).getExceptionType());
				tempList.add(message);
			}
			smellList.addAll(tempList);
		}
		if (ignoreExList != null) {
			/* FIXME - 暫時轉換用，等全部都換成MarkerInfo就不需要這個LOOP */
			List<MarkerInfo> tempList = new ArrayList<MarkerInfo>();
			for(int i = 0; i < ignoreExList.size(); i++) {
				MarkerInfo message = new MarkerInfo(ignoreExList.get(i).getCodeSmellType(), ignoreExList.get(i).getTypeBinding(), ignoreExList.get(i).getStatement(), ignoreExList.get(i).getPosition(), ignoreExList.get(i).getLineNumber(), ignoreExList.get(i).getExceptionType());
				tempList.add(message);
			}
			smellList.addAll(tempList); 
		}
		if (unprotectedMainList != null)
			smellList.addAll(unprotectedMainList);
		if (nestedTryList != null)
			smellList.addAll(nestedTryList);
		//SmellList排序
		sortCSMessageList(smellList);

		//記錄現在行數
		this.setCurrentLine(offset);
	}
	
	/**
	 * 取得目前游標所在行數
	 * @param pos
	 * @return
	 */
	public int getCurrentLine() {
		return this.currentLine;
	}
	public void setCurrentLine(int pos) {
		if (this.actRoot != null) {
			this.currentLine = this.actRoot.getLineNumber(pos);
		} else {
			this.currentLine = -1;
		}
	}

	public void clearData() {
		this.actRoot = null;
	}

	public boolean hasData() {
		return methodNode != null && actRoot != null;
	}

	public int getPosition() {
		return (this.methodNode != null ? this.methodNode.getStartPosition() : 0);
	}

	public String getMethodName() {
		if (methodNode != null) {
			MethodDeclaration method = (MethodDeclaration) methodNode;
			return method.getName().getIdentifier();
		} else {
			return null;
		}
	}

	public IOpenable getActOpenable() {
		return actOpenable;
	}

	/**
	 * 把CSMessage的List依行數做排序(升冪)
	 * @param smellList
	 */
	private void sortCSMessageList(List<MarkerInfo> smellList) {
		Collections.sort(smellList, new Comparator<MarkerInfo>(){
			public int compare(MarkerInfo s1, MarkerInfo s2){
				if(s1.getLineNumber() < s2.getLineNumber())
					return 0;
				else
					return 1;
			}
		});
	}
	
	/**
	 * 取得各個Smell的List
	 * @return
	 */
	public List<MarkerInfo> getDummyList() {
		return dummyHandlerList;
	}
	public List<MarkerInfo> getIgnoreList() {
		return ignoreExList;
	}
	public List<MarkerInfo> getnestedTryList() {
		return nestedTryList;
	}
	public List<MarkerInfo> getunprotectedMainList() {
		return unprotectedMainList;
	}
	public List<MarkerInfo> getAllSmellList() {
		return smellList;
	}
}
