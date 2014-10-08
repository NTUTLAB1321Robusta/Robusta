package ntut.csie.analyzer.careless;

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
		resource.close(); 
	}
	
	/* this defines one of the ranges we detect
	public void exceptionBetweenCloseAndResourceDeclaration() throws IOException{
		FileInputStream fis = null;
		methodBeforeClose.declaredCheckedException();
		fileInputStream.close();
	}
	*/
	
	/* this defines one of the ranges we detect
	public void closeRightAfterMethodDeclaration() throws IOException{
		fileInputStream.close();
	}
	*/
	
	// this also defines one of the range we detect
	public void resourceDeclarationDoNotRaiseException() throws IOException{
		FileInputStream fis = null;
		FileOutputStream fos = null;
		fis.close(); // should not be CC, bug!
	}
	
	
	public void resourceDeclarationDoNotRaiseException2() throws IOException{
		FileInputStream fis = null;
		String s = "I'm a string";
		fis.close(); // should not be CC, bug!
	}
	
	public void resourceDeclarationDoNotRaiseException3() throws IOException{
		FileInputStream fis = null;
		FileOutputStream fos;
		fis.close(); // should not be CC, bug!
	}
	
	// any statement that would always execute are treated as "would raise exception"
	public void statementThatIsDoNotAlwaysExecuted(boolean a) throws IOException{
		FileInputStream fis = null;
		
		if(a) // cause
			fis = null;
		
		if(fis != null)
			fis.close(); // should not be CC for human
	}
	
	// assignment statement as a statement that would throw an exception bug!
	public void assignmentStatementTreatedAsWouldRaiseException(boolean a) throws IOException{
		FileInputStream fis = null;
		
		fis = null; // cause
		
		fis.close(); // should not be CC for human
	}
	
	public void doWhileBetweenCloseAndResourceDeclaration() throws IOException{
		FileInputStream fis = null;
		
		do{
			fis.close(); // should not be CC, bug!
		}while(fis != null);
	}
	
	public void usingWhileForNonNullChecking() throws IOException{
		FileInputStream fis = null;
		
		while(fis != null){
			fis.close(); // should not be CC, bug!
		}
	}
	
	public void usingIfForNonNullChecking() throws IOException{
		FileInputStream fis = null;
		
		if(fis != null){
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
		} finally {
			fis.close();
		}
	}
	
	public void notCatchingUncheckedException() throws IOException{
		FileInputStream fis = null;
		
		try{
			methodBeforeClose.declaredCheckedException();
			methodBeforeClose.declaredUncheckedException();
		} catch(IOException e){
			// do some exception handling
		} finally {
			fis.close(); // not catching unchecked exception, maybe?
		}
	}
	
	public void notCatchingAllException() throws IOException{
		FileInputStream fis = null;
		
		try{
			methodBeforeClose.declaredCheckedException();
		} finally {
			fis.close(); // not catching checked exception but not caught, bug!
		}
	}
	
}
