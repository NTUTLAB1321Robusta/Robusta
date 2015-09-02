package ntut.csie.robusta.marker;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ntut.csie.csdet.data.MarkerInfo;

// Data Object class for classes other than the model to read the data
// Still only the model can manipulate the data
public class MarkerData {

	private static Logger logger = LoggerFactory.getLogger(MarkerData.class);
	private static List<MarkerInfo> markerList = new ArrayList<MarkerInfo>(32);
	
	public static void printAllMarkerData() {
		for(MarkerInfo mi : markerList) {
			logger.debug("Method Name:" + mi.getMethodName() + "\n" + "Statement:" + mi.getStatement() + "\n\n");
		}
		logger.debug("bad smell list size:" + markerList.size());
	}

	public static List<MarkerInfo> getMarkerInfo() {
		if(((ArrayList<MarkerInfo>)markerList).clone() != null)
			return (List<MarkerInfo>)((ArrayList<MarkerInfo>)markerList).clone();
		else
			return null;
	}
	
	protected void clear() {
		markerList.clear();
	}
	
	protected void append(List<MarkerInfo> markerInfoList) {
		markerList.addAll(markerInfoList);
	}

	protected void remove(IFile file) {
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
	
	public static List<AnnotationInfo> getAnnotationInfo(IResource correspondingResource) {
		List<AnnotationInfo> annotationPosList = new ArrayList<AnnotationInfo>();

		for(MarkerInfo mi : markerList) {
			if(mi.getClassName().equals(correspondingResource.getName()))
				annotationPosList.addAll(mi.getAnnotationPosList());
		}
		
		return annotationPosList;
	}
}
