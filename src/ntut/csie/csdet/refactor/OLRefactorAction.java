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
 * ���oOverLogging��T �M �R��OverLogging ���ʧ@
 * @author Shiau
 */
public class OLRefactorAction {
	private static Logger logger = LoggerFactory.getLogger(OLRefactorAction.class);

	//Class(CompilationUnit)��������T
	private IOpenable actOpenable = null;
	private CompilationUnit actRoot = null;
	//�s��ثe�ҭnfix��method node
	private ASTNode currentMethodNode = null;
	//�s��ثe�n�i�J��CatchNode
	private ASTNode currentCatchNode = null;
	//�s��P�@��Class���nfix��method
	private List<ASTNode> methodNodeList = new ArrayList<ASTNode>();
	//�ثeMethod����OverLogging
	private List<MarkerInfo> currentLoggingList = new ArrayList<MarkerInfo>();
	//�s��P�@��Class���nfix��method���A�ҥX�{��OverLogging
	private List<List<MarkerInfo>> loggingList = new ArrayList<List<MarkerInfo>>();
	
	/**
	 * ���oClass(CompilationUnit)��������T
	 * @param resource
	 * @return			�O�_���\���o
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
	 * ���oMethod������T(�w��MethodIndex)
	 * @param methodIdx
	 */
	public void bindMethod(int methodIdx) {				
		//���o��class�Ҧ���method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		List<ASTNode> methodList = methodCollector.getMethodList();
		
		//���o�ثe�n�Q�ק諸method node
		ASTNode tempNode = methodList.get(methodIdx);
		currentMethodNode = tempNode;

		if(tempNode != null) {
			//�M���method����OverLogging
			OverLoggingDetector loggingDetector = new OverLoggingDetector(this.actRoot, tempNode);
			loggingDetector.detect();
			//���o�M�פ�OverLogging
			List<MarkerInfo> overLogggingTemp = loggingDetector.getOverLoggingList();
			currentLoggingList = overLogggingTemp;

			//�Y�S��OverLogging�h���O��
			if (overLogggingTemp.size() != 0) {
				this.methodNodeList.add(tempNode);
				this.loggingList.add(overLogggingTemp);
			}
		}
		
		//������method�Ҧ���catch clause
		ASTCatchCollect catchCollector = new ASTCatchCollect();
		currentMethodNode.accept(catchCollector);
		List<ASTNode> catchList = catchCollector.getMethodList();
		//�R����Method����Catch����Logging Statement
		for (MarkerInfo msg : currentLoggingList) {
			//�h���startPosition,��X�n�ק諸�`�I
			for (ASTNode cc : catchList) {
				if (cc.getStartPosition() == msg.getPosition()) {
					currentCatchNode = cc;
					break;
				}
			}
		}
	}
	
	/**
	 * ���oMethod������T(�u���D����Method)
	 * @param method
	 */
	public void bindMethod(IMethod method) {
		//���o��class�Ҧ���method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		List<ASTNode> methodList = methodCollector.getMethodList();
		
		int methodIdx = -1;
		//�Y����omethodIndex�N�qmethodList���h����Ө��o
		if (method != null)
			methodIdx = findMethodIndex(method, methodList);

		//���o�ثe�n�Q�ק諸method node
		ASTNode tempNode = methodList.get(methodIdx);
		currentMethodNode = tempNode;
		
		if(tempNode != null) {
			//�M���method����OverLogging
			OverLoggingDetector loggingDetector = new OverLoggingDetector(this.actRoot, tempNode);
			loggingDetector.detect();
			//���o�M�פ�OverLogging
			List<MarkerInfo> overLogggingTemp = loggingDetector.getOverLoggingList();
			currentLoggingList = overLogggingTemp;

			//�Y�S��OverLogging�h���O��
			if (overLogggingTemp.size() != 0) {
				this.methodNodeList.add(tempNode);
				this.loggingList.add(overLogggingTemp);
			}
		}
	}
	
	/**
	 * �R��Message
	 */
	public void deleteMessage(){
		try {
			//�O���ק�
			actRoot.recordModifications();
			//�Y���Ƽƭ�Method�n�ק�
			for (int i=0; i < methodNodeList.size(); i++) {
				ASTNode currentMethodNode = methodNodeList.get(i);

				//������method�Ҧ���catch clause
				ASTCatchCollect catchCollector = new ASTCatchCollect();
				currentMethodNode.accept(catchCollector);
				List<ASTNode> catchList = catchCollector.getMethodList();
				//�R����Method����Catch����Logging Statement
				for (MarkerInfo msg : loggingList.get(i)) {
					//�h���startPosition,��X�n�ק諸�`�I
					for (ASTNode cc : catchList) {
						if (cc.getStartPosition() == msg.getPosition()) {
							//�R��Catch����Logging Statement
							deleteCatchStatement(cc, msg);
							//�g�^Edit��
							break;
						}
					}
				}
			}
			//�N�ҭn�ܧ󪺤��e�g�^Edit��
			applyChange();			
		} catch (Exception ex) {
			logger.error("[Delete Message] EXCEPTION ",ex);
		}
	}
	
	/**
	 * �R��Catch����OverLogging Marker��Logging�ʧ@
	 * @param cc
	 * @param msg
	 */
	private void deleteCatchStatement(ASTNode cc, MarkerInfo msg){		
		CatchClause clause = (CatchClause)cc;
		//���oCatchClause�Ҧ���statement,�N����print�ҥ~��T���F�貾��
		List statementList = clause.getBody().statements();
		
		if (statementList.size() != 0) {
			//���Catch���Ҧ���Statement
			for (int i=0; i < statementList.size(); i++) {
				if (statementList.get(i) instanceof ExpressionStatement ) {
					ExpressionStatement statement = (ExpressionStatement) statementList.get(i);

					//�Y����ܪ���ơA�h�R������
					int line = actRoot.getLineNumber(statement.getStartPosition());
					if (line == msg.getLineNumber()) {
						statementList.remove(i);
					}
				}
			}
		}
	}
	
	/**
	 * �N�ҭn�ܧ󪺤��e�g�^Edit��
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
	 * ���oMethod��Index
	 * @param callerMethod	�Q�����Method
	 * @param methodList	��class��������Method
	 * @return				���class���ĴX��Method
	 */
	private int findMethodIndex(IMethod callerMethod, List<ASTNode> methodList) {
		//�ഫ��MethodDeclaration
		ASTNode methodNode = transMethodNode(callerMethod);

		//�p��L���Method List�����@��index
		int methodIndex = -1;
		if (methodNode != null) {
			for (ASTNode method : methodList){
				methodIndex++;
				//�P�n�䪺�{���O�_�ۦP
				if (method.toString().equals(methodNode.toString()))
					break;
			}
		}
		return methodIndex;
	}
	
	/**
	 * �ഫ��ASTNode MethodDeclaration
	 * @param method
	 * @return
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;
		
		try {
			//Parser Jar�ɮɡA�|������ICompilationUnit
			if (method.getCompilationUnit() == null)
				return null;

			//����AST
			ASTParser parserAST = ASTParser.newParser(AST.JLS3);
			parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
			parserAST.setSource(method.getCompilationUnit());
			parserAST.setResolveBindings(true);
			ASTNode ast = parserAST.createAST(null);

			//���oAST��Method����
			ASTNode methodNode = NodeFinder.perform(ast, method.getSourceRange().getOffset(),
														method.getSourceRange().getLength());

			//�Y��ASTNode�ݩ�MethodDeclaration�A�h�૬
			if(methodNode instanceof MethodDeclaration) {
				md = (MethodDeclaration) methodNode;
			}
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] JavaModelException ", e);
		}

		return md;
	}

	//Class(CompilationUnit)��������T
	public IOpenable getActOpenable() { return actOpenable; }
	public CompilationUnit getActRoot() { return actRoot; }
	//���oMethod��������T
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
	 * �{�b�o��Method���O�_��OverLogging
	 * @return	�O�_��OverLogging
	 */
	public boolean isLoggingExist() {
		if (currentLoggingList == null || currentLoggingList.size() == 0)
			return false;
		return true;
	}
}
