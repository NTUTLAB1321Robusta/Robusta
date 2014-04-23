package ntut.csie.analyzer.dummy.example;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.log4j.Logger;

public class PrintOrLogBySuperMethod {

	@SuppressWarnings("serial")
	public class ConcreteException extends Exception {
		
		public void printStackTraceBySuperMethod() {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream("");
				fis.read();
			} catch (IOException e) { // DummyHandler
				super.printStackTrace();
			}
		}
	}

	public class ConcretePrintStream extends PrintStream {
		
		public ConcretePrintStream(File file) throws FileNotFoundException {
			super(file);
		}

		public void sysErrPrintBySuperMethod() {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream("");
				fis.read();
			} catch (IOException e) { // DummyHandler
				super.print("context");
			}
		}
	}

	public class ConcreteLog4J extends Logger {
	
		protected ConcreteLog4J(String name) {
			super(name);
		}

		public void log4JBySuperMethod() {
			FileInputStream fis = null;
			try {
				fis = new FileInputStream("");
				fis.read();
			} catch (IOException e) { // DummyHandler
				super.info("message");
			}
		}
	}
}
