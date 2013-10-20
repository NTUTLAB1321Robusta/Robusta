package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

public class UnprotectedMainProgramWithoutTryExample {
	public static void main(String[] args) {
		UnprotectedMainProgramWithoutTryExample test = new UnprotectedMainProgramWithoutTryExample();
		test.toString();
	}
}
