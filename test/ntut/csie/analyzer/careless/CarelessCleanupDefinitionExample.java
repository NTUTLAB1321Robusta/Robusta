package ntut.csie.analyzer.careless;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class contains test examples that would help define Careless Cleanup
 * rules. Test samples that are necessary for defining Careless Cleanup but is
 * already written somewhere else would be kept and commented.
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

	/*
	 * Since Java 7 Oracle introduced AutoCloseable interface and developers
	 * stared using it instead of Closeable; hence, we need to be able to detect
	 * Careless Cleanup for resources of AutoCloseable type.
	 * 
	 * commented because this example is also written somewhere else but
	 * necessary to be here to help define our rules
	 * 
	 * class ConcreteAutoCloseable implements AutoCloseable {
	 * 
	 * @Override public void close() throws IOException { // do some clean up
	 * here } public void write() throws IOException { // do some writing here }
	 * }
	 * 
	 * public void closeMethodInvocationOfConcreteAutoCloseable() throws
	 * Exception { ConcreteAutoCloseable concreteAutoCloseableObject = new
	 * ConcreteAutoCloseable(); concreteAutoCloseableObject.write();
	 * concreteAutoCloseableObject.close(); //unsafe }
	 */

	class ConcreteAutoCloseableOutputStream implements AutoCloseable {
		@Override
		public void close() throws IOException {
			// do some clean up here
		}
	}

	public void cleanUp(FileInputStream fileInputStream) {
		close();
	}

	public void cleanUp(AutoCloseable resource) {
		close();
	}

	public void close() {
		// some clean up code
	}
	
	class ClosableResourceContainClassVariable implements Closeable {
		public boolean a = true;
		public void close() {
			// do something
		}
	}

	// resource that does not implement closeable should not be detected by the
	// CC detector
	public void noncloseableResourceClosing() throws IOException {
		ConcreteNonCloseable nonCloseableResource = new ConcreteNonCloseable();
		methodBeforeClose.declaredCheckedExceptionOnMethodSignature();

		try {
			// do something here
		} finally {
			nonCloseableResource.close(); // safe
		}
	}

	// resource that does implement closeable should be detected by the CC
	// detector
	public void closeableResourceClosing() throws IOException {
		ConcreteCloseable closeableResource = new ConcreteCloseable();
		methodBeforeClose.declaredCheckedExceptionOnMethodSignature();

		try {
			// do something here
		} finally {
			closeableResource.close(); // unsafe
		}
	}

	// resource that does implement closeable but its clean up method is not
	// named "close"
	// would not be detected by the CC detector
	public void closeableResourceClosingMethodNotNamedClose()
			throws IOException {
		ConcreteCloseable resource = new ConcreteCloseable();
		methodBeforeClose.declaredCheckedExceptionOnMethodSignature();

		try {
			// do something here
		} finally {
			resource.cleanUp(); // safe
		}
	}

	// clean up method that is not named "close" would not be detected by the CC
	// detector
	// UNLESS it has a "Closeable" parameter and the method contains a close in
	// it body.
	public void closeMethodNotNamedClosePassedInCloseableResource()
			throws IOException {
		methodBeforeClose.declaredCheckedExceptionOnMethodSignature();

		try {
			// do something here
		} finally {
			cleanUp(fileInputStream); // unsafe if
										// "also detect this bad smell out of try statement"
										// is checked
		}
	}

	// clean up method that is not named "close" would not be detected by the CC
	// detector
	// UNLESS it has a "AutoCloseable" parameter and the method contains a close
	// in it body.
	public void closeMethodNotNamedClosePassedInAutoCloseableResource()
			throws IOException {
		ConcreteAutoCloseableOutputStream os = new ConcreteAutoCloseableOutputStream();
		methodBeforeClose.declaredCheckedExceptionOnMethodSignature();

		try {
			// do something here
		} finally {
			cleanUp(os); // unsafe if
							// "also detect this bad smell out of try statement"
							// is checked
		}
	}

	/*
	 * commented because these kind of examples are also written somewhere else
	 * but necessary to be here to help define our rules
	 * 
	 * // this defines one of the ranges we detect: from close statement back to
	 * resource's assignment public void
	 * exceptionBeforeCloseAndResourceDeclaration() throws IOException{
	 * methodBeforeClose.declaredCheckedException(); FileInputStream fis = new
	 * FileInputStream("C:\\FileNotExist.txt"); fis.close(); //safe }
	 * 
	 * // this defines one of the ranges we detect: from close statement back to
	 * resource's assignment public void
	 * exceptionBetweenCloseAndResourceDeclaration() throws IOException{
	 * FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");
	 * methodBeforeClose.declaredCheckedException(); fis.close(); //unsafe }
	 * 
	 * // this defines one of the ranges we detect: from close statement back to
	 * method declaration // if resource's assignment cannot be found public
	 * void exceptionBeforeCloseButNoResourceDeclaration() throws IOException{
	 * methodBeforeClose.declaredCheckedException(); fileInputStream.close();
	 * //unsafe }
	 */

	/*
	 * Story#7631
	 */
	// this defines one of the ranges we detect: from close statement back to
	// its resource's last assignment
	public void exceptionBeforeLastResourceAssignment(boolean a)
			throws IOException {
		FileInputStream fis = null;
		methodBeforeClose.declaredCheckedExceptionOnMethodSignature();

		fis = new FileInputStream("C:\\FileNotExist.txt");

		try {
			// do something
		} finally {
			fis.close(); // safe
		}
	}

	/*
	 * Story#7634
	 */
	// this defines one of the ranges we detect: from close statement back to
	// its
	// resource's declaration if its resource's assignment may not always be
	// executed
	public void exceptionBeforeLastResourceAssignmentThatMayNotBeExecuted(
			boolean a) throws IOException {
		FileInputStream fis = null;
		fis.read();

		if (a) {
			fis = new FileInputStream("C:\\FileNotExist.txt");
		}

		try {
			// do something
		} finally {
			fis.close(); // unsafe because last assignment is placed in a if
							// block
		}
	}

	/*
	 * Any statement in between a resource's close and last assignment would be
	 * treated as would or has possibility to raise an exception EXCEPT: 
	 * 1. a IF statement that contains only one or more boolean variables 
	 * 2. a IF statement enclosing a resource's close statement that is checking if the resource is NULL 
	 * 3. a TRY statement which has catch block(s) catching all CHECKED exception that may be thrown from it
	 *  or a TRY statement which has a blanket catch clause  
	 * 4. a variable declaration(without instance assignment or null assignment) 
	 * 5. a variable declaration of primitive types (with/without instance assignment)
	 */
	public void aStatementInBetweenDetectionRange(boolean a) throws IOException {
		someRandomClassThatWouldDoRandomThings randomObject = new someRandomClassThatWouldDoRandomThings();
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");

		randomObject.doSomething();

		fis.close(); // safe
	}

	public void ifStatementCheckingBooleanVariableInBetweenDetectionRange(
			boolean a) throws IOException {
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");

		if (a) {
		}

		fis.close(); // safe
	}

	public void ifStatementCheckingResourceIsNotNullContainClose(boolean a)
			throws IOException {
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");

		try {
			// do something
		} finally {
			if (fis != null) {
				fis.close(); // safe
			}
		}
	}

	public void ifStatementCheckingResourceIsNotNullBeforeClose(boolean a)
			throws IOException {
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");

		try {
			// do something
		} finally {
			if (fis != null) {
			}
			fis.close(); // safe
		}
	}

	public void ifStatementCheckingResourceIsSameContainClose(boolean a)
			throws IOException {
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");
		int one = 1;
		int two = 2;
		try {
			// do something
		} finally {
			if (one != two) {
				fis.close(); // safe
			}
		}
	}

	public void ifStatementCheckingResourceIsSameBeforeClose(boolean a)
			throws IOException {
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");
		int one = 1;
		int two = 2;
		try {
			// do something
		} finally {
			if (one != two) {
			}
			fis.close(); // safe
		}
	}

	public void tryStatementCatchingAllCheckedExceptionInBetweenDetectionRange(
			boolean a) throws IOException {
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");

		try {
			methodBeforeClose.declaredCheckedExceptionOnMethodSignature();
		} catch (IOException e) {
			// handle IOException e
		}

		fis.close(); // safe
	}
	
	public void tryBlockCatchingGenericExceptionInBetweenDetectionRange(
			boolean a) throws IOException {
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");

		try {
			methodBeforeClose.declaredCheckedException();
		} catch (Exception e) {
			// handle Exception e
		}

		fis.close(); // safe
	}

	public void objectDeclarationWithoutAssignmentInBetweenDetectionRange(
			boolean a) throws IOException {
		FileInputStream fis = new FileInputStream("C:\\FileNotExist.txt");

		FileOutputStream fos = null;
		someRandomClassThatWouldDoRandomThings randomObject;

		fis.close(); // safe
	}

	public void primitiveVariableDeclarationWithAssignmentInBetweenDetectionRange(
			boolean a) throws IOException {
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

		fis.close(); // safe
	}

	public void resourceClosingInIfStatementCheckingQualifiedName() throws IOException {
		ClosableResourceContainClassVariable qualifier = new ClosableResourceContainClassVariable();
		if (false != qualifier.a) {
			qualifier.close(); // safe
		}
	}
	
	public void resourceCloseAfterExpressionStatement() throws Exception {
		boolean a;
		boolean c;
		boolean d;
		int b = 1;
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		a = c = d = true;
		++b;
		b++;
		fis.close();// safe
	}
	
	public void resourceCloseInTheSynchronizedStatement() throws Exception {
		Integer a = new Integer(1);
		FileInputStream fis = new FileInputStream(new File("C:\\123"));
		synchronized (a) {
			fis.close();// safe
        }
	}
}
