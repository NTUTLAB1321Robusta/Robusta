package ntut.csie.analyzer.unprotected;

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
