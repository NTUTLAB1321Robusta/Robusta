package ntut.csie.csdet.smell.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ntut.csie.analyzer.dummy.DummyHandlerVisitor;
import ntut.csie.analyzer.empty.EmptyCatchBlockVisitor;
import ntut.csie.analyzer.nested.NestedTryStatementVisitor;
import ntut.csie.analyzer.unprotected.UnprotectedMainProgramVisitor;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.rleht.RLEHTPlugin;
import ntut.csie.rleht.common.ASTHandler;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.util.NodeUtils;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.NodeFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EHSmellModel {
	private static Logger logger = LoggerFactory.getLogger(EHSmellModel.class);

	private List<MarkerInfo> dummyHandlerList = null;
	private List<MarkerInfo> emptyCatchList = null;
	private List<MarkerInfo> nestedTryList = null;
	private List<MarkerInfo> unprotectedMainList = null;
	private List<MarkerInfo> smellList = new ArrayList<MarkerInfo>();

	private ASTNode methodNode = null;	

	private ASTHandler astHandler = null;
	
	private CompilationUnit actRoot;

	private IOpenable actOpenable;
	private int currentLine = 0;
	
	public EHSmellModel() {
		astHandler = new ASTHandler();
	}
	
	public void clear(){
		if(dummyHandlerList != null) dummyHandlerList.clear();
		if(emptyCatchList != null) emptyCatchList.clear();
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
			System.err.println("There is not a java program in editor！");
			throw createCoreException("編輯器內容不是java程式！", null);
		}

		this.actOpenable = openable;

		// check whether jdk's version is above 1.5(due to support annotation)
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
	 * parse smell in list
	 * @param offset
	 * @param length
	 */
	public void parseDocument(int offset, int length) {
		this.methodNode = NodeUtils.getSpecifiedParentNode(NodeFinder.perform(actRoot, offset, length), ASTNode.METHOD_DECLARATION);
		
		if (methodNode != null) {
			//scan bad smell in methodNode
			DummyHandlerVisitor dhVisitor = new DummyHandlerVisitor(actRoot);
			EmptyCatchBlockVisitor ecbVisitor = new EmptyCatchBlockVisitor(actRoot);
			this.methodNode.accept(dhVisitor);
			this.methodNode.accept(ecbVisitor);
			dummyHandlerList = dhVisitor.getDummyHandlerList();
			emptyCatchList = ecbVisitor.getEmptyCatchList();
	
			NestedTryStatementVisitor ntVisitor = new NestedTryStatementVisitor(actRoot);
			methodNode.accept(ntVisitor);
			nestedTryList = ntVisitor.getNestedTryStatementList();
			
			UnprotectedMainProgramVisitor mainVisitor = new UnprotectedMainProgramVisitor(actRoot);
			methodNode.accept(mainVisitor);
			unprotectedMainList = mainVisitor.getUnprotectedMainList();
		}

		smellList.clear();
		//combine all smells in smellList
		if (dummyHandlerList != null) {
			smellList.addAll(dummyHandlerList);
		}
		if (emptyCatchList != null) {
			smellList.addAll(emptyCatchList); 
		}
		if (unprotectedMainList != null)
			smellList.addAll(unprotectedMainList);
		if (nestedTryList != null)
			smellList.addAll(nestedTryList);
		sortCSMessageListInIncreasingOrder(smellList);

		this.setCurrentLine(offset);
	}
	
	/**
	 * get line number of cursor position 
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
	 * sort CSMessageList in increasing order
	 * @param smellList
	 */
	private void sortCSMessageListInIncreasingOrder(List<MarkerInfo> smellList) {
		Collections.sort(smellList, new Comparator<MarkerInfo>(){
			public int compare(MarkerInfo s1, MarkerInfo s2){
				if(s1.getLineNumber() < s2.getLineNumber())
					return 0;
				else
					return 1;
			}
		});
	}
	
	public List<MarkerInfo> getDummyList() {
		return dummyHandlerList;
	}
	public List<MarkerInfo> getEmptyList() {
		return emptyCatchList;
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
