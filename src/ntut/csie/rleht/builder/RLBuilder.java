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
	
	//延伸problem view好讓自己的marker可以加進去view中
	public static final String MARKER_TYPE = "ntut.csie.rleht.builder.RLProblem";
	
	/**
	 * 將相關例外資訊貼上marker
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
	 * 加code smell type的marker到problem view 中
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
	 * (non-Javadoc)每次有build的時候,都會invoke這個method
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
		System.out.println("RLBuild花費時間 "+(end - start) + " milli second.");
		return null;
	}

	/**
	 * 進行fullBuild or inrementalBuild時,都會去呼叫這個method
	 * @param resource
	 */
	void checkRLAnnotation(IResource resource) {
		if (resource instanceof IFile && resource.getName().endsWith(".java")) {

			logger.debug("[RLBuilder][checkRLAnnotation] START !!");

			IFile file = (IFile) resource;
			deleteMarkers(file);

			try {
				/* STEP1:針對每一個Java程式的Method檢查RLAnnotation */
				/*       並且找出專案中所有的Code Smell  */

				IJavaElement javaElement = JavaCore.create(resource);

				ASTParser parser = ASTParser.newParser(AST.JLS3);
				parser.setKind(ASTParser.K_COMPILATION_UNIT);

				parser.setSource((ICompilationUnit) javaElement);
				parser.setResolveBindings(true);
				CompilationUnit root = (CompilationUnit) parser.createAST(null);
				ASTMethodCollector methodCollector = new ASTMethodCollector();
				root.accept(methodCollector);
						
				//取得專案中所有的method
				List<ASTNode> methodList = methodCollector.getMethodList();

				ExceptionAnalyzer visitor = null;
				
				CodeSmellAnalyzer csVisitor = null;
				
				MainAnalyzer mainVisitor = null;
				
				CarelessCleanUpAnalyzer ccVisitor=null;
				
				OverLoggingDetector loggingDetector = null;
				// 目前method的Exception資訊
				List<RLMessage> currentMethodExList = null;

				// 目前method的RL Annotation資訊
				List<RLMessage> currentMethodRLList = null;
				
				// 目前method內的ignore Exception資訊
				List<CSMessage> ignoreExList = null;
				
				// 目前method內的dummy handler資訊
				List<CSMessage> dummyList = null;
				
				// 目前method內的Nested Try Block資訊
				List<CSMessage> nestedTryList = null; 
				
				// 目前method內的Unprotected Main資訊
				List<CSMessage> unprotectedMain = null;
				
				//目前method內的Careless CleanUp資訊
				List<CSMessage> ccList=null;
				
				List<CSMessage> overLoggingList = null;
				
				// 目前的Method AST Node
				ASTNode currentMethodNode = null;
				int methodIdx = -1;
				for (ASTNode method : methodList) {
					methodIdx++;

					visitor = new ExceptionAnalyzer(root, method.getStartPosition(), 0);
					method.accept(visitor);
					currentMethodNode = visitor.getCurrentMethodNode();
					currentMethodRLList = visitor.getMethodRLAnnotationList();
					
					// 找尋專案中所有的ignore Exception
					csVisitor = new CodeSmellAnalyzer(root);
					method.accept(csVisitor);
					//取得專案中的ignore Exception
					ignoreExList = csVisitor.getIgnoreExList();
					int csIdx = -1;
					if(ignoreExList != null){
						//將每個ignore exception都貼上marker
						for(CSMessage msg : ignoreExList){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]未處理!!!";
							//貼marker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);
						}
					}
					
					//取得專案中dummy handler
					dummyList = csVisitor.getDummyList();
					csIdx = -1;
					if(dummyList != null){
						// 將每個dummy handler都貼上marker
						for(CSMessage msg : dummyList){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]未處理!!!";
							//貼marker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);
						}
					}
					
					//取得專案中的Nested Try Block
					nestedTryList = visitor.getNestedTryList();
					csIdx = -1;
					if(nestedTryList != null){
						for(CSMessage msg : nestedTryList){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]未處理!!!";
							//貼marker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);						
						}
					}
					
					//找尋專案中所有的Careless Cleanup
					ccVisitor = new CarelessCleanUpAnalyzer(root);
					method.accept(ccVisitor);
					ccList = ccVisitor.getCarelessCleanUpList();
					csIdx = -1;
					if(ccList != null){
						// 將每個Careless Cleanup都貼上marker
						for(CSMessage msg : ccList){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]未處理!!!";
							//貼marker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);
						}
					}
					
										//尋找該method內的OverLogging
					loggingDetector = new OverLoggingDetector(root,method);
					loggingDetector.detect();
					overLoggingList = loggingDetector.getOverLoggingList();
					
					//依據所取得的code smell來貼Marker
					csIdx = -1;
					if(overLoggingList != null){
						for(CSMessage msg : overLoggingList){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]未處理!!!";
							//貼marker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);	
						}
					}
					
					//尋找該method內的unprotected main program
					mainVisitor = new MainAnalyzer(root);
					method.accept(mainVisitor);
					unprotectedMain = mainVisitor.getUnprotedMainList();
					
					//依據所取得的code smell來貼Marker
					csIdx = -1;
					if(unprotectedMain != null){
						for(CSMessage msg : unprotectedMain){
							csIdx++;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]未處理!!!";
							//貼marker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);	
						}
					}						
									
					if (currentMethodNode != null) {
						RLChecker checker = new RLChecker();
						currentMethodExList = checker.check(visitor);
					}					
					
					// 檢查@RL是否存在(丟出的例外是否被註記)
					int msgIdx = -1;
					for (RLMessage msg : currentMethodExList) {
						msgIdx++;
						if (msg.getRLData().getLevel() >= 0) {
							if (!msg.isHandling()) {
								String errmsg = "*例外[" + msg.getRLData().getExceptionType() + "] 未定義@RL！";
								this.addMarker(file, errmsg.toString(), msg.getLineNumber(), IMarker.SEVERITY_WARNING,
										RLMarkerAttribute.ERR_NO_RL, msg, msgIdx, methodIdx);
							}
						}
					}

					msgIdx = -1;
					for (RLMessage msg : currentMethodRLList) {
						msgIdx++;

						int lineNumber = root.getLineNumber(method.getStartPosition());

						// 檢查@RL清單內的level是否正確
						if (!RLData.validLevel(msg.getRLData().getLevel())) {
							StringBuffer errmsg = new StringBuffer();
							errmsg.append("@RL( level=").append(msg.getRLData().getLevel());
							errmsg.append(" , exception=");
							errmsg.append(msg.getRLData().getExceptionType() + ") level值錯誤！");

							this.addMarker(file, errmsg.toString(), lineNumber, IMarker.SEVERITY_ERROR,
									RLMarkerAttribute.ERR_RL_LEVEL, msg, msgIdx, methodIdx);
						}

						// 檢查@RL清單內的exception類別階層是否正確
						int idx2 = 0;
						for (RLMessage msg2 : currentMethodRLList) {
							if (msgIdx >= idx2++) {
								continue;
							}

							if (msg.getRLData().getExceptionType().equals(msg2.getRLData().getExceptionType())) {
								this.addMarker(file, "@RL(level=" + msg.getRLData().getLevel() + ",exception="
										+ msg.getRLData().getExceptionType() + ") 重覆！", lineNumber,
										IMarker.SEVERITY_ERROR, RLMarkerAttribute.ERR_RL_DUPLICATE, msg, msgIdx,
										methodIdx);
							}
							else if (ASTHandler.isInstance(msg2.getTypeBinding(), msg.getTypeBinding()
									.getQualifiedName())) {
								this.addMarker(file, "@RL(level=" + msg.getRLData().getLevel() + ",exception="
										+ msg.getRLData().getExceptionType() + ") 為 @RL(level="
										+ msg2.getRLData().getLevel() + ",exception="
										+ msg2.getRLData().getExceptionType() + ")之父類別(子類別順序應在前)！", lineNumber,
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
