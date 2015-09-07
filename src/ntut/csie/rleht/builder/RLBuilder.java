package ntut.csie.rleht.builder;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.SuppressWarningVisitor;
import ntut.csie.analyzer.UserDefinedMethodAnalyzer;
import ntut.csie.csdet.data.SSMessage;
import ntut.csie.csdet.preference.RobustaSettings;
import ntut.csie.csdet.preference.SmellSettings;
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
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
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
	private IMarker addMarker(IFile file, String message, int lineNumber, int severity, String mtype, RLMessage msg,
			int msgIdx, int methodIdx) {
		IMarker marker = null;
		try {
			marker = file.createMarker(MARKER_TYPE);
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
		
		return marker;
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
		loadRobustaSettingForProject(getProject());

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
	/* 針對每一個Java程式的Method
	/* 標記專案中所有的壞味道&
	 * 檢查RLAnnotation */
	private void checkBadSmells(IResource resource) {
		if (isJavaFile(resource)) {
			IFile file = (IFile) resource;
			
			// with editor tracker, we no longer need to add all markers at once during proj build
//			markerModel.deleteMarkers(file);
//			try {
//				markerModel.applyMarkers(file);				
//			} catch (Exception e) {
//				logger.error("Fail to apply marker onto file: " + file.toString() + " \n", e);
//				throw new RuntimeException(e);
//			}
			
			try {
				applyRLAnnotation(resource);
			}
			catch (Exception ex) {
				logger.error("[checkRLAnnotation] EXCEPTION ",ex);
				throw new RuntimeException(ex);
			}
		}
	}

	private CompilationUnit getRoot(IResource resource) {
		IJavaElement javaElement = JavaCore.create(resource);

		ASTParser parser = ASTParser.newParser(AST.JLS3);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		parser.setSource((ICompilationUnit) javaElement);
		parser.setResolveBindings(true);
		
		CompilationUnit root = (CompilationUnit) parser.createAST(null);
		return root;
	}

	private void applyRLAnnotation(IResource resource) {
		ExceptionAnalyzer visitor;
		List<RLMessage> currentMethodExList;
		List<RLMessage> currentMethodRLList;
		List<SSMessage> suppressSmellList;
		int methodIdx = -1;
		
		IFile file = (IFile) resource;
		CompilationUnit root = getRoot(resource);
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		root.accept(methodCollector);
		List<MethodDeclaration> methodList = methodCollector.getMethodList();
		
		for (MethodDeclaration method : methodList) {
			methodIdx++;

			visitor = new ExceptionAnalyzer(root, method.getStartPosition(), 0);
			method.accept(visitor);
			currentMethodRLList = visitor.getMethodRLAnnotationList();
			SuppressWarningVisitor swVisitor = new SuppressWarningVisitor(root);
			method.accept(swVisitor);
			suppressSmellList = swVisitor.getSuppressWarningList();

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

	private boolean isJavaFile(IResource resource) {
		try {
			return resource.getFileExtension().equals("java");
		} catch (NullPointerException e) {
			// maybe resource has no extension
			return false;
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
			IJavaElement javaElement = JavaCore.create(resource);
			/**
			 * javaElement is null when unable to associate the given resource with a Java element.
			 * A folder will have to type: PackageFragment or PackageFragmentRoot, if the folder is
			 * not these folder, so we should not visit it (javaElement == null).
			 **/
			if(javaElement == null)
				return false;
			//Check if it was set not to be detect in robusta setting
			String folderName = resource.getFullPath().segment(1);
			return robustaSettings.getProjectDetectAttribute(folderName);
		}
		return true;
	}
		
	private void loadRobustaSettingForProject(IProject project)
	{
		robustaSettings = new RobustaSettings(
				UserDefinedMethodAnalyzer.getRobustaSettingXMLPath(project),
				project);
	}

}
