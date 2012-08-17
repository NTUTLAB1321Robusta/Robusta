package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

public class UnprotectedMainProgramWithoutCatchExceptionExample {
	public static void main(String[] args) {
		try {
			UnprotectedMainProgramWithoutCatchExceptionExample test = new UnprotectedMainProgramWithoutCatchExceptionExample();
			test.toString();
		} catch (RuntimeException ex) {
		}
	}
}
