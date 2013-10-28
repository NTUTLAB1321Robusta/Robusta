package ntut.csie.rleht.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.csdet.data.SSMessage;
import ntut.csie.csdet.preference.JDomUtil;
import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.csdet.preference.SmellSettings;
import ntut.csie.csdet.visitor.CarelessCleanupVisitor;
import ntut.csie.csdet.visitor.DummyHandlerVisitor;
import ntut.csie.csdet.visitor.IgnoreExceptionVisitor;
import ntut.csie.csdet.visitor.NestedTryStatementVisitor;
import ntut.csie.csdet.visitor.OverLoggingDetector;
import ntut.csie.csdet.visitor.OverwrittenLeadExceptionVisitor;
import ntut.csie.csdet.visitor.SuppressWarningVisitor;
import ntut.csie.csdet.visitor.UnprotectedMainProgramVisitor;
import ntut.csie.csdet.visitor.UserDefinedMethodAnalyzer;
import ntut.csie.java.util.CastingObject;
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
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RLBuilder extends IncrementalProjectBuilder {
	private static Logger logger = LoggerFactory.getLogger(RLBuilder.class);
	
	public static final String BUILDER_ID = "ntut.csie.rleht.builder.RLBuilder";

	// 延伸problem view好讓自己的marker可以加進去view中
	public static final String MARKER_TYPE = "ntut.csie.rleht.builder.RLProblem";

	// 使用者所設定的是否偵測EH Smell設定
	private TreeMap<String, Boolean> detSmellSetting = new TreeMap<String, Boolean>();
	
	private ResourceBundle resource = ResourceBundle.getBundle("robusta", new Locale("en", "US"));

	private RobustaSettings robustaSettings;

	/**
	 * 將相關例外資訊貼上marker(RLMessage)
	 * 使用於@RL時 
	 */
	private void addMarker(IFile file, String message, int lineNumber, int severity, String mtype, RLMessage msg,
			int msgIdx, int methodIdx) {
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
	}
	
	/**
	 * 將相關例外資訊貼上marker
	 * @param file
	 * @param errmsg
	 * @param severityLevel
	 * @param markerInfo
	 * @param csIdx
	 * @param methodIdx
	 * @author Crimson
	 */
	private void addMarker(IFile file, String errmsg, int severityLevel,
			MarkerInfo markerInfo, int csIdx, int methodIdx) {
		IMarker marker;
		try{
			marker = file.createMarker(MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, errmsg);
			marker.setAttribute(IMarker.SEVERITY, severityLevel);
			if (markerInfo.getLineNumber() == -1) {
				markerInfo.setLineNumber(1);
			}
			marker.setAttribute(IMarker.LINE_NUMBER, markerInfo.getLineNumber());
			//marker type =  EH smell type
			marker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE, markerInfo.getCodeSmellType());
			marker.setAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION, markerInfo.getExceptionType());
			marker.setAttribute(RLMarkerAttribute.RL_INFO_SRC_POS, String.valueOf(markerInfo.getPosition()));
			marker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, String.valueOf(methodIdx));
			marker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, String.valueOf(csIdx));
			marker.setAttribute(RLMarkerAttribute.CCU_WITH_TRY, markerInfo.getIsInTry());
			marker.setAttribute(RLMarkerAttribute.MI_WITH_Ex, markerInfo.getExceptionType());
		} catch (CoreException e) {
			logger.error("[addMarker] Exception ",e);
		}
	}

	/**
	 * 將相關例外資訊貼上marker(SSMessage)
	 */
	private void addMarker(IFile file, String message, int lineNumber, int severity, String mtype, SSMessage msg,
			int msgIdx, int methodIdx) {

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
	}
	
	/*
	 * this method will be invoked every build
	 * 
	 * @see org.eclipse.core.internal.events.InternalBuilder#build(int,
	 *      java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor) throws CoreException {
		getDetectSettings();
		loadRobustaSettingForProject(getProject().getName());
		
		logger.debug("[RLBuilder] START !!");
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
		logger.debug("[RLBuilder] END !!");
		System.out.println("RLBuild花費時間 " + (end - start) + " milli second.");
		return null;
	}

	/**
	 * 進行fullBuild or inrementalBuild時,都會去呼叫這個method
	 * @param resource
	 */
	private void checkBadSmells(IResource resource) {
		if (isJavaFile(resource)) {
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
				
				// 取得專案中所有的method
				List<MethodDeclaration> methodList = methodCollector.getMethodList();

				ExceptionAnalyzer visitor = null;
				
				UnprotectedMainProgramVisitor mainVisitor = null;
				
				// RLAnalyzer eaVisitor = null;
				
				OverLoggingDetector loggingDetector = null;

				// 目前method的Exception資訊
				List<RLMessage> currentMethodExList = null;

				// 目前method的RL Annotation資訊
				List<RLMessage> currentMethodRLList = null;

				List<SSMessage> suppressSmellList = null;
				
				// 目前method內的Unprotected Main資訊
				List<MarkerInfo> unprotectedMain = null;
				
				// 目前method內的OverLogging資訊
				List<MarkerInfo> overLoggingList = null;
				
				// 找尋專案中所有的Overwritten Lead Exception
				// FIXME 未加入 SuppressSmell 的作用, 或許可參考 ReportBuilder 的作法
				OverwrittenLeadExceptionVisitor oleVisitor = new OverwrittenLeadExceptionVisitor(root);
				root.accept(oleVisitor);
				List<MarkerInfo> overwrittenList = oleVisitor.getOverwrittenList();
				for(int oleIndex = 0; oleIndex < overwrittenList.size(); oleIndex++) {
					MarkerInfo markerInfo = overwrittenList.get(oleIndex);
					String errmsg = this.resource.getString("ex.smell.type.undealt") + markerInfo.getCodeSmellType() + this.resource.getString("ex.smell.type");
					this.addMarker(file, errmsg, IMarker.SEVERITY_WARNING, markerInfo, oleIndex, -1);  // methodIdx 沒有用，故先給-1
				}

				// 找尋專案中所有的 nested try
				// FIXME 未加入 SuppressSmell 的作用, 或許可參考 ReportBuilder 的作法
				NestedTryStatementVisitor nestedTryStatementVisitor = new NestedTryStatementVisitor(root);
				root.accept(nestedTryStatementVisitor);
				List<MarkerInfo> nestedTryList = nestedTryStatementVisitor.getNestedTryStatementList();
				int ntsIndex = -1;
				for(MarkerInfo markerInfo : nestedTryList) {
					ntsIndex++;
					String errmsg = this.resource.getString("ex.smell.type.undealt") + markerInfo.getCodeSmellType() + this.resource.getString("ex.smell.type");
					this.addMarker(file, errmsg, IMarker.SEVERITY_WARNING, markerInfo, ntsIndex, -1);  // methodIdx 先給-1
				}
				
				//Detect dummy handler for initializer
				DummyHandlerVisitor dmhVisitor = new DummyHandlerVisitor(root);
				ASTInitializerCollector initializerCollector = new ASTInitializerCollector();
				root.accept(initializerCollector);
				for(Initializer init : initializerCollector.getInitializerList())
					init.accept(dmhVisitor);
				List<MarkerInfo> dmList = dmhVisitor.getDummyList();
				for(int dmIndex = 0; dmIndex < dmList.size(); dmIndex++) {
					MarkerInfo markerInfo = dmList.get(dmIndex);
					String errmsg = this.resource.getString("ex.smell.type.undealt") + markerInfo.getCodeSmellType() + this.resource.getString("ex.smell.type");
					this.addMarker(file, errmsg, IMarker.SEVERITY_WARNING, markerInfo, dmIndex, -1);  // methodIdx = -1 => not support quickfix right now
				}
				
				
				int methodIdx = -1;
				for (MethodDeclaration method : methodList) {
					methodIdx++;

					visitor = new ExceptionAnalyzer(root, method.getStartPosition(), 0);
					method.accept(visitor);
					currentMethodRLList = visitor.getMethodRLAnnotationList();
					SuppressWarningVisitor swVisitor = new SuppressWarningVisitor(root);
					method.accept(swVisitor);
					suppressSmellList = swVisitor.getSuppressWarningList();

					// SuppressSmell
					TreeMap<String,Boolean> detMethodSmell = new TreeMap<String,Boolean>();
					TreeMap<String, List<Integer>> detCatchSmell = new TreeMap<String, List<Integer>>();
					// 將使用者設定複製過來
					detMethodSmell = CastingObject.castTreeMap(detSmellSetting.clone(), String.class, Boolean.class);

					// 儲存SuppressSmell設定
					inputSuppressData(suppressSmellList, detMethodSmell, detCatchSmell);
					
					// 找尋專案中所有的ignore Exception
					IgnoreExceptionVisitor ieVisitor = new IgnoreExceptionVisitor(root);
					method.accept(ieVisitor);
					List<MarkerInfo> ignoreList = ieVisitor.getIgnoreList();
					int csIdx = -1;
					if(detMethodSmell.get(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK)) {
						List<Integer> posList = detCatchSmell.get(RLMarkerAttribute.CS_EMPTY_CATCH_BLOCK);
						for(MarkerInfo markerInfo : ignoreList) {
							csIdx++;
							// 判斷使用者有沒有在Catch內貼Annotation，抑制Smell Marker
							if (suppressMarker(posList, markerInfo.getPosition()))
								continue;
							String errmsg = this.resource.getString("ex.smell.type.undealt") + markerInfo.getCodeSmellType() + this.resource.getString("ex.smell.type");
							this.addMarker(file, errmsg, IMarker.SEVERITY_WARNING, markerInfo, csIdx, methodIdx);
						}
					}
					
					// 取得專案中dummy handler
					DummyHandlerVisitor dhVisitor = new DummyHandlerVisitor(root);
					method.accept(dhVisitor);
					List<MarkerInfo> dummyList = dhVisitor.getDummyList();
					csIdx = -1;
					// Dummy List不為Null，且使用者沒有抑制Method內所有的Dummy Handler Marker
					if(detMethodSmell.get(RLMarkerAttribute.CS_DUMMY_HANDLER)) {
						List<Integer> posList = detCatchSmell.get(RLMarkerAttribute.CS_DUMMY_HANDLER);
						// 將每個dummy handler都貼上marker
						for (MarkerInfo markerInfo : dummyList) {
							csIdx++;
							// 判斷使用者有沒有在Catch內貼Annotation，抑制Smell Marker
							if (suppressMarker(posList, markerInfo.getPosition()))
								continue;
							String errmsg = this.resource.getString("ex.smell.type.undealt") + markerInfo.getCodeSmellType() + this.resource.getString("ex.smell.type");
							// 貼marker
							this.addMarker(file, errmsg, IMarker.SEVERITY_WARNING, markerInfo, csIdx, methodIdx);
						}
					}

					// 找尋專案中所有的Careless Cleanup
					CarelessCleanupVisitor carelessCleanupVisitor = new CarelessCleanupVisitor(root);
					method.accept(carelessCleanupVisitor);
					List<MarkerInfo> carelessCleanupList = carelessCleanupVisitor.getCarelessCleanupList();
					csIdx = -1;
					if(detMethodSmell.get(RLMarkerAttribute.CS_CARELESS_CLEANUP)) {
						for(MarkerInfo markerInfo : carelessCleanupList) {
							csIdx++;
							String errmsg = this.resource.getString("ex.smell.type.undealt") + markerInfo.getCodeSmellType() + this.resource.getString("ex.smell.type");
							// 貼Marker
							this.addMarker(file, errmsg, IMarker.SEVERITY_WARNING, markerInfo, csIdx, methodIdx);
						}
					}

					// 尋找專案中所有可以給予RL建議的statements
//					eaVisitor = new RLAnalyzer(root);
//					method.accept(eaVisitor);
//					csIdx = -1;
//					eRLAdviceList = eaVisitor.getExceptionRLAdviceList();
//					for(RLAdviceMessage msg: eRLAdviceList){
//						csIdx++;
//						StringBuilder errmsg = new StringBuilder();
//						if(msg.getRobustnessLevel() != null){
//							for(int i = 0; i<msg.getRobustnessLevel().length; i++){
//								if(msg.getRobustnessLevel()[i].getExString().contains(msg.getExceptionType())){
//									errmsg.append(msg.getStatement());
//									errmsg.append("對例外").append(msg.getRobustnessLevel()[i].getExString());
//									errmsg.append("處理等級，");
//									errmsg.append("達到RL").append(msg.getRobustnessLevel()[i].getLevel());
//								}
//							}
//							this.addMarker(file, errmsg.toString(), IMarker.SEVERITY_INFO,
//									msg, csIdx, methodIdx);
//						}
//					}
					
					// 尋找該method內的OverLogging
					loggingDetector = new OverLoggingDetector(root, method);
					if(loggingDetector != null) {
						loggingDetector.detect();
						// 取得專案中OverLogging
						overLoggingList = loggingDetector.getOverLoggingList();
	
						// 依據所取得的code smell來貼Marker
						csIdx = -1;
						// 使用者沒有抑制Method內所有的OverLogging Marker
						if(detMethodSmell.get(RLMarkerAttribute.CS_OVER_LOGGING)){
							List<Integer> posList = detCatchSmell.get(RLMarkerAttribute.CS_OVER_LOGGING);
							for(MarkerInfo msg : overLoggingList) {
								csIdx++;
								// 判斷使用者有沒有在Catch內貼Annotation，抑制Smell Marker
								if (suppressMarker(posList, msg.getPosition()))
									continue;
								String errmsg = this.resource.getString("ex.smell.type.undealt") + msg.getCodeSmellType() + this.resource.getString("ex.smell.type");
								// 貼marker
								this.addMarker(file, errmsg, IMarker.SEVERITY_WARNING, msg, csIdx, methodIdx);	
							}
						}
					}
					
					// 尋找該method內的unprotected main program
					mainVisitor = new UnprotectedMainProgramVisitor(root);
					method.accept(mainVisitor);
					unprotectedMain = mainVisitor.getUnprotedMainList();

					// 依據所取得的code smell來貼Marker
					csIdx = -1;
					// 使用者沒有抑制Method內的Unprotected Main Marker
					if(detMethodSmell.get(RLMarkerAttribute.CS_UNPROTECTED_MAIN)) {
						for (MarkerInfo msg : unprotectedMain) {
							csIdx++;
							String errmsg = this.resource.getString("ex.smell.type.undealt") + msg.getCodeSmellType() + this.resource.getString("ex.smell.type");
							// 貼marker
							this.addMarker(file, errmsg, IMarker.SEVERITY_WARNING, msg, csIdx, methodIdx);	
						}
					}
							
					RLChecker checker = new RLChecker();
					currentMethodExList = checker.check(visitor);
					
					// 檢查@RL是否存在(丟出的例外是否被註記)
					int msgIdx = -1;
					for (RLMessage msg : currentMethodExList) {
						msgIdx++;
						if (msg.getRLData().getLevel() >= 0) {
							if (!msg.isHandling()) {
								SmellSettings smellSettings = new SmellSettings(UserDefinedMethodAnalyzer.SETTINGFILEPATH);
								if(smellSettings.getPreferenceAttribute(SmellSettings.PRE_SHOWRLANNOTATIONWARNING))
								{
									String errmsg = this.resource.getString("tag.undefine1") + msg.getRLData().getExceptionType() + this.resource.getString("tag.undefine2");
									this.addMarker(file, errmsg.toString(), msg.getLineNumber(), IMarker.SEVERITY_WARNING,
											RLMarkerAttribute.ERR_NO_RL, msg, msgIdx, methodIdx);
								}
							}
						}
					}
					
					int ssIdx = -1;
					for (SSMessage msg : suppressSmellList) {
						ssIdx++;
						// Smell名稱錯誤
						if (msg.isFaultName()) {
							String errmsg = this.resource.getString("error.smell.name");
							this.addMarker(file, errmsg, msg.getLineNumber(),
									IMarker.SEVERITY_ERROR, RLMarkerAttribute.ERR_SS_FAULT_NAME, msg, ssIdx,
									methodIdx);
						// 沒有任何Smell
						} else if (msg.getSmellList().size() == 0) {
							String errmsg = this.resource.getString("null.smell.name");
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
							String errmsg = this.resource.getString("tag.level1") + msg.getRLData().getLevel() + 
											this.resource.getString("tag.level2") + msg.getRLData().getExceptionType() + 
											this.resource.getString("tag.level3");

							this.addMarker(file, errmsg, lineNumber, IMarker.SEVERITY_ERROR,
									RLMarkerAttribute.ERR_RL_LEVEL, msg, msgIdx, methodIdx);
						}

						// 檢查@RL清單內的exception類別階層是否正確
						int idx2 = 0;
						for (RLMessage msg2 : currentMethodRLList) {
							if (msgIdx >= idx2++) {
								continue;
							}

							if (msg.getRLData().getExceptionType().equals(msg2.getRLData().getExceptionType())) {
								this.addMarker(file, this.resource.getString("tag.level1") + msg.getRLData().getLevel() + this.resource.getString("tag.level2")
										+ msg.getRLData().getExceptionType() + this.resource.getString("tag.level4"), lineNumber,
										IMarker.SEVERITY_ERROR, RLMarkerAttribute.ERR_RL_DUPLICATE, msg, msgIdx,
										methodIdx);
							}
							else if (ASTHandler.isInstance(msg2.getTypeBinding(), msg.getTypeBinding()
									.getQualifiedName())) {
								this.addMarker(file, this.resource.getString("tag.level1") + msg.getRLData().getLevel() + this.resource.getString("tag.level2")
										+ msg.getRLData().getExceptionType() + this.resource.getString("tag.level5")
										+ msg2.getRLData().getLevel() + this.resource.getString("tag.level2")
										+ msg2.getRLData().getExceptionType() + this.resource.getString("tag.level6"), lineNumber,
										IMarker.SEVERITY_ERROR, RLMarkerAttribute.ERR_RL_INSTANCE, msg, msgIdx,
										methodIdx);
							}
						}
					}
				}
			}
			catch (Exception ex) {
				logger.error("[checkRLAnnotation] EXCEPTION ",ex);
				throw new RuntimeException(ex);
			}
		}
	}
	
	/**
	 * check if the resource is java file
	 */
	private boolean isJavaFile(IResource resource) {
		try {
			return resource.getFileExtension().equals("java");
		} catch (NullPointerException e) {
			// maybe resource has no extension
			return false;
		}
	}
	
	private void inputSuppressData(List<SSMessage> suppressSmellList,
		TreeMap<String, Boolean> detMethodSmell, TreeMap<String, List<Integer>> detCatchSmell) {
		/// 初始化設定 ///
		// 預設每個Smell都偵測
//		for (String smellType : RLMarkerAttribute.CS_TOTAL_TYPE)
//			detMethodSmell.put(smellType, true);

		for (String smellType : RLMarkerAttribute.CS_CATCH_TYPE)
			detCatchSmell.put(smellType, new ArrayList<Integer>());

		for (SSMessage msg : suppressSmellList) {
			// 若為Method上的設定
			if (!msg.isInCatch()) {
				// 若使用者偵測哪個Smell不偵測，就把該Smell偵測設定為false
				for (String smellType : msg.getSmellList())
					detMethodSmell.put(smellType, false);
			// 若為Catch內的設定
			} else {
				// 若使用者設定Catch內Smell不偵測，記錄該Smell所在的Catch位置
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
		if(smellPosList != null) {
			for (Integer index : smellPosList)
				// 若Catch位置相同，表示要抑制的Marker為同一個Marker
				if (pos == index)
					return true;
		}
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
		getProject().accept(new RLResourceVisitor());
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) throws CoreException {
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
			/*
			 * do nothing when the IResourceDelta is REMOVED
			 * otherwise have to call checkBadSmells()
			 */
			if(delta.getKind() != IResourceDelta.REMOVED) {
				if (!shouldGoInInside(resource))
					return false;
				checkBadSmells(resource);
			} 
			// return true to continue visiting children.
			return true;
		}
	}

	class RLResourceVisitor implements IResourceVisitor {
		public boolean visit(IResource resource) {
			if (!shouldGoInInside(resource))
				return false;
			checkBadSmells(resource);
			// return true to continue visiting children.
			return true;
		}
	}
	
	private boolean shouldGoInInside(IResource resource) {
		if (resource.getType() == IResource.FOLDER) {
			//Check if it is in PackageFragmentRoot or not
			IProject project = resource.getProject();
			try {
				if (project.isNatureEnabled(JavaCore.NATURE_ID)) {
					IJavaProject javaProject = JavaCore.create(project);
					if (javaProject.findPackageFragmentRoot(resource.getFullPath().uptoSegment(2)) == null)
						return false;
				}
			} catch (Exception ex) {
				logger.error("[RLBuilder] EXCEPTION ", ex);
			}
			//Check if it was set not to be detect in robusta setting
			String folderName = resource.getFullPath().segment(1);
			return robustaSettings.getProjectDetectAttribute(folderName);
		}
		return true;
	}
	
	private void getDetectSettings(){
		Document docJDom = JDomUtil.readXMLFile();

		if(docJDom != null) {
			// 從XML裡讀出之前的設定
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
	
	private void loadRobustaSettingForProject(String projectName)
	{
		robustaSettings = new RobustaSettings(
				UserDefinedMethodAnalyzer.getRobustaSettingXMLPath(projectName),
				projectName);
	}

}
