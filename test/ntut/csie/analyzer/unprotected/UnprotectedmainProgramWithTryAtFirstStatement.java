package ntut.csie.analyzer.unprotected;

public class UnprotectedmainProgramWithTryAtFirstStatement {
	public static void main(String[] args) {
		try {
			int i = 0;
			i = i++;
		} catch (Exception ex) {
		}
		UnprotectedMainProgramWithCatchRuntimeExceptionExample test = new UnprotectedMainProgramWithCatchRuntimeExceptionExample();
		test.toString();
	}
}
