package ntut.csie.csdet.preference;
import org.junit.Assert;
import org.junit.Test;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;

public class DetectedFileTest {
	 @Test
	 public void detectedFileTest() throws Exception{
		 IWorkspace workspace = ResourcesPlugin.getWorkspace();
		 IResource resouce = workspace.getRoot().getProject("Robusta").getFile(new Path("./test/ntut/csie/csdet/preference/DetectedFileTest.java"));
		 DetectFile detectFile = new DetectFile();
		 Assert.assertFalse(detectFile.detectedFile(resouce));
	 }
	 @Test
	 public void detectFileNotJava(){
		 IWorkspace workspace = ResourcesPlugin.getWorkspace();
		 IResource resouce = workspace.getRoot().getProject("Robusta").getFile(new Path("./lib/log4j.properties"));
		 DetectFile detectFile = new DetectFile();
		 Assert.assertFalse(detectFile.detectedFile(resouce));
	 }
	 @Test
	 public void detectNotInTest(){
		 IWorkspace workspace = ResourcesPlugin.getWorkspace();
		 IResource resouce = workspace.getRoot().getProject("Robusta").getFile(new Path("./src/ntut/csie/csdet/preference/JDomUtil.java"));
		 DetectFile detectFile = new DetectFile();
		 Assert.assertTrue(detectFile.detectedFile(resouce));
	 }
	 @Test
	 public void detectNotIFile(){
		 IWorkspace workspace = ResourcesPlugin.getWorkspace();
		 IResource resouce = workspace.getRoot().getProject("Robusta").getFolder(new Path("./src"));
		 DetectFile detectFile = new DetectFile();
		 Assert.assertFalse(detectFile.detectedFile(resouce));
	 }
	 @Test
	 public void detectNullExtentionFile(){
		 IWorkspace workspace = ResourcesPlugin.getWorkspace();
		 IResource resouce = workspace.getRoot().getProject("Robusta").getFile(new Path("./FirstTest/src/test"));
		 DetectFile detectFile = new DetectFile();
		 Assert.assertFalse(detectFile.detectedFile(resouce));
	 }
	 
	 
	 
	 
	 
	 
}
