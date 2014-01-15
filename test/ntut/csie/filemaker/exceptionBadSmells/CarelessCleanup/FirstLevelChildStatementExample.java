package ntut.csie.filemaker.exceptionBadSmells.CarelessCleanup;

public class FirstLevelChildStatementExample {

	public void methodWithTwoStatements() {
		System.out.println("Hi");
		;
	}
}
