package ntut.csie.rleht.builder;

import java.util.List;
import java.util.Map;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.visitor.CarelessCleanUpAnalyzer;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
import ntut.csie.csdet.visitor.MainAnalyzer;
import ntut.csie.csdet.visitor.OverLoggingDetector;
import ntut.csie.rleht.common.ASTHandler;
import ntut.csie.rleht.views.ExceptionAnalyzer;
import ntut.csie.rleht.views.RLChecker;
import ntut.csie.rleht.views.RLData;
import ntut.csie.rleht.views.RLMessage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RLBuilder extends IncrementalProjectBuilder {
	private static Logger logger = LoggerFactory.getLogger(RLBuilder.class);
	
	public static final String BUILDER_ID = "ntut.csie.rleht.builder.RLBuilder";
	
	//����problem view�n���ۤv��marker�i�H�[�i�hview��
	public static final String MARKER_TYPE = "ntut.csie.rleht.builder.RLProblem";
	
	/**
	 * �N�����ҥ~��T�K�Wmarker
	 */
	private void addMarker(IFile file, String message, int lineNumber, int severity, String mtype, RLMessage msg,
			int msgIdx, int methodIdx) {
		
		logger.debug("[RLBuilder][addMarker] START! ");

		try {
			IMarker marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);

			marker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE, mtype);
			marker.setAttribute(RLMarkerAttribute.RL_INFO_LEVEL, String.valueOf(msg.getRLData().getLevel()));
			marker.setAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION, msg.getRLData().getExceptionType());
			marker.setAttribute(RLMarkerAttribute.RL_INFO_SRC_POS, String.valueOf(msg.getPosition()));

			marker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, String.valueOf(methodIdx));
			marker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, String.valueOf(msgIdx));
			
			
		}
		catch (CoreException ex) {
			logger.error("[addMarker] EXCEPTION ",ex);
		}
		logger.debug("[RLBuilder][addMarker] END ! ");
	}

	/**
	 * �[code smell type��marker��problem view ��
	 */
	private void addMarker(IFile file, String message, int lineNumber, int severity, String mtype, CSMessage msg,
			int msgIdx, int methodIdx) {
		logger.debug("[RLBuilder][addCSMarker] START! ");
		IMarker marker;
		try {
			marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			if (lineNumber == -1) {
				lineNumber = 1;
			}
			
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
			//marker type =  EH smell type
			marker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE, mtype);
			marker.setAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION, msg.getExceptionType());
			marker.setAttribute(RLMarkerAttribute.RL_INFO_SRC_POS, String.valueOf(msg.getPosition()));
			marker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, String.valueOf(methodIdx));
			marker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, String.valueOf(msgIdx));
			
		} catch (CoreException e) {
			logger.error("[addCSMarker] EXCEPTION ",e);
		}
		logger.debug("[RLBuilder][addCSMarker] END ! ");
	
	}
	
	/*
	 * (non-Javadoc)�C����build���ɭ�,���|invoke�o��method
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		long start = System.currentTimeMillis();
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		}
		else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			}
			else {
				incrementalBuild(delta, monitor);
			}
		}
		long end = System.currentTimeMillis();
		System.out.println("RLBuild��O�ɶ� "+(end - start) + " milli second.");
		return null;
	}

	/**
	 * �i��fullBuild or inrementalBuild��,���|�h�I�s�o��method
	 * @param resource
	 */
	void checkRLAnnotation(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {

			logger.debug("[RLBuilder][checkRLAnnotation] START !!");

			IFile file = (IFile) resource;
			deleteMarkers(file);

			try {
				/* STEP1:�w��C�@��Java�{����Method�ˬdRLAnnotation */
				/*       �åB��X�M�פ��Ҧ���Code Smell  */

				IJavaElement javaElement = JavaCore.create(resource);

				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);

				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				CompilationUnit root = (CompilationUnit) parser.createAST(null);
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				root.accept(methodCollector);
						
				//���o�M�פ��Ҧ���method
				List<ASTNode> methodList = methodCollector.getMethodList();

				ExceptionAnalyzer visitor = null;
				
				CodeSmellAnalyzer csVisitor = null;
				
				MainAnalyzer mainVisitor = null;
				
				CarelessCleanUpAnalyzer ccVisitor=null;
				
				OverLoggingDetector loggingDetector = null;
				// �ثemethod��Exception��T
				List<RLMessage> currentMethodExList = null;

				// �ثemethod��RL Annotation��T
				List<RLMessage> currentMethodRLList = null;
				
				// �ثemethod����ignore Exception��T
				List<CSMessage> ignoreExList = null;
				
				// �ثemethod����dummy handler��T
				List<CSMessage> dummyList = null;
				
				// �ثemethod����Nested Try Block��T
				List<CSMessage> nestedTryList = null; 
				
				// �ثemethod����Unprotected Main��T
				List<CSMessage> unprotectedMain = null;
				
				//�ثemethod����Careless CleanUp��T
				List<CSMessage> ccList=null;
				
				List<CSMessage> overLoggingList = null;
				
				// �ثe��Method AST Node
				ASTNode currentMethodNode = null;
				int methodIdx = -1;
				for (ASTNode method : methodList) {
					methodIdx++;

					visitor = new ExceptionAnalyzer(root, method.getStartPosition(), 0);
					method.accept(visitor);
					currentMethodNode = visitor.getCurrentMethodNode();
					currentMethodRLList = visitor.getMethodRLAnnotationList();
					
					// ��M�M�פ��Ҧ���ignore Exception
					csVisitor = new CodeSmellAnalyzer(root);
					method.accept(csVisitor);
					//���o�M�פ���ignore Exception
					ignoreExList = csVisitor.getIgnoreExList();
					int csIdx = -1;
					if(ignoreExList != null){
						//�N�C��ignore exception���K�Wmarker
						for(CSMessage msg : ignoreExList){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]���B�z!!!";
							//�Kmarker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);
						}
					}
					
					//���o�M�פ�dummy handler
					dummyList = csVisitor.getDummyList();
					csIdx = -1;
					if(dummyList != null){
						// �N�C��dummy handler���K�Wmarker
						for(CSMessage msg : dummyList){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]���B�z!!!";
							//�Kmarker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);
						}
					}
					
					//���o�M�פ���Nested Try Block
					nestedTryList = visitor.getNestedTryList();
					csIdx = -1;
					if(nestedTryList != null){
						for(CSMessage msg : nestedTryList){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]���B�z!!!";
							//�Kmarker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);						
						}
					}
					
					//��M�M�פ��Ҧ���Careless Cleanup
					ccVisitor = new CarelessCleanUpAnalyzer(root);
					method.accept(ccVisitor);
					ccList = ccVisitor.getCarelessCleanUpList();
					csIdx = -1;
					if(ccList != null){
						// �N�C��Careless Cleanup���K�Wmarker
						for(CSMessage msg : ccList){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]���B�z!!!";
							//�Kmarker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);
						}
					}
					
										//�M���method����OverLogging
					loggingDetector = new OverLoggingDetector(root,method);
					loggingDetector.detect();
					overLoggingList = loggingDetector.getOverLoggingList();
					
					//�̾کҨ��o��code smell�ӶKMarker
					csIdx = -1;
					if(overLoggingList != null){
						for(CSMessage msg : overLoggingList){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]���B�z!!!";
							//�Kmarker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);	
						}
					}
					
					//�M���method����unprotected main program
					mainVisitor = new MainAnalyzer(root);
					method.accept(mainVisitor);
					unprotectedMain = mainVisitor.getUnprotedMainList();
					
					//�̾کҨ��o��code smell�ӶKMarker
					csIdx = -1;
					if(unprotectedMain != null){
						for(CSMessage msg : unprotectedMain){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]���B�z!!!";
							//�Kmarker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);	
						}
					}						
									
					if (currentMethodNode != null) {
						RLChecker checker = new RLChecker();
						currentMethodExList = checker.check(visitor);
					}					
					
					// �ˬd@RL�O�_�s�b(��X���ҥ~�O�_�Q���O)
					int msgIdx = -1;
					for (RLMessage msg : currentMethodExList) {
						msgIdx++;
						if (msg.getRLData().getLevel() >= 0) {
							if (!msg.isHandling()) {
								String errmsg = "*�ҥ~[" + msg.getRLData().getExceptionType() + "] ���w�q@RL�I";
								this.addMarker(file, errmsg.toString(), msg.getLineNumber(), IMarker.SEVERITY_WARNING,
										RLMarkerAttribute.ERR_NO_RL, msg, msgIdx, methodIdx);
							}
						}
					}

					msgIdx = -1;
					for (RLMessage msg : currentMethodRLList) {
						msgIdx++;

						int lineNumber = root.getLineNumber(method.getStartPosition());

						// �ˬd@RL�M�椺��level�O�_���T
						if (!RLData.validLevel(msg.getRLData().getLevel())) {
							StringBuffer errmsg = new StringBuffer();
							errmsg.append("@RL( level=").append(msg.getRLData().getLevel());
							errmsg.append(" , exception=");
							errmsg.append(msg.getRLData().getExceptionType() + ") level�ȿ��~�I");

							this.addMarker(file, errmsg.toString(), lineNumber, IMarker.SEVERITY_ERROR,
									RLMarkerAttribute.ERR_RL_LEVEL, msg, msgIdx, methodIdx);
						}

						// �ˬd@RL�M�椺��exception���O���h�O�_���T
						int idx2 = 0;
						for (RLMessage msg2 : currentMethodRLList) {
							if (msgIdx >= idx2++) {
								continue;
							}

							if (msg.getRLData().getExceptionType().equals(msg2.getRLData().getExceptionType())) {
								this.addMarker(file, "@RL(level=" + msg.getRLData().getLevel() + ",exception="
										+ msg.getRLData().getExceptionType() + ") ���СI", lineNumber,
										IMarker.SEVERITY_ERROR, RLMarkerAttribute.ERR_RL_DUPLICATE, msg, msgIdx,
										methodIdx);
							}
							else if (ASTHandler.isInstance(msg2.getTypeBinding(), msg.getTypeBinding()
									.getQualifiedName())) {
								this.addMarker(file, "@RL(level=" + msg.getRLData().getLevel() + ",exception="
										+ msg.getRLData().getExceptionType() + ") �� @RL(level="
										+ msg2.getRLData().getLevel() + ",exception="
										+ msg2.getRLData().getExceptionType() + ")�������O(�l���O�������b�e)�I", lineNumber,
										IMarker.SEVERITY_ERROR, RLMarkerAttribute.ERR_RL_INSTANCE, msg, msgIdx,
										methodIdx);
							}

						}
					}
				}
			}
			catch (Exception ex) {
				logger.error("[checkRLAnnotation] EXCEPTION ",ex);
			}
			logger.debug("[RLBuilder][checkRLAnnotation] END !!");

		}
	}

	private void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE, false, IResource.DEPTH_ZERO);
		}
		catch (CoreException ce) {
		}
	}

	protected void fullBuild(final IProgressMonitor monitor) throws CoreException {
		try {
			getProject().accept(new RLResourceVisitor());
		}
		catch (CoreException e) {
		}
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
		// the visitor does the work.
		delta.accept(new RLMethodDeltaVisitor());
	}

	// =========================================================================

	class RLMethodDeltaVisitor implements IResourceDeltaVisitor {
		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(org.eclipse.core.resources.IResourceDelta)
		 */
		public boolean visit(IResourceDelta delta) throws CoreException {
			IResource resource = delta.getResource();
			switch (delta.getKind()) {
				case IResourceDelta.ADDED:
					// handle added resource
					checkRLAnnotation(resource);
					break;
				case IResourceDelta.REMOVED:
					// handle removed resource
					break;
				case IResourceDelta.CHANGED:
					// handle changed resource
					checkRLAnnotation(resource);
					break;
			}
			// return true to continue visiting children.
			return true;
		}
	}

	class RLResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			checkRLAnnotation(resource);
			// return true to continue visiting children.
			return true;
		}
	}

}
