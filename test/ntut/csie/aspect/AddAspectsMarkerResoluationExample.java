package ntut.csie.aspect;

import java.io.IOException;

public class AddAspectsMarkerResoluationExample {

	// this is a a.b() case for aspect dummy handler
	public void aspectCaseForOneMethodInvocationWhichWillNotThrowIOException() {
		try {
			TestObjectAForAddAspectsMarkerResoluation a = new TestObjectAForAddAspectsMarkerResoluation();
			a.doSomethingWillNotThrowIOException();
			throw new IOException();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// this is a a.b() case for aspect dummy handler
	public void aspectCaseForOneMethodInvocationWhichWillThrowIOException() {
		try {
			TestObjectAForAddAspectsMarkerResoluation a = new TestObjectAForAddAspectsMarkerResoluation();
			a.doSomethingWillThrowIOException();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// this is a a.b().c() case for aspect dummy handler
	public void aspectCaseForTwoMethodInvocationAndLastInvocationWillThrowIOException() {
		try {
			TestObjectAForAddAspectsMarkerResoluation a = new TestObjectAForAddAspectsMarkerResoluation();
			a.getTestObjectB().doSomethingWillThrowIOException();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// this is a a.b().c() case for aspect dummy handler
	public void aspectCaseForTwoMethodInvocationAndLastInvocationWillNotThrowIOException() {
		try {
			TestObjectAForAddAspectsMarkerResoluation a = new TestObjectAForAddAspectsMarkerResoluation();
			a.getTestObjectB().doSomethingWillNotThrowException();
			throw new IOException();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// this is a a.b().c() case for aspect dummy handler
	public void aspectCaseForTwoMethodInvocationAndTheFirstInvocationWillThrowIOException() {
		try {
			TestObjectAForAddAspectsMarkerResoluation a = new TestObjectAForAddAspectsMarkerResoluation();
			a.getTestObjectBWillThrowIOException()
					.doSomethingWillNotThrowException();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// this is a a.b().c() case for aspect dummy handler
	public void aspectCaseForTwoMethodInvocationAndTheTwoInvocationWillThrowIOException() {
		try {
			TestObjectAForAddAspectsMarkerResoluation a = new TestObjectAForAddAspectsMarkerResoluation();
			a.getTestObjectBWillThrowIOException().doSomethingWillThrowIOException();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void testDataForGetAllTryStatementOfMethodDeclaration() {
		try {
			throw new IOException();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			throw new IOException();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			throw new IOException();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void testDataForgetMethodReturnType() {
		try {
			TestObjectAForAddAspectsMarkerResoluation a = new TestObjectAForAddAspectsMarkerResoluation();
			a.getTestObjectBWillThrowIOException();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
