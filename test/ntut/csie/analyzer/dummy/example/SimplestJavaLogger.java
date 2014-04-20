package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;

public class SimplestJavaLogger {

	java.util.logging.Logger javaLogger = null;

	public void javaLogWithMethodInfo() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			javaLogger.info("");
		}
	}

	public void javaLogWithMethodLog() {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) { // DummyHandler
			javaLogger.log(Level.INFO, "Just log it.");
		}
	}
}
