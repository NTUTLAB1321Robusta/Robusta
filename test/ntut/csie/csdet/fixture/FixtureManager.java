package ntut.csie.csdet.fixture;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IPackageFragment;

public class FixtureManager {
	TestProject testProject;
	
	public void createProject(){
		createProject("Project-1");
	}
	
	public void createProject(String name){		
		testProject = new TestProject(name);	
		//Step 1. add junit.jar for test case
//		try {
//			testProject.addJar("org.junit", "junit.jar");
//		} catch (MalformedURLException e) {
//			
//			e.printStackTrace();
//		} catch (JavaModelException e) {
//			
//			e.printStackTrace();
//		} catch (IOException e) {
//			
//			e.printStackTrace();
//		}	
		
		//create project corresponging to project name
		if (name.equals("MM")) 
			createMM(testProject);

	}	
	
	public void dispose() throws CoreException{
		testProject.dispose();
	}
	
	public TestProject getProject(){
		return testProject;
	}
	
	/**
	 * Transform source code into fixture.
	 * 
	 * Note: Edit source code file for generating xxxFixture.java.
	 *       All changes need to be performed in xxxFixture.java should be edited there.
	 *
	 * Edit Step:
	 * 1. edit source code file
	 * 2. run this function to generate xxxFixture.java
	 * 3. run the test code for xxxFixture.java to check it is ok 
	 */
	public static void transform(String srcFileLocation,String desFileLocation
			,String desName, String packName){
		FileReader fr;
		BufferedReader br;
		FileWriter fw;
		BufferedWriter bw;
		try {
			fr = new FileReader(new File(srcFileLocation));
			br = new BufferedReader(fr);
			fw = new FileWriter(new File(desFileLocation));
			bw = new BufferedWriter(fw);
			
			bw.append("package "+packName+";\n\n");
			bw.append("/**\n*\n* Note: Don't edit this file.\n");
			bw.append("* See FixtureManager.java for detail information.\n*\n**/\n\n");
			bw.append("public class "+ desName +" {\n\n");
			bw.append("public "+ desName +"(){}\n\n");
			bw.append("public String getSource(){\n");
			bw.append("StringBuffer buffer = new StringBuffer();\n");
			
			while (br.ready()){
				String temp=br.readLine();
				if (temp.contains("package ntut.csie.micky.tcs.junit;"))
					continue;
				if (temp.startsWith("/**") || temp.startsWith(" *") 
						|| temp.startsWith(" */") || temp.startsWith("/*"))
					continue;						
				if (temp.contains("\""))					
					temp=temp.replaceAll("\"", "\\\\\"");				
				bw.append("buffer.append(\""+temp+"\\n\");\n");			
			}		

			bw.append("return buffer.toString();\n}\n}\n\n");
			bw.flush();
			bw.close();
			System.out.println("Finish Transform!!");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//create MM project
	private void createMM(TestProject testProject){			
		IPackageFragment tempPackage;		
		
		try {
			//Step 2. create a package
			tempPackage = testProject.createPackage("a.b.c");
			//Step 3. create a Type in the package
			// ignore exception
			testProject.createType(tempPackage, "MM.java", new MMFixture().getSource());
			// dummy handler
			testProject.createType(tempPackage, "DHFixture.java", new DHFixture().getSource());
			testProject.createType(tempPackage, "MainFixture.java", new MainFixture().getSource());
//			testProject.createType(tempPackage, "MMTest.java", new MMTestFixture().getSource());
//			testProject.createType(tempPackage, "MMTest1.java", new MMTest1Fixture().getSource());
//			testProject.createType(tempPackage, "MMTest2.java", new MMTest2Fixture().getSource());
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
}
