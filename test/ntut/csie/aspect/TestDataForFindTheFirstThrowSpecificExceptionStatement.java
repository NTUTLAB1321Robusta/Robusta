package ntut.csie.aspect;

import java.io.FileOutputStream;
import java.io.IOException;

public class TestDataForFindTheFirstThrowSpecificExceptionStatement {
	public void doSomething(){
		try{
			FileOutputStream output = new FileOutputStream
					("C:\\Users\\steven\\new_eclipse_workspace\\BadSmellProject\\output.txt");
			output.flush();
			output.close();
		}catch( IOException e){
			System.out.println("exception happen");
			e.printStackTrace();
		}
	} 
}
