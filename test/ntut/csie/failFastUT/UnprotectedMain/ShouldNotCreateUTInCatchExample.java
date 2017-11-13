package ntut.csie.failFastUT.UnprotectedMain;

import java.io.IOException;

public class ShouldNotCreateUTInCatchExample {
	public static void main(String[] args) throws IOException {
		try{
			throwIO1();
		}catch (IOException e) {
			System.out.println();
			throwIO2();
		}
	}

	private static void throwIO1() throws IOException {
		throw new IOException();
		
	}
	private static void throwIO2() throws IOException {
		throw new IOException();
		
	}

}
