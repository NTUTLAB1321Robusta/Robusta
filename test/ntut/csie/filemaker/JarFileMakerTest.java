package ntut.csie.filemaker;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.file.Files;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class JarFileMakerTest {

	private static void deleteFolder(File folder) {
	    File[] files = folder.listFiles();
	    if(files!=null) { //some JVMs return null for empty dirs
	        for(File f: files) {
	            if(f.isDirectory()) {
	                deleteFolder(f);
	            } else {
	                f.delete();
	            }
	        }
	    }
	    folder.delete();
	}
	
	private static final String BIN_FOLDER = "./TestFolder";
	private static final String TARGET_FOLDER = "./TestFolder/TargetFolder";
	@BeforeClass
	public static void setUp() throws Exception {
		File file = new File(BIN_FOLDER);
		deleteFolder(file);
		String path1 =  TARGET_FOLDER + "./file1.txt";
		String path2 =  TARGET_FOLDER + "./file2.txt";
		file = new File(path1);
		File parent = file.getParentFile();
		if(!parent.exists()) assertTrue(parent.mkdirs());
		
		//Create empty text file for file1 level2
		OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		writer.close();
		Files.copy(file.toPath(), new File(BIN_FOLDER + "./text1.txt").toPath());
		
		//Create not empty file for file2 level2
		file = new File(path2);
		writer = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
		writer.write("This string make me not be empty");
		writer.close();
		Files.copy(file.toPath(), new File(BIN_FOLDER + "./text2.txt").toPath());
	}
	
	@AfterClass
	public static void tearDown() {
		deleteFolder(new File(BIN_FOLDER));
		deleteFolder(new File("./compress.jar"));
	}
	
	@Test
	public void testSplitNameByDot() throws Exception {
		JarFileMaker jarFileMaker = new JarFileMaker();
		
		Method splitNameByDotMethod = JarFileMaker.class.getDeclaredMethod("splitNameByDot", String.class);
		splitNameByDotMethod.setAccessible(true);
		
		Assert.assertArrayEquals(new String[]{"document", "txt"},(Object[])splitNameByDotMethod.invoke(jarFileMaker, "document.txt"));
		Assert.assertArrayEquals(new String[]{"document","", "txt"}, (Object[])splitNameByDotMethod.invoke(jarFileMaker, "document..txt"));
		Assert.assertArrayEquals(new String[]{"docu","ment","", "txt"}, (Object[])splitNameByDotMethod.invoke(jarFileMaker, "docu.ment..txt"));
		Assert.assertEquals( 1, ((Object[])splitNameByDotMethod.invoke(jarFileMaker, "justTheName")).length);
		Assert.assertEquals("justTheName", ((Object[])splitNameByDotMethod.invoke(jarFileMaker, "justTheName"))[0]);
		
	}

	@Test
	public void testGetWillBeJaredClasses() throws Exception {
		JarFileMaker jarFileMaker = new JarFileMaker();
		Method splitNameByDotMethod = JarFileMaker.class.getDeclaredMethod("getWillBeJaredClasses", File.class, String.class);
		splitNameByDotMethod.setAccessible(true);
		Assert.assertArrayEquals((new File(TARGET_FOLDER)).listFiles(), (File[])splitNameByDotMethod.invoke(jarFileMaker, new File(BIN_FOLDER), "TargetFolder.AnotherFolder"));
		Assert.assertEquals(3, ((File[])splitNameByDotMethod.invoke(jarFileMaker, new File(BIN_FOLDER), "NotExit.Folder")).length);
	}

	@Test
	public void testCreateJarFile() {
		JarFileMaker jarFileMaker = new JarFileMaker();
		File file = new File("./compress.jar");
		File binFolder = new File(BIN_FOLDER);
		jarFileMaker.createJarFile(file, binFolder, "test.TestFolder");
		Assert.assertTrue(file.exists());
		
	}
	@Test(timeout = 10)
	public void testCreateJarFileTimeOut() {
		JarFileMaker jarFileMaker = new JarFileMaker();
		File file = new File("./compress.jar");
		File binFolder = new File(BIN_FOLDER);
		jarFileMaker.createJarFile(file, binFolder, "test.TestFolder");
		Assert.assertTrue(file.exists());
	}
	
	@Test(expected = RuntimeException.class)
	public void testCreateJarFileNullParam() throws Exception {
		try {
			JarFileMaker jarFileMaker = new JarFileMaker();
			jarFileMaker.createJarFile(null, null, "test.TestFolder");
		} catch(Exception e) {
			throw e;
		}
	}
}
