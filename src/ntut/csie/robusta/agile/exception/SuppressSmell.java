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
// PARAMETER�R���A�bcatch�̭���annotation��ant javac�|compile���L
@Target({ METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE })
@Retention(RetentionPolicy.RUNTIME)
public @interface SuppressSmell {
	/* annotation�̭����঳�L�h��T�A�_�hant javac�|compile���L 
	//ignore exception��type
	public static final String CS_EMPTY_CATCH_BLOCK = "Empty_Catch_Block";
	
	//Dummy Handler��type
	public static final String CS_DUMMY_HANDLER = "Dummy_Handler";
	
	//Nested Try Block��type
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
