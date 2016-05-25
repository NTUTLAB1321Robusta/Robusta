package ntut.csie.analyzer.careless.closingmethod;

import java.io.Closeable;
import java.io.IOException;

public class ClassImplementCloseable implements Closeable {

	@Override
	public void close() throws IOException {
		throw new IOException();
	}

	public void doSomethingWithoutThrowsExceptionOnSignature() {

	}

	public void doSomethingWhichThrowsExceptionOnSignature() throws IOException {
		throw new IOException();
	}
}
