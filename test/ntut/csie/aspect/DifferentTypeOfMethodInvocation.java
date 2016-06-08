package ntut.csie.aspect;

import java.io.IOException;

public class DifferentTypeOfMethodInvocation {

	public void mehodInvocationWithExpression(){
		DifferentTypeOfMethodInvocation object = new DifferentTypeOfMethodInvocation();
		try{
			object.doSomething();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void mehodInvocationWithoutExpression(){
		try{
			doSomething();
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void doSomething() throws IOException{
		
	}
}
