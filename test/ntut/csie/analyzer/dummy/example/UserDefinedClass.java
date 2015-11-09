package ntut.csie.analyzer.dummy.example;

import java.io.FileInputStream;
import java.io.IOException;

public class UserDefinedClass {

	/**
	 * use outer class in catch clause to test user defined pattern 
	 */
	public void method() {
		UserDefinedClassDeclaration classPattern = new UserDefinedClassDeclaration();
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("");
			fis.read();
		} catch (IOException e) {
			// user defined type1 - when meet a template like "[javaFilePath].ClassPattern." - true
			classPattern.eat();
			/*
			 * user defined type1 - when meet a template like "[javaFilePath].UserDefinedClassDeclaration." - true
			 * there is a System.out.println() invocation in swim(), but swim() would not be detected as "*.toString()"
			 */
			classPattern.swim();
		} catch (Exception e) {
			/*
			 * ser defined type1 - when meet a template like "*[javaFilePath].UserDefineDummyHandlerFish.*" - false
		 	 * this "userDefineDummyHandlerFish.toString()" will not be detected, due to its' .toString() is inherited from Object class.
		 	 * if "userDefineDummyHandlerFish.toString()"'s .toString() is a override method, userDefineDummyHandlerFish.toString() will be detected as "*.toString()".
			 */
			classPattern.toString();
		}
	}
}
