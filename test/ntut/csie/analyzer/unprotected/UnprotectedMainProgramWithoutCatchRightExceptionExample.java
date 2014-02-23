package ntut.csie.analyzer.unprotected;

import java.io.IOException;

public class UnprotectedMainProgramWithoutCatchRightExceptionExample {
	public static void main(String[] args) {
		try {
			throw new IOException();
		} catch (IOException ex) {
		}
	}
}
