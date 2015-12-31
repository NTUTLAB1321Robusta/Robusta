package ntut.csie.robusta.marker;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import ntut.csie.analyzer.ASTMethodCollector;
import ntut.csie.analyzer.BadSmellCollector;
import ntut.csie.csdet.data.MarkerInfo;
import ntut.csie.rleht.builder.RLMarkerAttribute;
import ntut.csie.util.IFileCollectorVisitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MarkerModel {

	private static Logger logger = LoggerFactory.getLogger(MarkerModel.class);
	
	private ResourceBundle resourceBundle = ResourceBundle.getBundle("robusta", new Locale("en", "US"));
	
	public static final String MARKER_TYPE_BAD_SMELL = "ntut.csie.robusta.marker.BSProblem";
	
	private List<IProject> projectRegisteredMarkerService = new ArrayList<IProject>();
	
	private static List<MarkerInfo> markerList = new ArrayList<MarkerInfo>();
	
	public MarkerModel() {
		super();
	}

	private void applyMarkers(IFile file) {
		CompilationUnit root = getRoot(file);
		ASTMethodCollector methodCollector = new ASTMethodCollector();
		root.accept(methodCollector);
		
		BadSmellCollector badSmellCollector = new BadSmellCollector(file.getProject(), root);
		badSmellCollector.collectBadSmell();
		List<MarkerInfo> badSmellList = badSmellCollector.getAllBadSmells();
		
		// for each class we scan, store all the bad smells that has been found
		markerList.addAll(badSmellList);
		
		for(int i = 0; i<badSmellList.size(); i++)
		{
			MarkerInfo markerInfo = badSmellList.get(i);
			String errmsg = this.resourceBundle.getString("ex.smell.type.undealt") + markerInfo.getCodeSmellType() + this.resourceBundle.getString("ex.smell.type");
			this.addMarker(file, errmsg, IMarker.SEVERITY_WARNING, markerInfo, markerInfo.getBadSmellIndex(), markerInfo.getMethodIndex());		
		}
	}
	
	private IMarker addMarker(IFile file, String errmsg, int severityLevel,
			MarkerInfo markerInfo, int csIdx, int methodIdx) {
		IMarker marker = null;
		try{
			marker = file.createMarker(MARKER_TYPE_BAD_SMELL);
			marker.setAttribute(IMarker.MESSAGE, errmsg);
			marker.setAttribute(IMarker.SEVERITY, severityLevel);
			if (markerInfo.getLineNumber() == -1) {
				markerInfo.setLineNumber(1);
			}
			marker.setAttribute(IMarker.LINE_NUMBER, markerInfo.getLineNumber());
			marker.setAttribute(RLMarkerAttribute.RL_MARKER_TYPE, markerInfo.getCodeSmellType());
			marker.setAttribute(RLMarkerAttribute.RL_INFO_EXCEPTION, markerInfo.getExceptionType());
			marker.setAttribute(RLMarkerAttribute.RL_INFO_SRC_POS, String.valueOf(markerInfo.getPosition()));
			marker.setAttribute(RLMarkerAttribute.RL_METHOD_INDEX, String.valueOf(methodIdx));
			marker.setAttribute(RLMarkerAttribute.RL_MSG_INDEX, String.valueOf(csIdx));
			marker.setAttribute(RLMarkerAttribute.CCU_WITH_TRY, markerInfo.getIsInTry());
			marker.setAttribute(RLMarkerAttribute.MI_WITH_Ex, markerInfo.getExceptionType());
			marker.setAttribute(RLMarkerAttribute.RL_INFO_SUPPORT_REFACTORING, markerInfo.isSupportRefactoring());
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
		
		return marker;
	}
	
	private CompilationUnit getRoot(IResource resource) {
		CompilationUnit root = null;
		try {
			IJavaElement javaElement = JavaCore.create(resource);
			ASTParser parser = ASTParser.newParser(AST.JLS3);
			parser.setKind(ASTParser.K_COMPILATION_UNIT);
			parser.setSource((ICompilationUnit) javaElement);
			parser.setResolveBindings(true);
			
			root = (CompilationUnit) parser.createAST(null);
		} catch (Exception e) {
			logger.error("Fail to get compilation unit from source", e);
			throw new RuntimeException(e);
		}
		return root;
	}
	
	public void deleteMarkers(IProject project) {
		try {
			IFileCollectorVisitor iFileCollectorVisitor = new IFileCollectorVisitor();
			project.accept(iFileCollectorVisitor);
			
			for(IFile file : iFileCollectorVisitor.getIFiles()) {
				deleteMarkers(file);
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void deleteMarkers(IFile file) {
		try {
			file.deleteMarkers(MARKER_TYPE_BAD_SMELL, false, IResource.DEPTH_ZERO);
			removeMarkerInfo(file);
		}
		catch (CoreException ce) {
			throw new RuntimeException("Fail to clean up old markers", ce);
		}
	}

	private void removeMarkerInfo(IFile file) {
		// to avoid concurrent modification of the marker data
		List<MarkerInfo> removeList = new ArrayList<MarkerInfo>();
		
		for(MarkerInfo mi : markerList) {
			if(mi.getClassName().equals(file.getName()))
				removeList.add(mi);
		}
		
		for(MarkerInfo mi : removeList) {
			markerList.remove(mi);			
		}
	}
	
	public void updateMarker(IFile file) {
		IProject project = file.getProject();
		if(projectRegisteredMarkerService.contains(project)) {
			deleteMarkers(file);
			applyMarkers(file);			
		}
	}
	
	public List<AnnotationInfo> getAnnotationInfo(IResource correspondingResource) {
		List<AnnotationInfo> annotationPosList = new ArrayList<AnnotationInfo>();

		for(MarkerInfo mi : markerList) {
			if(mi.getClassName().equals(correspondingResource.getName()))
				annotationPosList.addAll(mi.getAnnotationList());
		}
		
		return annotationPosList;
	}
	
	public void registerMarkerService(IProject project) {
		if(!projectRegisteredMarkerService.contains(project))
		this.projectRegisteredMarkerService.add(project);
	}
	
	public void unregisterMarkerService(IProject project) {
		if(projectRegisteredMarkerService.contains(project))
			projectRegisteredMarkerService.remove(project);
	}
	
	public boolean hasRegisteredMarkerService(IProject project) {
		return (projectRegisteredMarkerService.contains(project))? true : false;
	}
}
