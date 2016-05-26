package ntut.csie.aspect;

import java.util.HashMap;
import java.util.Map;

import ntut.csie.filemaker.RuntimeEnvironmentProjectReader;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;

public class MockMarker implements IMarker{

	HashMap map = new HashMap();
	IResource resource = null;
	public MockMarker(){
		try {
			resource = RuntimeEnvironmentProjectReader.getType("AddAspectsMarkerResoluationExampleProject",
					AddAspectsMarkerResoluationExample.class.getPackage().getName(),
					AddAspectsMarkerResoluationExample.class.getSimpleName()).getResource();
		} catch (JavaModelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete() throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Object getAttribute(String attributeName) throws CoreException {
		// TODO Auto-generated method stub
		return (Object) map.get(attributeName);
	}

	@Override
	public int getAttribute(String attributeName, int defaultValue) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getAttribute(String attributeName, String defaultValue) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean getAttribute(String attributeName, boolean defaultValue) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Map<String, Object> getAttributes() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object[] getAttributes(String[] attributeNames) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getCreationTime() throws CoreException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public long getId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IResource getResource() {
		// TODO Auto-generated method stub
		return resource;
	}

	@Override
	public String getType() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSubtypeOf(String superType) throws CoreException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setAttribute(String attributeName, int value) throws CoreException {
		map.put(attributeName, value);
	}
	
	public void setAttribute(String attributeName, String value) throws CoreException {
		map.put(attributeName, value);
	}

	@Override
	public void setAttribute(String attributeName, Object value) throws CoreException {
		map.put(attributeName, value);
	}

	@Override
	public void setAttribute(String attributeName, boolean value)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAttributes(String[] attributeNames, Object[] values)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAttributes(Map<String, ? extends Object> attributes)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

}
