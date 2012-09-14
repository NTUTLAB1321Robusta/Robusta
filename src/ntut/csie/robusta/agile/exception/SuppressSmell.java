package ntut.csie.robusta.agile.exception;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
// PARAMETER刪掉，在catch裡面的annotation用ant javac會compile不過
@Target({ METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SuppressSmell {
	/* annotation裡面不能有過多資訊，否則ant javac會compile不過 
	//ignore exception的type
	public static final String CS_INGNORE_EXCEPTION = "Ignore_Checked_Exception";
	
	//Dummy Handler的type
	public static final String CS_DUMMY_HANDLER = "Dummy_Handler";
	
	//Nested Try Block的type
	public static final String CS_NESTED_TRY_BLOCK = "Nested_Try_Block";
	
	//Unprotected Main Program
	public static final String CS_UNPROTECTED_MAIN = "Unprotected_Main_Program";
	
	//Careless CleanUp
	public static final String CS_CARELESS_CLEANUP = "Careless_CleanUp";
	
	//Over Logging
	public static final String CS_OVER_LOGGING = "Over_Logging";
	
	public static final String[] CS_TOTAL_TYPE = new String[]{ CS_INGNORE_EXCEPTION,
		CS_DUMMY_HANDLER, CS_NESTED_TRY_BLOCK, CS_UNPROTECTED_MAIN, CS_CARELESS_CLEANUP, CS_OVER_LOGGING};

	public static final String[] CS_CATCH_TYPE = new String[]{ CS_INGNORE_EXCEPTION,
		CS_DUMMY_HANDLER, CS_OVER_LOGGING, CS_CARELESS_CLEANUP};*/
	
	String[] value();
}
