package ntut.csie.filemaker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import ntut.csie.robusta.util.PathUtils;

public class JarFileMaker {
	public int BUFFER_SIZE = 10240;
	/**
	 * @param archiveFile 指定產生Jar檔的路徑 + 檔名
	 * @param binFolder 傳入bin的listFile
	 * @param fullPackageName
	 */
	public void createJarFile(File archiveFile, File binFolder, String fullPackageName) {
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
	    	
	    	File[] tobeJared = getWillBeJaredClasses(binFolder, fullPackageName);
	    	
	    	for (int i = 0; i < tobeJared.length; i++) {
	    		if (tobeJared[i] == null || !tobeJared[i].exists() || tobeJared[i].isDirectory())
	    			continue; // Just in case...
			
				// Add archive entry
				JarEntry fileEntry = new JarEntry(PathUtils.dot2slash(fullPackageName) 
						+ "/" + tobeJared[i].getName());
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
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 根據.分隔package name
	 * @param name
	 * @return
	 */
	private String[] splitNameByDot(String name) {
		String[] result = new String[1];
		if(name.indexOf(".") != -1) {
			return name.split("\\.");
		}
		result[0] = name;
		return result;
	}
	
	/**
	 * 蒐集要包Jar檔的class
	 * @param binFolder
	 * @param fullPackageName
	 * @return
	 */
	private File[] getWillBeJaredClasses(File binFolder, String fullPackageName) {
		String[] splitedPackageName = splitNameByDot(fullPackageName);
		File willBeListedDir = binFolder;
		for(int i = 0; i<splitedPackageName.length; i++) {
			for(File currentFolder: willBeListedDir.listFiles()) {
				if(currentFolder.getName().equals(splitedPackageName[i])) {
					willBeListedDir = currentFolder;
					break;
				}
			}
		}
		return willBeListedDir.listFiles();
	}
}
