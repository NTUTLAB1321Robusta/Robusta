package ntut.csie.csdet.preference;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;

public class DetectFile {
	public boolean detectedFile(IResource resource) {
		if (resource instanceof IFile
				&& resource.getFileExtension() != null
				&& !resource.getFullPath().segment(1).toLowerCase()
						.contains("test")
				&& resource.getFileExtension().equals("java")) {
			return true;

		} else {
			return false;
		}

	}

}
