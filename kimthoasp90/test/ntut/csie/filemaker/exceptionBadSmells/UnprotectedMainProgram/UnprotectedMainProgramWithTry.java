package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

public class UnprotectedMainProgramWithTry {
	public static void main(String[] args) {
		int i = 0, j = 0, k = 0;
		try {
			i = i++;
		} catch (RuntimeException ex) {
		}
		UnprotectedMainProgramWithCatchRuntimeExceptionExample test = new UnprotectedMainProgramWithCatchRuntimeExceptionExample();
		try {
			j = j++;
		} catch (RuntimeException ex) {
		}
		test.toString();
		try {
			k = k++;
		} catch (RuntimeException ex) {
		}
	}
}
