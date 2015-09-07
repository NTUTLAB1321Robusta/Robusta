package ntut.csie.analyzer.unprotected;

public class UnprotectedMainProgramWithTryAtLastStatement {
	public static void main(String[] args) {
		UnprotectedMainProgramWithCatchThrowableExample test = new UnprotectedMainProgramWithCatchThrowableExample();
		test.toString();
		try {
			int i = 0;
			i = i++;
		} catch (RuntimeException ex) {
		}
	}
}
