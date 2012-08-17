package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

public class UnprotectedMainProgramWithTryAtMiddleStatement {
	public static void main(String[] args) {
		UnprotectedMainProgramWithoutCatchExceptionExample test = new UnprotectedMainProgramWithoutCatchExceptionExample();
		try {
			int i = 0;
			i = i++;
		} catch (RuntimeException ex) {
		}
		test.toString();
	}
}
