package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

public class UnprotectedMainProgramWithCatchRuntimeExceptionExample {
	public static void main(String[] args) {
		try {
			UnprotectedMainProgramWithCatchRuntimeExceptionExample test = new UnprotectedMainProgramWithCatchRuntimeExceptionExample();
			test.toString();
		} catch (RuntimeException ex) {
		}
	}
}
