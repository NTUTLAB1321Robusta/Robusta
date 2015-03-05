package ntut.csie.analyzer.careless;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class contains test examples that would help define Careless Cleanup rules.
 * Test samples that are necessary for defining Careless Cleanup but is already written
 * somewhere else would be kept and commented.
 * @author Peter
 */
public class CarelessCleanupDefinitionExample {
 
	FileInputStream fileInputStream = null;
	MethodInvocationBeforeClose methodBeforeClose = new MethodInvocationBeforeClose();
	
	class someRandomClassThatWouldDoRandomThings {
		public void doSomething() {
			// do something
		}
	}
	// a sample class that does not implement closeable
	class ConcreteNonCloseable {
		public void close() {
			// do some clean up
		}
	}
	
	// a sample class that does implement closeable
	class ConcreteCloseable implements Closeable {
		public void close() {
			// do some clean up
		}
		public void cleanUp() {
			// do some clean up
		}
	}
	
	public void cleanUp(FileInputStream fileInputStream) {
		close();
	}
	
	public void close() {
		// do nothing
	}
	
	// resource that does not implement closeable should not be detected by the CC detector
	public void noncloseableResourceClosing() throws IOException{
		ConcreteNonCloseable nonCloseableResource = new ConcreteNonCloseable();
		methodBeforeClose.declaredCheckedException();
		
		try{
			// do something here
		} finally {
			nonCloseableResource.close(); //safe
		}
	}
	
	// resource that does implement closeable should be detected by the CC detector
	public void closeableResourceClosing() throws IOException{
		ConcreteCloseable closeableResource = new ConcreteCloseable();
		methodBeforeClose.declaredCheckedException();
		
		try{
			// do something here
		}finally {
			closeableResource.close(); //unsafe
		}
	}
	
	// resource that does implement closeable but its clean up method is not named "close"
	// would not be detected by the CC detector
	public void closeableResourceClosingMethodNotNamedClose() throws IOException{
		ConcreteCloseable resource = new ConcreteCloseable();
		methodBeforeClose.declaredCheckedException();
		
		try{
			// do something here
		}finally {
			resource.cleanUp(); //safe
		}
	}
	
	// clean up method that is not named "close" would not be detected by the CC detector 
	// UNLESS it has a closeable parameter and the method contains a close in it body.
	public void closeMethodNotNamedClose() throws IOException{
		methodBeforeClose.declaredCheckedException();
		
		try{
			// do something here
		}finally {
			cleanUp(fileInputStream); //unsafe if "also detect this bad smell out of try statement" is checked
		}
	}
	
	/*
	 * commented because these kind of examples are also written somewhere else
	 * but necessary to be here to help define our rules
	 *  
	// this defines one of the ranges we detect: from close statement back to resource's assignment
	public void exceptionBeforeCloseAndResourceDeclaration() throws IOException{
		methodBeforeClose.declaredCheckedException();
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");
		fis.close(); //safe
	}
	
	// this defines one of the ranges we detect: from close statement back to resource's assignment
	public void exceptionBetweenCloseAndResourceDeclaration() throws IOException{
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");
		methodBeforeClose.declaredCheckedException();
		fis.close(); //unsafe
	}
	
	// this defines one of the ranges we detect: from close statement back to method declaration 
	// if resource's assignment cannot be found
	public void exceptionBeforeCloseButNoResourceDeclaration() throws IOException{
		methodBeforeClose.declaredCheckedException();
		fileInputStream.close(); //unsafe
	}
	 */
	
	/* 
	 * Story#7631
	 */
	// this defines one of the ranges we detect: from close statement back to its resource's last assignment
	public void exceptionBeforeLastResourceAssignment(boolean a) throws IOException{
		FileInputStream fis = null;
		methodBeforeClose.declaredCheckedException();
		
		fis = new FileInputStream("C:\\FileNotExist.txt");
		
		try{
			// do something
		} finally {
			fis.close(); //safe
		}
	}
	
	/* 
	 * Story#7634
	 */
	// this defines one of the ranges we detect: from close statement back to its 
	// resource's declaration if its resource's assignment may not always be executed
	public void exceptionBeforeLastResourceAssignmentThatMayNotBeExecuted(boolean a) throws IOException{
		FileInputStream fis = null;
		methodBeforeClose.declaredCheckedException();
		
		if(a){
			fis = new FileInputStream("C:\\FileNotExist.txt");
		}
		
		try{
			// do something
		} finally {
			fis.close(); //unsafe because last assignment is placed in a if block
		}
	}
	
	/* Any statement in between a resource's close and last assignment would be treated
	 * as would or has possibility to raise an exception
	 * EXCEPT:
	 * 1. a IF statement that contains only one or more boolean variables
	 * 2. a IF statement enclosing a resource's close statement that is checking if the resource is NULL
	 * 3. a TRY block which has catch block(s) catching all possible exception that may be thrown from it
	 * 4. a variable declaration(without instance assignment or null assignment)
	 * 5. a variable declaration of primitive types (with/without instance assignment)
	 */
	public void aStatementInBetweenDetectionRange(boolean a) throws IOException{
		someRandomClassThatWouldDoRandomThings randomObject = new someRandomClassThatWouldDoRandomThings();
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");
		
		randomObject.doSomething();
		
		fis.close(); //unsafe
	}
	
	public void ifStatementCheckingBooleanVariableInBetweenDetectionRange(boolean a) throws IOException{
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");
		
		if(a){
		}
		
		fis.close(); //safe
	}
	
	public void ifStatementCheckingResourceIsNotNullBeforeCloseAndInBetweenDetectionRange(boolean a) throws IOException{
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");
		
		try{
			// do something
		} finally {
			if(fis != null){
				fis.close(); //safe
			}
		}
	}
	
	/* 
	 * not yet implemented;
	 */
	public void tryBlockCatchingAllPossibleExceptionInBetweenDetectionRange(boolean a) throws IOException{
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");
		
		try {
			methodBeforeClose.declaredCheckedException();
		} catch (IOException e) {
			// handle IOException e
		} catch (Exception e) {
			// handle Exception e
		}
		
		fis.close(); //safe if bug fixed 
	}
	
	public void objectDeclarationWithoutAssignmentInBetweenDetectionRange(boolean a) throws IOException{
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");
		
		FileOutputStream fos = null;
		someRandomClassThatWouldDoRandomThings randomObject;
		
		fis.close(); //safe
	}
	
	public void primitiveVariableDeclarationWithAssignmentInBetweenDetectionRange(boolean a) throws IOException{
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");
		
		byte byteType = 0;
		short shortType = 1;
		int integerType = 2;
		long longType = 3;
		float floatType = 4;
		double doubleType = 5;
		char charType = '\u0001';		
		boolean booleanType = false;
		String stringType = "Im what Im.";
		
		fis.close(); //safe
	}
}
