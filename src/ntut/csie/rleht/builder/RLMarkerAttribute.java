package ntut.csie.rleht.builder;

public interface RLMarkerAttribute {
	public static final String ERR_RL_LEVEL = "ERR_RL_LEVEL";

	public static final String ERR_NO_RL = "ERR_NO_RL";
	
	public static final String ERR_RL_DUPLICATE = "ERR_RL_DUPLICATE";
	
	public static final String ERR_RL_INSTANCE = "ERR_RL_INSTANCE";

	public static final String RL_MARKER_TYPE = "RL_MARKER_TYPE";

	public static final String RL_INFO_LEVEL = "RL_INFO_LEVEL";

	public static final String RL_INFO_EXCEPTION = "RL_INFO_EXCEPTION";

	public static final String RL_INFO_SRC_POS = "RL_INFO_SRC_POS";

	public static final String RL_METHOD_INDEX = "RL_METHOD_INDEX";

	public static final String RL_MSG_INDEX = "RL_MSG_INDEX";
	
	public static final String RL_INFO_SUPPORT_REFACTORING = "RL_INFO_SUPPORT_REFACTORING";
	
	/** Suppress Smell Error */
	public static final String ERR_SS_NO_SMELL = "ERR_SS_NO_SMELL";

	public static final String ERR_SS_FAULT_NAME = "ERR_SS_FAULT_NAME";

	/** Suppress Smell In Catch */
	public static final String SS_IN_CATCH = "SS_IN_CATCH";

	


	/** Empty Catch Block **/
	public static final String CS_EMPTY_CATCH_BLOCK = "Empty_Catch_Block";
	
	/** Dummy Handler **/
	public static final String CS_DUMMY_HANDLER = "Dummy_Handler";
	
	/** Nested Try Statement **/
	public static final String CS_NESTED_TRY_STATEMENT = "Nested_Try_Statement";
	
	/** Unprotected Main Program **/
	public static final String CS_UNPROTECTED_MAIN = "Unprotected_Main_Program";
	
	/** Careless Cleanup **/
	public static final String CS_CARELESS_CLEANUP = "Careless_Cleanup";
	
	/** Over Logging **/
	public static final String CS_OVER_LOGGING = "Over_Logging";
	
	/** Throws Exception In Finally Block **/
	public static final String CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK = "Exception_Thrown_From_Finally_Block";
	
	/** Exception Robustness Level Advice */
	public static final String CS_EXCEPTION_RLADVICE  = "Exception_RL_Advice";

	/** Careless cleanup with try block */
	public static final String CCU_WITH_TRY = "Careless_Cleanup_With_Try_Block";
	
	/** MethodInvocation with exceptions */
	public static final String MI_WITH_Ex = "MethodInvocation_With_Exceptions";
	
	
	/** Total Code Smell Type */
	public static final String[] CS_TOTAL_TYPE = new String[]{ CS_EMPTY_CATCH_BLOCK,
		CS_DUMMY_HANDLER, CS_NESTED_TRY_STATEMENT, CS_UNPROTECTED_MAIN, CS_CARELESS_CLEANUP, CS_OVER_LOGGING, CS_EXCEPTION_THROWN_FROM_FINALLY_BLOCK};

	/** Total Smell Type In Catch */
	public static final String[] CS_CATCH_TYPE = new String[]{ CS_EMPTY_CATCH_BLOCK,
		CS_DUMMY_HANDLER, CS_OVER_LOGGING};
}
