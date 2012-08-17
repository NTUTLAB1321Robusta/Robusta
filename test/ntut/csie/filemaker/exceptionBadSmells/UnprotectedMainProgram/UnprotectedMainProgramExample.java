package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

import agile.exception.Robustness;
import agile.exception.RL;

public class UnprotectedMainProgramExample {
	@Robustness(value = { @RL(level = 1, exception = java.lang.Exception.class) })
	public static void main(String[] args) {
		try {
			UnprotectedMainProgramExample test = new UnprotectedMainProgramExample();
			test.toString();
		} catch (Exception ex) {
			//TODO: handle exception
		}
	}

}
