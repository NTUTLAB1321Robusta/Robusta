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

	//������쪺DummyHandler Smell
	private List<MarkerInfo> dummyHandlerList = null;
	//������쪺IgnoreCheckedException Smell
	private List<MarkerInfo> ignoreExList = null;
	//������쪺NestedTryBlock Smell
	private List<MarkerInfo> nestedTryList = null;
	//������쪺UnprotectedMain Smell
	private List<MarkerInfo> unprotectedMainList = null;
	//�����Ҧ���Smell
	private List<MarkerInfo> smellList = new ArrayList<MarkerInfo>();

	// �ثe��Method AST Node
	private ASTNode methodNode = null;	

	private ASTHandler astHandler = null;
	
	private CompilationUnit actRoot;

	private IOpenable actOpenable;
	//�O���{�b���
	private int currentLine = 0;
	
	public EHSmellModel() {
		astHandler = new ASTHandler();
	}
	
	public void clear(){
		//�M��List
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
			System.err.println("�s�边���e���Ojava�{���I");
			throw createCoreException("�s�边���e���Ojava�{���I", null);
		}

		this.actOpenable = openable;

		// �P�_Java���X�������O�_��1.5�H�W(�]���n�ϥ�Annotation)
		IJavaProject project = (IJavaProject) ((IJavaElement) actOpenable).getAncestor(IJavaElement.JAVA_PROJECT);
		String option = project.getOption(JavaCore.COMPILER_SOURCE, true);
		if (!JavaCore.VERSION_1_5.equals(option) && !JavaCore.VERSION_1_6.equals(option)) {
			throw createCoreException("java�{�����O1.5�H�W�����I", null);
		}
		try {
			this.actRoot = astHandler.createAST(actOpenable, pos);
			return (this.actRoot != null);
		} catch (RuntimeException ex) {
			logger.error("[createAST] EXCEPTION ", ex);
			throw createCoreException("�L�k����AST:\n" + ex.getMessage(), ex);
		}
	}
	
	private CoreException createCoreException(String msg, Throwable cause) {
		return new CoreException(new Status(IStatus.ERROR, RLEHTPlugin.PLUGIN_ID, IStatus.ERROR, msg, cause));
	}
	
	/**
	 * �ѪR�XSmell��List
	 * @param offset
	 * @param length
	 */
	public void parseDocument(int offset, int length) {
		this.methodNode = NodeUtils.getSpecifiedParentNode(NodeFinder.perform(actRoot, offset, length), ASTNode.METHOD_DECLARATION);
		
		//����S�Imethod�X�{NullPoint���~
		if (methodNode != null) {
			//��X�o��method��code smell
			DummyHandlerVisitor dhVisitor = new DummyHandlerVisitor(actRoot);
			IgnoreExceptionVisitor ieVisitor = new IgnoreExceptionVisitor(actRoot);
			this.methodNode.accept(dhVisitor);
			this.methodNode.accept(ieVisitor);
			dummyHandlerList = dhVisitor.getDummyList();
			ignoreExList = ieVisitor.getIgnoreList();
	
			//���o�M�פ���Nested Try Block
			NestedTryStatementVisitor ntVisitor = new NestedTryStatementVisitor(actRoot);
			methodNode.accept(ntVisitor);
			nestedTryList = ntVisitor.getNestedTryStatementList();
			
			//�M���method����unprotected main program
			UnprotectedMainProgramVisitor mainVisitor = new UnprotectedMainProgramVisitor(actRoot);
			methodNode.accept(mainVisitor);
			unprotectedMainList = mainVisitor.getUnprotedMainList();
		}

		//�M��Smell
		smellList.clear();
		//��Ҧ�Smell��List�[�b�@�_
		if (dummyHandlerList != null) {
			/* FIXME - �Ȯ��ഫ�ΡA������������MarkerInfo�N���ݭn�o��LOOP */
			List<MarkerInfo> tempList = new ArrayList<MarkerInfo>();
			for(int i = 0; i < dummyHandlerList.size(); i++) {
				MarkerInfo message = new MarkerInfo(dummyHandlerList.get(i).getCodeSmellType(), dummyHandlerList.get(i).getTypeBinding(), dummyHandlerList.get(i).getStatement(), dummyHandlerList.get(i).getPosition(), dummyHandlerList.get(i).getLineNumber(), dummyHandlerList.get(i).getExceptionType());
				tempList.add(message);
			}
			smellList.addAll(tempList);
		}
		if (ignoreExList != null) {
			/* FIXME - �Ȯ��ഫ�ΡA������������MarkerInfo�N���ݭn�o��LOOP */
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
		//SmellList�Ƨ�
		sortCSMessageList(smellList);

		//�O���{�b���
		this.setCurrentLine(offset);
	}
	
	/**
	 * ���o�ثe��ЩҦb���
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
	 * ��CSMessage��List�̦�ư��Ƨ�(�ɾ�)
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
	 * ���o�U��Smell��List
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
