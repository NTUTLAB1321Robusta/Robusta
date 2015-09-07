package ntut.csie.analyzer.unprotected;

public class UnprotectedMainProgramWithTryAtMiddleStatement {
	public static void main(String[] args) {
		UnprotectedMainProgramWithCatchThrowableExample test = new UnprotectedMainProgramWithCatchThrowableExample();
		try {
			int i = 0;
			i = i++;
		} catch (RuntimeException ex) {
		}
		test.toString();
	}
}
