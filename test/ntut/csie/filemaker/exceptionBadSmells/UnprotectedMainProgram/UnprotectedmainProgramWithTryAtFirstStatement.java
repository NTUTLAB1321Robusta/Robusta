package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

public class UnprotectedmainProgramWithTryAtFirstStatement {
	public static void main(String[] args) {
		try {
			int i = 0;
			i = i++;
		} catch (Exception ex) {
		}
		UnprotectedMainProgramWithoutCatchExceptionExample test = new UnprotectedMainProgramWithoutCatchExceptionExample();
		test.toString();
	}
}
