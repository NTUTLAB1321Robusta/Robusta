package ntut.csie.failFastUT.UnprotectedMain;

import java.io.IOException;

public class CreateUTBetweenEHBlockExample {
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
		try{
			throwIO4();
		} catch (IOException e) {
			e.printStackTrace();
			throwIO2();
		}finally {
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
	private static void throwIO3() throws IOException {
		throw new IOException();
	}
	private static void throwIO4() throws IOException {
		throw new IOException();
	}
	private static void print1() {
		System.out.println("print1");
		
	}
}
