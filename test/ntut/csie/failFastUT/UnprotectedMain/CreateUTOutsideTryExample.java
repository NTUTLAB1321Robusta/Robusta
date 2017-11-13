package ntut.csie.failFastUT.UnprotectedMain;

import java.io.IOException;

public class CreateUTOutsideTryExample {
	public static void main(String[] args) throws IOException {
		try{
			throwIO1();
		} catch (IOException e) {
			e.printStackTrace();
			throwIO2();
		}finally {
			System.out.println();
			throwIO2();
		}
		throwIO3();
		print1();
	}

	private static void throwIO1() throws IOException {
		throw new IOException();
		
	}
	private static void throwIO2() throws IOException {
		throw new IOException();
		
	}
	private static void throwIO3() throws IOException {
		throw new IOException();
		
	}
	private static void print1() {
		System.out.println("print1");
		
	}
}
