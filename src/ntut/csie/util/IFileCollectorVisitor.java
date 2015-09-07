package ntut.csie.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

public class IFileCollectorVisitor implements IResourceVisitor {

	private List<IFile> iFileList;
	
	public IFileCollectorVisitor () {
		iFileList = new ArrayList<IFile>();
	}
	
	public List<IFile> getIFiles() {
		return iFileList;
	}

	@Override
	public boolean visit(IResource resource) throws CoreException {
		if(resource instanceof IFile && resource.getName().endsWith(".java")) {
			iFileList.add((IFile) resource);			
			return false;
		}
		return true;
	}
}
