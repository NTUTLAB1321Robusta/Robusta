package ntut.csie.filemaker.exceptionBadSmells.UnprotectedMainProgram;

import java.io.IOException;

public class UnprotectedMainProgramWithoutCatchRightExceptionExample {
	public static void main(String[] args) {
		try {
			throw new IOException();
		} catch (IOException ex) {
		}
	}
}
