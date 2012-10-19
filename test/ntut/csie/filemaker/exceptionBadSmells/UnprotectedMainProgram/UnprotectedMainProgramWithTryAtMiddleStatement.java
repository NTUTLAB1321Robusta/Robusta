package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

public class UnprotectedMainProgramWithTryAtMiddleStatement {
	public static void main(String[] args) {
		UnprotectedMainProgramWithCatchRuntimeExceptionExample test = new UnprotectedMainProgramWithCatchRuntimeExceptionExample();
		try {
			int i = 0;
			i = i++;
		} catch (RuntimeException ex) {
		}
		test.toString();
	}
}
