package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;

public class UserDefinedClass {

	/**
	 * 在catch內使用outer class來測試使用者自訂的pattern
	 */
	public void method() {
		UserDefinedClassDeclaration classPattern = new UserDefinedClassDeclaration();
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			// 使用者自訂type1 - [javaFilePath].ClassPattern.*時 - true
			classPattern.eat();
			/*
			 * 使用者自訂type1 - [javaFilePath].UserDefinedClassDeclaration.*時 - true
			 * swim()有去呼叫System.out.println，但不會被「*.toString」偵測到
			 */
			classPattern.swim();
		} catch (Exception e) {
			/*
			 * 使用者自訂type1 - [javaFilePath].UserDefinedClassDeclaration.*時 - false
			 * 雖然使用UserDefinedClassDeclaration，但此method是繼承Object來的，故不被記入
			 * 若讓UserDefinedClassDeclaration override此method，就會被記入
			 */
			classPattern.toString();
		}
	}
}
