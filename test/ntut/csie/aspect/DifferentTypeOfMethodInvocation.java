package ntut.csie.aspect;

import java.io.IOException;

public class DifferentTypeOfMethodInvocation {

	public void mehodInvocationWithExpression(){
		DifferentTypeOfMethodInvocation object = new DifferentTypeOfMethodInvocation();
		try{
			object.doSomethind();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void mehodInvocationWithoutExpression(){
		try{
			doSomethind();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void doSomethind() throws IOException{
		
	}
}
