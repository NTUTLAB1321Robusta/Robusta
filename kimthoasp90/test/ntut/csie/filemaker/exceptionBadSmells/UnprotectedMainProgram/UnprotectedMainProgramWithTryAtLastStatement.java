package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

public class UnprotectedMainProgramWithTryAtLastStatement {
	public static void main(String[] args) {
		UnprotectedMainProgramWithCatchRuntimeExceptionExample test = new UnprotectedMainProgramWithCatchRuntimeExceptionExample();
		test.toString();
		try {
			int i = 0;
			i = i++;
		} catch (RuntimeException ex) {
		}
	}
}
