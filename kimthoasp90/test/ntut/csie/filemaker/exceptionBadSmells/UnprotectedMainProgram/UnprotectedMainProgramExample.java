package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

public class UnprotectedMainProgramExample {
	@Robustness(value = { @RTag(level = 1, exception = java.lang.Exception.class) })
	public static void main(String[] args) {
		try {
			UnprotectedMainProgramExample test = new UnprotectedMainProgramExample();
			test.toString();
		} catch (Exception ex) {
			//TODO: handle exception
		}
	}

}
