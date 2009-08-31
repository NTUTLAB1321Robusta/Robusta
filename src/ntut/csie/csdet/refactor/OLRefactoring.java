package ntut.csie.csdet.refactor;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.visitor.ASTBinding;
import ntut.csie.csdet.visitor.LoggingAnalyzer;
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
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IOpenable;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
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
import org.jdom.Attribute;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * �bMarker�W����Refactor���A�[�J�R���P��Method��Reference���Y��Logging
 * @author Shiau
 */
public class OLRefactoring implements IMarkerResolution{
	private static Logger logger = LoggerFactory.getLogger(OLRefactoring.class);

	//����code smell��type
	private String problem;
	//code smell���T��
	private String label;
	//OverLogging�]�w (�O�_�n����Exception�૬)
	private boolean detectTransExSet = false;
	//OverLogging�]�w (�ϥΪ̭n������Logging�W�h)
	private TreeMap<String, Integer> libMap = new TreeMap<String, Integer>();
	//�n�R��OverLogging��List(�@��CompilationUnit�����)
	private List<OLRefactorAction> fixList = new ArrayList<OLRefactorAction>();

	public OLRefactoring(String label){
		//���o�T��
		this.label = label;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void run(IMarker marker) {
		try {
			//Ĳ�oMarker�O�_��OverLogging
			problem = (String) marker.getAttribute(RLMarkerAttribute.RL_MARKER_TYPE);
			if(problem != null && (problem.equals(RLMarkerAttribute.CS_OVER_LOGGING))) {
				//�NUser���OverLogging���]�w�s�U��
				getOverLoggingSettings();

				//���oMarker����T
				String methodIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_METHOD_INDEX);
				String msgIdx = (String) marker.getAttribute(RLMarkerAttribute.RL_MSG_INDEX);

				//���oClass��������T (Ĳ�o��Method)
				OLRefactorAction baseMethod = new OLRefactorAction();
				boolean isok = baseMethod.obtainResource(marker.getResource());
				if(isok) {
					//���oMethod������T
					baseMethod.bindMethod(Integer.parseInt(methodIdx));

					//���o���ϥժ����
					int selectLine = baseMethod.getCurrentLoggingList().get(Integer.parseInt(msgIdx)).getLineNumber();
					//�[�J�ܧR���W��
					fixList.add(baseMethod);

					//���oCaller Method��Logging��T
					traceCallerMethod(baseMethod.getMethodNode(0));
					//���oCallee Method��Logging��T
					traceCalleeMethod(baseMethod.getMethodNode(0));

					/// �Y��ʪ�CompilationUnit��A��ʷ|�X��                   ///
					/// �ҥH��Ҧ��n�R����Logging���O���_�ӡA�A�@���R�� ///
					for (OLRefactorAction temp : fixList)
						//�R��List����OverLogging (�@���H�@��Class�����R��)
						temp.deleteMessage();

					//�}���I�諸java�ɪ�Editor
					//�⭶�����^�ӡA�~�వ�w��\��
					openEditor(marker.getResource());
					//��Щw�� 
					setSelectLine(baseMethod.getActOpenable(), selectLine -1);
				}
			}
		} catch (CoreException e) {
			logger.error("[OLQuickFix] EXCEPTION ",e);
		}
	}

	/**
	 * ���W�@�hTrace�A��XCaller��OverLogging��T
	 * @param currentNode
	 */
	private void traceCallerMethod(ASTNode currentNode) {
		//TODO caller�������OverLogging(���Ptry catch��Marker)�A�|�@�֧R��
		if (currentNode instanceof MethodDeclaration) {
			//�qMethodDeclaration���oIMthod
			MethodDeclaration methodDeclaration = (MethodDeclaration) currentNode;
			IMethod method = (IMethod) methodDeclaration.resolveBinding().getJavaElement();

			//���oMethod��Caller
			MethodWrapper currentMW = new CallHierarchy().getCallerRoot(method);				
			MethodWrapper[] calls = currentMW.getCalls(new NullProgressMonitor());

			//�Y��Caller
			if (calls.length != 0) {
				for (MethodWrapper mw : calls) {
					//���ocaller��IMethod
					IMethod callerMethod = (IMethod) mw.getMember();

					OLRefactorAction tempAction = new OLRefactorAction();
					boolean isOK = tempAction.obtainResource(callerMethod.getResource());
					tempAction.bindMethod(callerMethod);

					//�O�_��OverLogging�A�S���N���B�z (�Y����N�����R���ʧ@)
					if (tempAction.isLoggingExist() && isOK)
						//��Class�O�_�w�g�s�b��List��
						addFixList(callerMethod, tempAction);

					//�����O�_�~��Trace�W�@�hCaller
					boolean isTrace = getIsKeepTrace(methodDeclaration, method, callerMethod);

					//�YException�S�Ǩ�W�@�h�A�h�~�򰻴�
					if (isTrace)
						traceCallerMethod(tempAction.getCurrentMethodNode());
				}
			}
		}
	}

	/**
	 * �����O�_�~��Trace�W�@�hCaller(Exception�O�_�Ǩ�W�@�h)
	 * @param methodDeclaration
	 * @param method
	 * @param callerMethod
	 * @return					�O�_�~��Trace
	 */
	private boolean getIsKeepTrace(MethodDeclaration methodDeclaration,	IMethod method, IMethod callerMethod) {
		//���oLoggingAnalyzer�n����T
		String classInfo = methodDeclaration.resolveBinding().getDeclaringClass().getQualifiedName();
		MethodDeclaration methodNode = transMethodNode(callerMethod);
		//�����O�_�NException�Ǩ�W�@�h
		LoggingAnalyzer visitor = new LoggingAnalyzer(classInfo, method.getElementName(),
										libMap, detectTransExSet);				
		methodNode.accept(visitor);

		return visitor.getIsKeepTrace();
	}	
	
	/**
	 * ���U�@�hTrace�A��XCaller��OverLogging��T
	 * @param currentMethodNode
	 */
	private void traceCalleeMethod(ASTNode currentMethodNode) {
		if (currentMethodNode instanceof MethodDeclaration) {
			//�qMethodDeclaration���oIMthod
			MethodDeclaration methodDeclaration = (MethodDeclaration) currentMethodNode;
			IMethod method = (IMethod) methodDeclaration.resolveBinding().getJavaElement();

			//���oMethod��Caller
			MethodWrapper currentMW = new CallHierarchy().getCalleeRoot(method);				
			MethodWrapper[] calls = currentMW.getCalls(new NullProgressMonitor());

			//�Y��Callee
			if (calls.length != 0) {
				for (MethodWrapper mw : calls) {
					IMember calleeMember = (IMember) mw.getMember();
					if (calleeMember instanceof IMethod) {
						IMethod calleeMethod = (IMethod) calleeMember;

						//CalleeMethod����NULL�� ��귽���ɦW����java(�o��jar�ɤ���)
						if (calleeMethod.getResource() != null &&
							calleeMethod.getResource().getName().endsWith(".java")) {
							try {
								String[] calleeType = calleeMethod.getExceptionTypes();
								//�YCallee��throws Exception
								if (calleeType != null && calleeType.length != 0) {
									for (String type : calleeType) {
										//���oException(���o����Ƭ�"QException;"�R���Y����Q�M;)
										type = type.substring(1, type.length()-1);
	
										OLRefactorAction tempAction = new OLRefactorAction();
										boolean isOK = tempAction.obtainResource(calleeMethod.getResource());
										tempAction.bindMethod(calleeMethod);
		
										//�O�_��OverLogging�A�S���N���B�z (�Y����N�����R���ʧ@)
										if (tempAction.isLoggingExist() && isOK)
											//��Class�O�_�w�g�s�b��List��
											addFixList(calleeMethod, tempAction);
										
										//�~��U�@�hTrace
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
	 * IMethod�ഫ��ASTNode MethodDeclaration
	 * @param method	IMethod
	 * @return			MethodDeclaration(ASTNode)
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

	/**
	 * �T�{��Class�O�_�w�g�s�b��List���C
	 * �Y�s�b�h�bCompilationUnit���s�W���ק諸Method�F�Y���s�b�h�s�WCompilationUnit��List
	 * @param callerMethod
	 * @param refactorAction
	 */
	private void addFixList(IMethod callerMethod, OLRefactorAction refactorAction) {
		boolean isExist = false;

		//�}�Ҧ�Method��java�ɪ�Editor
		openEditor(callerMethod.getResource());

		for (OLRefactorAction oldAction : fixList) {
			//�P�_CompilationUnit�O�_�PList�����ۦP
			if (oldAction.getActRoot().getJavaElement()
				.equals(refactorAction.getActRoot().getJavaElement())) {
				//�Y�w�[�J�A�h�h���o�s��Method��T
				oldAction.bindMethod(callerMethod);
				isExist = true;
				break;
			}
		}
		//�Y�S�[�J�A�h�s�[�J��List
		if (!isExist)
			fixList.add(refactorAction);
	}

	/**
	 * ��Щw��
	 * @param iopenable	�귽
	 * @param line		���
	 */
	private void setSelectLine(IOpenable iopenable, int line) {
		try {
			ICompilationUnit icu = (ICompilationUnit) iopenable;
			Document document = new Document(icu.getBuffer().getContents());

			//���o�ثe��EditPart
			IEditorPart editorPart = EditorUtils.getActiveEditor();
			ITextEditor editor = (ITextEditor) editorPart;

			//���o��ƪ����
			IRegion lineInfo = document.getLineInformation(line); 

			//�ϥոӦ� �bQuick fix������,�i�H�N��Щw��bQuick Fix����
			editor.selectAndReveal(lineInfo.getOffset(), 0);
		} catch (JavaModelException jme) {
			logger.error("[BadLocation] EXCEPTION ", jme);
		} catch (BadLocationException ble) {
			logger.error("[BadLocation] EXCEPTION ", ble);
		}
	}
	
	/**
	 * �NUser���OverLogging���]�w�s�U��
	 */
	public void getOverLoggingSettings() {
		Element root = JDomUtil.createXMLContent();
		//�p�G�Onull����XML�ɬO��ئn��,�٨S��OverLogging��tag,�������X�h
		if(root.getChild(JDomUtil.OverLoggingTag) != null) {
			//�o�̪��ܤ��e�ϥΪ̤w�g���]�w�Lpreference�F,�h���o���������]�w��
			Element overLogging = root.getChild(JDomUtil.OverLoggingTag);
			Element rule = overLogging.getChild("rule");
			String log4jSet = rule.getAttribute(JDomUtil.apache_log4j).getValue();
			String javaLogger = rule.getAttribute(JDomUtil.java_Logger).getValue();

			/// �⤺�ذ����[�J��W�椺 ///
			//��log4j�MjavaLog�[�J������
			if (log4jSet.equals("Y"))
				libMap.put("org.apache.log4j", ASTBinding.LIBRARY);
			if (javaLogger.equals("Y"))
				libMap.put("java.util.logging", ASTBinding.LIBRARY);

			Element libRule = overLogging.getChild("librule");
			// ��~��Library�MStatement�x�s�bList��
			List<Attribute> libRuleList = libRule.getAttributes();

			/// ��ϥΪ̩ҳ]�w��Exception�૬�������]�w ///
			Element exrule = overLogging.getChild("exrule");
			String exSet = exrule.getAttribute(JDomUtil.transException).getValue();
			detectTransExSet = exSet.equals("Y");
			
			//��~����Library�[�J�����W�椺
			for (int i=0; i < libRuleList.size(); i++) {
				if (libRuleList.get(i).getValue().equals("Y")) {
					String temp = libRuleList.get(i).getQualifiedName();					

					//�Y��.*���u����Library
					if (temp.indexOf(".EH_STAR")!=-1) {
						int pos = temp.indexOf(".EH_STAR");
						libMap.put(temp.substring(0,pos), ASTBinding.LIBRARY);
					//�Y��*.���u����Method
					} else if (temp.indexOf("EH_STAR.") != -1) {
						libMap.put(temp.substring(8), ASTBinding.METHOD);
					//���S�����������A����Library+Method
					} else if (temp.lastIndexOf(".") != -1) {
						libMap.put(temp, ASTBinding.LIBRARY_METHOD);
					//�Y���䥦�Ϊp�h�]��Method
					} else {
						libMap.put(temp, ASTBinding.METHOD);
					}
				}
			}
		}
	}
	
	/**
	 * �}��Resource��Editor
	 * @param resource	�n�Q���}���귽
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