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
	// Class(CompilationUnit)��������T
	private IOpenable actOpenable = null;
	private CompilationUnit actRoot = null;
	// �s��ثe�ҭnfix��method node
	private MethodDeclaration currentMethodNode = null;
	// �s��P�@��Class���nfix��method
	private List<MethodDeclaration> methodNodeList = new ArrayList<MethodDeclaration>();
	// �ثeMethod����OverLogging
	private List<MarkerInfo> currentLoggingList = new ArrayList<MarkerInfo>();
	// �s��P�@��Class���nfix��method���A�ҥX�{��OverLogging
	private List<List<MarkerInfo>> loggingList = new ArrayList<List<MarkerInfo>>();
	private List<CompilationUnit> unitList = new ArrayList<CompilationUnit>();
	
	public void refator(IMarker marker) {
		try {
			// ���oMarker����T
			String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
			if(obtainResource(marker.getResource())) {
				// ���oMethod������T
				bindMethod(Integer.parseInt(methodIdx));
				// �[�J�ܧR���W��
				unitList.add(actRoot);
				// ���oMethod��Logging��T
				traceCallerMethod(methodNodeList.get(0));
				traceCalleeMethod(methodNodeList.get(0));
				// �Y���CompilationUnit��A��ʷ|�X��
				// �ҥH��Ҧ��n�R����Logging���O���_�ӡA�A�@���R��
				for (CompilationUnit unit : unitList) {
					// �R��List����OverLogging (�@���H�@��Class�����R��)
					deleteMessage(unit);
					// �N�ҭn�ܧ󪺤��e�g�^Edit��
					applyChange();
				}
			}
		} catch(CoreException e) {
			logger.error("[OLQuickFix] EXCEPTION ", e);
		}
	}

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
	 * ���oMethod������T(�w��MethodIndex)
	 * @param methodIdx
	 */
	public void bindMethod(int methodIdx) {			
		// ���o��class�Ҧ���method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		
		// ���o�ثe�n�Q�ק諸method node
		MethodDeclaration tempNode = methodList.get(methodIdx);
		currentMethodNode = tempNode;

		// �M���method����OverLogging
		OverLoggingDetector loggingDetector = new OverLoggingDetector(actRoot, tempNode);
		loggingDetector.detect();
		// ���o�M�פ�OverLogging
		List<MarkerInfo> overLogggingTemp = loggingDetector.getOverLoggingList();
		currentLoggingList = overLogggingTemp;

		// �Y�S��OverLogging�h���O��
		if (overLogggingTemp.size() != 0 && !isExistence(tempNode)) {
			methodNodeList.add(tempNode);
			loggingList.add(overLogggingTemp);
		}
	}
	
	/**
	 * ���W�@�hTrace�A��XCaller��OverLogging��T
	 * @param currentNode
	 */
	private void traceCallerMethod(MethodDeclaration methodDeclaration) {
		// �qMethodDeclaration���oIMthod
		IMethod method = (IMethod) methodDeclaration.resolveBinding().getJavaElement();
		// ���oMethod��Caller
		IMember[] methodArray = new IMember[] {method};
		MethodWrapper[] callerMethodWrapper = CallHierarchy.getDefault().getCallerRoots(methodArray);
		MethodWrapper[] callers = null;
		
		if (callerMethodWrapper.length == 1)
			callers = callerMethodWrapper[0].getCalls(new NullProgressMonitor());
		
		if (callerMethodWrapper.length == 1 && callers.length != 0) {
			for (MethodWrapper methodWrapper : callers) {
				// ���ocaller��IMethod
				IMethod callerMethod = (IMethod) methodWrapper.getMember();
				boolean isOK = obtainResource(callerMethod.getResource());
				if(isOK)
					bindMethod(callerMethod);

				// �O�_��OverLogging�A�S���N���B�z (�Y����N�����R���ʧ@)
				if (isLoggingExist() && isOK) {
					// ��Class�O�_�w�g�s�b��List��
					addFixList();
				}

				// �����O�_�~��Trace�W�@�hCaller
				// �YException�S�Ǩ�W�@�h�A�h�~�򰻴�
				if (getIsKeepTrace(method, callerMethod))
					traceCallerMethod(currentMethodNode);
			}
		}
	}
	
	/**
	 * ���oMethod������T(�u���D����Method)
	 * @param method
	 */
	public void bindMethod(IMethod method) {
		// ���o��class�Ҧ���method
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		actRoot.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		
		int methodIdx = -1;
		// �Y����omethodIndex�N�qmethodList���h����Ө��o
		methodIdx = findMethodIndex(method, methodList);

		// ���o�ثe�n�Q�ק諸method node
		MethodDeclaration tempNode = methodList.get(methodIdx);
		currentMethodNode = tempNode;
		
		// �M���method����OverLogging
		OverLoggingDetector loggingDetector = new OverLoggingDetector(actRoot, tempNode);
		loggingDetector.detect();
		// ���o�M�פ�OverLogging
		List<MarkerInfo> overLogggingTemp = loggingDetector.getOverLoggingList();
		currentLoggingList = overLogggingTemp;

		// �Y�S��OverLogging�h���O��
		if (overLogggingTemp.size() != 0 && !isExistence(tempNode)) {
			methodNodeList.add(tempNode);
			loggingList.add(overLogggingTemp);
		}
	}
	
	/**
	 * ���oMethod��Index
	 * @param callerMethod	�Q�����Method
	 * @param methodList	��class��������Method
	 * @return				���class���ĴX��Method
	 */
	private int findMethodIndex(IMethod callerMethod, List<MethodDeclaration> methodList) {
		// �ഫ��MethodDeclaration
		ASTNode methodNode = transMethodNode(callerMethod);

		// �p��L���Method List�����@��index
		int methodIndex = -1;
		if (methodNode != null) {
			for (MethodDeclaration method : methodList){
				methodIndex++;
				// �P�n�䪺�{���O�_�ۦP
				if (method.toString().equals(methodNode.toString()))
					return methodIndex;
			}
		}
		return -1;
	}

	/**
	 * IMethod�ഫ��ASTNode MethodDeclaration
	 * @param method	IMethod
	 * @return			MethodDeclaration(ASTNode)
	 */
	private MethodDeclaration transMethodNode(IMethod method) {
		MethodDeclaration md = null;

		try {
			// Parser Jar�ɮɡA�|������ICompilationUnit
			if (method.getCompilationUnit() == null)
				return null;

			// ����AST
			ASTParser parserAST = ASTParser.newParser(AST.JLS3);
			parserAST.setKind(ASTParser.K_COMPILATION_UNIT);
			parserAST.setSource(method.getCompilationUnit());
			parserAST.setResolveBindings(true);
			ASTNode unit = parserAST.createAST(null);

			// ���oAST��Method����
			NodeFinder nodeFinder = new NodeFinder(method.getSourceRange().getOffset(), method.getSourceRange().getLength());
			unit.accept(nodeFinder);
			ASTNode methodNode = nodeFinder.getCoveredNode();

			// �Y��ASTNode�ݩ�MethodDeclaration�A�h�૬
			if(methodNode instanceof MethodDeclaration)
				md = (MethodDeclaration) methodNode;
		} catch (JavaModelException e) {
			logger.error("[Java Model Exception] JavaModelException ", e);
		}
		return md;
	}
	
	/**
	 * ���U�@�hTrace�A��XCaller��OverLogging��T
	 * @param currentMethodNode
	 */
	private void traceCalleeMethod(MethodDeclaration methodDeclaration) {
		// �qMethodDeclaration���oIMthod
		IMethod method = (IMethod) methodDeclaration.resolveBinding().getJavaElement();
		// ���oMethod��Callee
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
					// �YCallee��throws Exception
					for (String type : calleeType) {
						boolean isOK = obtainResource(calleeMethod.getResource());
						if(isOK)
							bindMethod(calleeMethod);

						// �O�_��OverLogging�A�S���N���B�z (�Y����N�����R���ʧ@)
						if (isLoggingExist() && isOK) {
							// ��Class�O�_�w�g�s�b��List��
							addFixList();
						}

						// �~��U�@�hTrace
						traceCalleeMethod(currentMethodNode);
					}
				} catch (JavaModelException e) {
					logger.error("[Java Model Exception] JavaModelException ", e);
				}
			}
		}
	}
	
	/**
	 * �R��Message
	 */
	public void deleteMessage(CompilationUnit unit) {
		try {
			actRoot = unit;
			actRoot.recordModifications();
			actOpenable = (IOpenable)actRoot.getJavaElement();
			// �Y���Ƽƭ�Method�n�ק�
			for (int i=0; i < methodNodeList.size(); i++) {
				// ������method�Ҧ���catch clause
				ASTCatchCollect catchCollector = new ASTCatchCollect();
				methodNodeList.get(i).accept(catchCollector);
				List<CatchClause> catchList = catchCollector.getMethodList();
				// �R����Method����Catch����Logging Statement
				for (MarkerInfo msg : loggingList.get(i)) {
					// �h���startPosition,��X�n�ק諸�`�I
					for (CatchClause cc : catchList) {
						if (cc.getStartPosition() == msg.getPosition()) {
							// �R��Catch����Logging Statement
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
	 * �R��Catch����OverLogging Marker��Logging�ʧ@
	 * @param cc
	 * @param msg
	 */
	private void deleteCatchStatement(CatchClause cc, MarkerInfo msg) {
		// ���oCatchClause�Ҧ���statement�A�NOverLogging��T���F�貾��
		List<?> statementList = cc.getBody().statements();
		
		// ���Catch���Ҧ���Statement
		for (int i=0; i < statementList.size(); i++) {
			if (statementList.get(i) instanceof ExpressionStatement ) {
				ExpressionStatement statement = (ExpressionStatement) statementList.get(i);

				// �Y����ܪ���ơA�h�R������
				int line = actRoot.getLineNumber(statement.getStartPosition());
				if (line == msg.getLineNumber())
					statementList.remove(i);
			}
		}
	}
	
	/**
	 * �N�ҭn�ܧ󪺤��e�g�^Edit��
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
	 * �����O�_�~��Trace�W�@�hCaller(Exception�O�_�Ǩ�W�@�h)
	 * @param methodDeclaration
	 * @param callerMethod
	 * @return					�O�_�~��Trace
	 */
	private boolean getIsKeepTrace(IMethod method, IMethod callerMethod) {
		MethodDeclaration methodNode = transMethodNode(callerMethod);
		// �����O�_�NException�Ǩ�W�@�h
		OverLoggingVisitor visitor = new OverLoggingVisitor(actRoot, method.getElementName());				
		methodNode.accept(visitor);

		return visitor.getIsKeepTrace();
	}
	
	/**
	 * �T�{��Class�O�_�w�g�s�b��List���C
	 * �Y�s�b�h�bCompilationUnit���s�W���ק諸Method�F�Y���s�b�h�s�WCompilationUnit��List
	 */
	private void addFixList() {
		boolean isExistence = false;

		for (CompilationUnit unit: unitList) {
			// �P�_CompilationUnit�O�_�PList�����ۦP
			if (unit.toString().equals(actRoot.toString())) {
				isExistence = true;
				break;
			}
		}
		// �Y�S�[�J�A�h�s�[�J��List
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
	 * �{�b�o��Method���O�_��OverLogging
	 * @return	�O�_��OverLogging
	 */
	public boolean isLoggingExist() {
		if (currentLoggingList == null || currentLoggingList.size() == 0)
			return false;
		return true;
	}
}
