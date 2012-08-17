package ntut.csie.filemaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class JarFileMaker {
	public int BUFFER_SIZE = 10240;
	/**
	 * @param archiveFile 指定產生Jar檔的路徑 + 檔名
	 * @param packageList 傳入bin的listFile
	 */
	public void createJarFile(File archiveFile, File[] packageList) {
	    try {
	    	byte buffer[] = new byte[BUFFER_SIZE];
	    	// Open archive file
	    	FileOutputStream stream = new FileOutputStream(archiveFile);
	    	// Write manifest
	    	Manifest manifest = new Manifest();
	    	Attributes attributes = manifest.getMainAttributes();
	    	attributes.putValue("Manifest-Version", "1.0");
	    	// Open jar file
	    	JarOutputStream out = new JarOutputStream(stream, manifest);
	    	File agilePackage = packageList[0];
	    	File exceptionPackage = agilePackage.listFiles()[0];
	    	File[] tobeJared = exceptionPackage.listFiles();
	    	
	    	for (int i = 0; i < tobeJared.length; i++) {
	    		if (tobeJared[i] == null || !tobeJared[i].exists() || tobeJared[i].isDirectory())
	    			continue; // Just in case...
		    	System.out.println("Adding " + tobeJared[i].getName());
			
				// Add archive entry
				JarEntry fileEntry = new JarEntry(agilePackage.getName() + "/" + exceptionPackage.getName() + "/" + tobeJared[i].getName());
				fileEntry.setTime(tobeJared[i].lastModified());
				out.putNextEntry(fileEntry);
			
				// Write file to archive
			    FileInputStream in = new FileInputStream(tobeJared[i]);
			    while (true) {
			    	int nRead = in.read(buffer, 0, buffer.length);
			    	if (nRead <= 0)
			    		break;
			    	out.write(buffer, 0, nRead);
			    }
		    	in.close();
	    	}
	    	
	    	out.close();
	    	stream.close();
	    	System.out.println("Adding completed OK");
		} catch (Exception ex) {
			ex.printStackTrace();
			System.out.println("Error: " + ex.getMessage());
		}
	}
}
