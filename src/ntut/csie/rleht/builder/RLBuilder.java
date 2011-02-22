package ntut.csie.rleht.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import ntut.csie.csdet.data.CSMessage;
import ntut.csie.csdet.data.SSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.visitor.CarelessCleanUpAnalyzer;
import ntut.csie.csdet.visitor.CodeSmellAnalyzer;
import ntut.csie.csdet.visitor.MainAnalyzer;
import ntut.csie.csdet.visitor.OverLoggingDetector;
import ntut.csie.rleht.common.ASTHandler;
import ntut.csie.rleht.rlAdvice.RLAnalyzer;
import ntut.csie.rleht.rlAdvice.RLAdviceMessage;
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
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RLBuilder extends IncrementalProjectBuilder {
	private static Logger logger = LoggerFactory.getLogger(RLBuilder.class);
	
	public static final String BUILDER_ID = "ntut.csie.rleht.builder.RLBuilder";
	
	//延伸problem view好讓自己的marker可以加進去view中
	public static final String MARKER_TYPE = "ntut.csie.rleht.builder.RLProblem";
	
	TreeMap<String,Boolean> detSmellSetting = new TreeMap<String,Boolean>();
	
	/**
	 * 將相關例外資訊貼上marker(RLMessage)
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
			}else{
				System.out.println("RLBuilder, Line 68, objCodeLine:" + lineNumber);
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
	
	private void addMarker(IFile file, String message, int severity, 
			RLAdviceMessage msg, int msgIdx, int methodIdx){
		logger.debug("[RLBuilder][addRLAdviceMarker] START! ");
		IMarker marker;
		try{
			marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.LINE_NUMBER, msg.getCatchClauseLineNumber());	//mark會在哪行出現
			marker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE, msg.getCodeSmellType());
			marker.setAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION, msg.getExceptionType());
			marker.setAttribute(RLMarkerAttribute.RL_INFO_SRC_POS, String.valueOf(msg.getMethodInvocationStartPosition()));
			marker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, String.valueOf(methodIdx));
			marker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, String.valueOf(msgIdx));
			marker.setAttribute(RLMarkerAttribute.MI_WITH_Ex, msg.getExceptionType());
		}catch(CoreException e){
			logger.error("[addRLAdviceMarker] EXCEPTION ", e);
		}
		logger.debug("[RLBuilder][addRLAdviceMarker] END! ");
	}

	/**
	 * 將相關例外資訊貼上marker(CSMessage)
	 * @param file
	 * @param message
	 * @param lineNumber
	 * @param severity
	 * @param mtype
	 * @param msg
	 * @param msgIdx
	 * @param methodIdx
	 * @param lineDetailInfo	//0, 沒有try block; 1, 有try block
	 */
	private void addMarker(IFile file, String message, int lineNumber, int severity, 
			String mtype, CSMessage msg, int msgIdx, int methodIdx, int lineDetailInfo){
		logger.debug("[RLBuilder][addCSMarker] == ccu with try == START! ");
		IMarker marker;
		try{
			marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			System.out.println("RLBuilder, line 131, thisLineWillBeMarked:" + lineNumber);		
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
			marker.setAttribute(RLMarkerAttribute.CCU_WITH_TRY, lineDetailInfo);
			marker.setAttribute(RLMarkerAttribute.MI_WITH_Ex, msg.getExceptionType());

		}catch(CoreException e){
			logger.error("[addCSMarker] == ccu with try == EXCEPTION ",e);
		}
		logger.debug("[RLBuilder][addCSMarker] == ccu with try == END ! ");
	}
	
	/**
	 * 提供給其他Code smell原來的Method，careless cleanup需要用新的方法
	 * @param file
	 * @param message
	 * @param lineNumber
	 * @param severity
	 * @param mtype
	 * @param msg
	 * @param msgIdx
	 * @param methodIdx
	 */
	private void addMarker(IFile file, String message, int lineNumber, int severity, 
			String mtype, CSMessage msg, int msgIdx, int methodIdx){
		addMarker(file, message, lineNumber, severity, mtype, msg, msgIdx, methodIdx, -1);
	}
	
	/**
	 * 將相關例外資訊貼上marker(SSMessage)
	 */
	private void addMarker(IFile file, String message, int lineNumber, int severity, String mtype, SSMessage msg,
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
			marker.setAttribute(RLMarkerAttribute.RL_INFO_SRC_POS, String.valueOf(msg.getPosition()));
			marker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, String.valueOf(methodIdx));
			marker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, String.valueOf(msgIdx));
			
			marker.setAttribute(RLMarkerAttribute.SS_IN_CATCH, String.valueOf(msg.isInCatch()));

			if (msg.isFaultName()) {
				marker.setAttribute(RLMarkerAttribute.ERR_SS_FAULT_NAME, msg.getFaultName());
				for (String type : RLMarkerAttribute.CS_TOTAL_TYPE){
					boolean isAdd = false;
					for (String smell : msg.getSmellList()) {
						if (smell.equals(type)) {
							isAdd = true;
							break;
						}
					}
					marker.setAttribute(type, String.valueOf(isAdd));
				}
			}
		} catch (CoreException ex) {
			logger.error("[addMarker] EXCEPTION ",ex);
		}
		logger.debug("[RLBuilder][addMarker] END ! ");
	}
	
	/*
	 * 每次有build的時候,都會invoke這個method
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		getDetectSettings();
		
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
				
				RLAnalyzer eaVisitor = null;
				
				OverLoggingDetector loggingDetector = null;

				// 目前method的Exception資訊
				List<RLMessage> currentMethodExList = null;

				// 目前method的RL Annotation資訊
				List<RLMessage> currentMethodRLList = null;

				List<SSMessage> suppressSmellList = null;
				
				// 目前method內的ignore Exception資訊
				List<CSMessage> ignoreExList = null;
				
				// 目前method內的dummy handler資訊
				List<CSMessage> dummyList = null;
				
				// 目前method內的Nested Try Block資訊
				List<CSMessage> nestedTryList = null; 
				
				// 目前method內的Unprotected Main資訊
				List<CSMessage> unprotectedMain = null;
				
				//目前method內的Careless CleanUp資訊(in try block)
				List<CSMessage> ccuListInTry = null;
				
				//目前method內，關於RLAdvice的資訊
				List<RLAdviceMessage> eRLAdviceList = null;
				
				//目前method內的Careless CleanUp資訊(without try block)
				List<CSMessage> ccuListNoTry = null;
				
				// 目前method內的OverLogging資訊
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
					suppressSmellList = visitor.getSuppressSemllAnnotationList();

					//SuppressSmell
					TreeMap<String,Boolean> detMethodSmell = new TreeMap<String,Boolean>();
					TreeMap<String, List<Integer>> detCatchSmell = new TreeMap<String, List<Integer>>();
					//將使用者設定複製過來
					detMethodSmell = (TreeMap<String,Boolean>) detSmellSetting.clone();
					//儲存SuppressSmell設定
					inputSuppressData(suppressSmellList, detMethodSmell, detCatchSmell);

					// 找尋專案中所有的ignore Exception
					csVisitor = new CodeSmellAnalyzer(root);
					method.accept(csVisitor);
					//取得專案中的ignore Exception
					ignoreExList = csVisitor.getIgnoreExList();
					int csIdx = -1;
					//Ignore List不為Null，且使用者沒有抑制Method內所有的Ignore Marker
					if(ignoreExList != null && detMethodSmell.get(RLMarkerAttribute.CS_INGNORE_EXCEPTION)) {
						List<Integer> posList = detCatchSmell.get(RLMarkerAttribute.CS_INGNORE_EXCEPTION);
						//將每個ignore exception都貼上marker
						for(CSMessage msg : ignoreExList) {
							csIdx++;
							//判斷使用者有沒有在Catch內貼Annotation，抑制Smell Marker
							if (suppressMarker(posList, msg.getPosition()))
								continue;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]未處理!!!";
							//貼marker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);
						}
					}
					
					//取得專案中dummy handler
					dummyList = csVisitor.getDummyList();
					csIdx = -1;
					//Dummy List不為Null，且使用者沒有抑制Method內所有的Dummy Handler Marker
					if(dummyList != null && detMethodSmell.get(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
						List<Integer> posList = detCatchSmell.get(RLMarkerAttribute.CS_DUMMY_HANDLER);
						// 將每個dummy handler都貼上marker
						for (CSMessage msg : dummyList) {
							csIdx++;
							//判斷使用者有沒有在Catch內貼Annotation，抑制Smell Marker
							if (suppressMarker(posList, msg.getPosition()))
								continue;
							String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]未處理!!!";
							//貼marker
							this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
									msg.getCodeSmellType(), msg, csIdx, methodIdx);
						}
					}
					
					//取得專案中的Nested Try Block
					nestedTryList = visitor.getNestedTryList();
					csIdx = -1;
					//NestedTry List不為Null，且使用者沒有抑制Method內所有的Nested Try Marker
					if(nestedTryList != null && detMethodSmell.get(RLMarkerAttribute.CS_NESTED_TRY_BLOCK)) {
						for(CSMessage msg : nestedTryList) {
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
					/* CarelessCleanUp List不為Null，且使用者沒有抑制Method內所有的Careless CleanUp Marker */
					csIdx = -1;
					ccuListInTry = ccVisitor.getCarelessCleanUpList(true);
					ccuAddMarkerActoin(file, ccuListInTry, methodIdx, detMethodSmell, csIdx, 1);

					csIdx = -1;
					ccuListNoTry = ccVisitor.getCarelessCleanUpList(false);
					ccuAddMarkerActoin(file, ccuListNoTry, methodIdx, detMethodSmell, csIdx, 0);
					/* CarelessCleanUp List不為Null，且使用者沒有抑制Method內所有的Careless CleanUp Marker */

					//尋找專案中所有可以給予RL建議的statements
					eaVisitor = new RLAnalyzer(root);
					method.accept(eaVisitor);
					csIdx = -1;
					eRLAdviceList = eaVisitor.getExceptionRLAdviceList();
					for(RLAdviceMessage msg: eRLAdviceList){
						csIdx++;
						StringBuilder errmsg = new StringBuilder();
						if(msg.getRobustnessLevel() != null){
							for(int i = 0; i<msg.getRobustnessLevel().length; i++){
								if(msg.getRobustnessLevel()[i].getExString().contains(msg.getExceptionType())){
									errmsg.append(msg.getStatement());
									errmsg.append("對例外").append(msg.getRobustnessLevel()[i].getExString());
									errmsg.append("處理等級，");
									errmsg.append("達到RL").append(msg.getRobustnessLevel()[i].getLevel());
								}
							}
							this.addMarker(file, errmsg.toString(), IMarker.SEVERITY_INFO,
									msg, csIdx, methodIdx);
						}
					}
					
					
					//尋找該method內的OverLogging
					loggingDetector = new OverLoggingDetector(root, method);
					loggingDetector.detect();
					//取得專案中OverLogging
					overLoggingList = loggingDetector.getOverLoggingList();

					//依據所取得的code smell來貼Marker
					csIdx = -1;
					//OverLogging List不為Null，且使用者沒有抑制Method內所有的OverLogging Marker
					if(overLoggingList != null && detMethodSmell.get(RLMarkerAttribute.CS_OVER_LOGGING)){
						List<Integer> posList = detCatchSmell.get(RLMarkerAttribute.CS_OVER_LOGGING);
						for(CSMessage msg : overLoggingList) {
							csIdx++;
							//判斷使用者有沒有在Catch內貼Annotation，抑制Smell Marker
							if (suppressMarker(posList, msg.getPosition()))
								continue;
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
					//OverLogging List不為Null，且使用者沒有抑制Method內的Unprotected Main Marker
					if(unprotectedMain != null && detMethodSmell.get(RLMarkerAttribute.CS_UNPROTECTED_MAIN)) {
						for (CSMessage msg : unprotectedMain) {
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
					
					int ssIdx = -1;
					for (SSMessage msg : suppressSmellList) {
						ssIdx++;
						//Smell名稱錯誤
						if (msg.isFaultName()) {
							String errmsg = "Error EH Smell Name!";
							this.addMarker(file, errmsg, msg.getLineNumber(),
									IMarker.SEVERITY_ERROR, RLMarkerAttribute.ERR_SS_FAULT_NAME, msg, ssIdx,
									methodIdx);
						//沒有任何Smell
						} else if (msg.getSmellList().size() == 0) {
							String errmsg = "Null EH Smell Name!";
							this.addMarker(file, errmsg, msg.getLineNumber(),
									IMarker.SEVERITY_ERROR, RLMarkerAttribute.ERR_SS_NO_SMELL, msg, ssIdx,
									methodIdx);
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

	/**
	 * 因為Careless Cleanup的smell會因為在try/catch內外，而有不同的處理方式，
	 * 所以在把這個方法Extract出來，讓不同位置的Smell，加入不同的maker attribute
	 * @param file
	 * @param carelessCleanUpList
	 * @param methodIdx
	 * @param detMethodSmell
	 * @param csIdx
	 * @param codeWithTryInfo	特別拿來區分Careless Cleanup，是否在try/catch block中
	 */
	private void ccuAddMarkerActoin(IFile file,
			List<CSMessage> carelessCleanUpList, int methodIdx,
			TreeMap<String, Boolean> detMethodSmell, int csIdx, int codeWithTryInfo) {
		if(carelessCleanUpList != null && detMethodSmell.get(RLMarkerAttribute.CS_CARELESS_CLEANUP)) {
			// 將每個Careless Cleanup都貼上marker
			for(CSMessage msg : carelessCleanUpList) {
				csIdx++;
				String errmsg = "EH Smell Type:["+ msg.getCodeSmellType() + "]未處理!!!";
				//貼marker
				this.addMarker(file, errmsg, msg.getLineNumber(), IMarker.SEVERITY_WARNING,
						msg.getCodeSmellType(), msg, csIdx, methodIdx, codeWithTryInfo);
			}
		}
	}
	
	/**
	 * 儲存Suppress Smell的設定
	 * @param suppressSmellList
	 * @param detMethodSmell
	 * @param detCatchSmell
	 */
	private void inputSuppressData(List<SSMessage> suppressSmellList,
		TreeMap<String, Boolean> detMethodSmell, TreeMap<String, List<Integer>> detCatchSmell) {
		/// 初始化設定 ///
		//預設每個Smell都偵測
//		for (String smellType : RLMarkerAttribute.CS_TOTAL_TYPE)
//			detMethodSmell.put(smellType, true);

		for (String smellType : RLMarkerAttribute.CS_CATCH_TYPE)
			detCatchSmell.put(smellType, new ArrayList<Integer>());

		for (SSMessage msg : suppressSmellList) {
			//若為Method上的設定
			if (!msg.isInCatch()) {
				//若使用者偵測哪個Smell不偵測，就把該Smell偵測設定為false
				for (String smellType : msg.getSmellList())
					detMethodSmell.put(smellType, false);
			//若為Catch內的設定
			} else {
				//若使用者設定Catch內Smell不偵測，記錄該Smell所在的Catch位置
				for (String smellType : msg.getSmellList()) {
					List<Integer> smellPosList = detCatchSmell.get(smellType);
					if (smellPosList != null)
						smellPosList.add(msg.getPosition());
				}
			}
		}
	}

	/**
	 * 判斷是否要不貼Marker
	 * @param smellPosList
	 * @param pos
	 * @return
	 */
	private boolean suppressMarker(List<Integer> smellPosList, int pos) {
		for (Integer index : smellPosList)
			//若Catch位置相同，表示要抑制的Marker為同一個Marker
			if (pos == index)
				return true;

		return false;
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
	
	private void getDetectSettings(){
		Document docJDom = JDomUtil.readXMLFile();

		if(docJDom != null) {
			//從XML裡讀出之前的設定
			Element root = docJDom.getRootElement();
			if (root.getChild(JDomUtil.DetectSmellTag) != null) {
				Element rule = root.getChild(JDomUtil.DetectSmellTag).getChild("rule");
				boolean isDetAll = rule.getAttribute(JDomUtil.detect_all).getValue().equals("Y");
				if (isDetAll) {
					for (String smellType : RLMarkerAttribute.CS_TOTAL_TYPE)
						detSmellSetting.put(smellType, true);
				} else {
					for (String smellType : RLMarkerAttribute.CS_TOTAL_TYPE) {
						boolean isDet = rule.getAttribute(smellType).getValue().equals("Y");
						detSmellSetting.put(smellType, isDet);
					}
				}
			}
		} else {
			for (String smellType : RLMarkerAttribute.CS_TOTAL_TYPE)
				detSmellSetting.put(smellType, true);			
		}
	}

}
