package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

public class UnprotectedMainProgramWithTryAtLastStatement {
	public static void main(String[] args) {
		UnprotectedMainProgramWithoutCatchExceptionExample test = new UnprotectedMainProgramWithoutCatchExceptionExample();
		test.toString();
		try {
			int i = 0;
			i = i++;
		} catch (RuntimeException ex) {
		}
	}
}
