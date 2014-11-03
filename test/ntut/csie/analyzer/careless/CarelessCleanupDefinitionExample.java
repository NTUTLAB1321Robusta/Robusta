package ntut.csie.analyzer.careless;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class contains test examples that would help define Careless Cleanup rule.
 * Tests that are necessary for defining Careless Cleanup but is already written
 * somewhere else would be kept and commented.
 * @author Peter
 */
public class CarelessCleanupDefinitionExample {
 
	FileInputStream fileInputStream = null;
	MethodInvocationBeforeClose methodBeforeClose = new MethodInvocationBeforeClose();
	
	// a class that does not implement closeable
	class ConcreteNonCloseable {
		public void close() {
			// do some clean up
		}
	}
	
	// resource that does not implement closeable should not be detected by the CC detector
	public void nonclosableResourceClosing() throws IOException{
		ConcreteNonCloseable resource = new ConcreteNonCloseable();
		methodBeforeClose.declaredCheckedException();
		
		try{
			// do something here
		} finally {
			resource.close(); 
		}
	}
	
	// a class that does implement closeable
	class ConcreteCloseable implements Closeable {
		public void close() {
			// do some clean up
		}
	}
	
	// resource that does implement closeable should be detected by the CC detector
	public void closableResourceClosing() throws IOException{
		ConcreteCloseable resource = new ConcreteCloseable();
		methodBeforeClose.declaredCheckedException();
		
		try{
			// do something here
		}finally {
			resource.close();
		}
	}
	
	// this defines one of the ranges we detect
	public void exceptionBetweenCloseAndResourceDeclaration(){
		FileInputStream fis = null;
		
		try{
			methodBeforeClose.declaredCheckedException();
		} catch (IOException e) {
			// handle the exception
		} finally {
			try {
				fis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// this defines one of the ranges we detect
	public void closeRightAfterMethodDeclaration(){
		
		try{
			methodBeforeClose.declaredCheckedException();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				fileInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	// this defines one of the ranges we detect
	public void statementThatIsDoNotAlwaysExecuted(boolean a) throws IOException{
		FileInputStream fis = null;
		
		if(a){
			fis = new FileInputStream("C:\\FileNotExist.txt");
		}
		
		try{
			// do something
		} finally {
			// Bug: nothing in between line 95 and 102 would raise exception
			fis.close(); 
		}
	}
	
	// this defines one of the ranges we detect
	public void statementThatIsDoNotAlwaysExecuted2(boolean a) throws IOException{
		FileInputStream fis = null;
		
		//if(a){
			fis = new FileInputStream("C:\\FileNotExist.txt");
		//}
		
		try{
			// do something
		} finally {
			// Bug: or not? it's not one of the exception of the rule: any statement would raise exception,
			// but it's the resource itself's assignment.
			fis.close(); 
		}
	}
	
	// this defines one of the ranges we detect
	public void statementThatIsDoNotAlwaysExecuted3(boolean a) throws IOException{
		FileInputStream fis = null;
		
		if(a){
			//fis = new FileInputStream("C:\\FileNotExist.txt");
		}
		
		try{
			// do something
		} finally {
			// Bug: the if statement is treated as would raise exception
			fis.close(); 
		}
	}
	
	// this defines exceptions of the rule: any statement is treated as would throw exception
	public void resourceDeclarationDoNotRaiseException() throws IOException{
		FileInputStream fis = null;
		FileOutputStream fos = null;
		
		try{
			// do something
		} finally {
			fis.close(); // should not be CC, bug!
		}
	}
	
	// this defines exceptions of the rule: any statement is treated as would throw exception
	public void resourceDeclarationDoNotRaiseException2() throws IOException{
		FileInputStream fis = null;
		String s;
		
		try{
			// do something
		} finally {
			fis.close(); // should not be CC, bug!
		}
	}
	
	// this defines exceptions of the rule: any statement is treated as would throw exception
	public void resourceDeclarationDoNotRaiseException3() throws IOException{
		FileInputStream fis = null;
		FileOutputStream fos;
		
		try {
			// do something
		} finally {
			fis.close(); // should not be CC, bug!
		}
	}
	
	// this defines exceptions of the rule: any statement is treated as would throw exception
	public void nullAssignmentDoNotRaiseException(boolean a) throws IOException{
		FileInputStream fis = null;
		fis = null; // cause
		
		try{
			
		} finally {
			// Bug: not only the assignment in between is a null assignment, is it also the resourece itself
			fis.close(); 
		}
	}
	
	public void usingIfForNonNullChecking() throws IOException{
		FileInputStream fis = null;
		
		try{
			// do something 
		}finally{
			if(fis != null)
				fis.close();
		}
	}
	
	public void catchAllException() throws Throwable{
		FileInputStream fis = null;
		
		try{
			methodBeforeClose.declaredCheckedException();
			methodBeforeClose.declaredUncheckedException();
		} catch(Exception e){
			// do some exception handling
		}
		
		// Bug: all exception in try would be caught, but Robusta detects it as a CC
		fis.close();
	}
	
	public void catchAllCheckedException() throws IOException{
		FileInputStream fis = null;
		
		try{
			methodBeforeClose.declaredCheckedException();
		} catch(IOException e) {
			// do some exception handling
		}
		
		// not a bug for that the statement in the try might somehow raise a runtime exception
		fis.close(); 
	}
	
	public void notCatchingUncheckedException() throws IOException{
		FileInputStream fis = null;
		
		try{
			methodBeforeClose.declaredCheckedException();
			methodBeforeClose.declaredUncheckedException();
		} catch(IOException e){
			// do some exception handling
		} 
		
		fis.close();
	}
	
}
