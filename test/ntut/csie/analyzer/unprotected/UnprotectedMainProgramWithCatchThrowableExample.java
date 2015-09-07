package ntut.csie.analyzer.unprotected;

public class UnprotectedMainProgramWithCatchThrowableExample {
	public static void main(String[] args) {
		try {
			UnprotectedMainProgramWithCatchThrowableExample test = new UnprotectedMainProgramWithCatchThrowableExample();
			test.toString();
		} catch (Throwable e) {
		}
	}
}
