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
	
	/** Suppress Smell Error */
	public static final String ERR_SS_NO_SMELL = "ERR_SS_NO_SMELL";

	public static final String ERR_SS_FAULT_NAME = "ERR_SS_FAULT_NAME";

	public static final String ERR_SS_DUPLICATE = "ERR_SS_DUPLICATE";

	/** Suppress Smell In Catch */
	public static final String SS_IN_CATCH = "SS_IN_CATCH";

	


	/** ignore exceptionªºtype */
	public static final String CS_INGNORE_EXCEPTION = "Ignored_Checked_Exception";
	
	/** Dummy Handlerªºtype */
	public static final String CS_DUMMY_HANDLER = "Dummy_Handler";
	
	/** Nested Try Blockªºtype */
	public static final String CS_NESTED_TRY_BLOCK = "Nested_TryStatement";
	
	/** Unprotected Main Program */
	public static final String CS_UNPROTECTED_MAIN = "Unprotected_Main_Program";
	
	/** Careless CleanUp */
	public static final String CS_CARELESS_CLEANUP = "Careless_Cleanup";
	
	/** Over Logging */
	public static final String CS_OVER_LOGGING = "Over_Logging";
	
	/** Overwritten Lead */
	public static final String CS_OVERWRITTEN_LEAD_EXCEPTION = "Overwritten_Lead_Exception";
	
	/** Exception Robustness Level Advice */
	public static final String CS_EXCEPTION_RLADVICE  = "Exception_RL_Advice";

	/** Careless cleanup with try block */
	public static final String CCU_WITH_TRY = "Careless_Cleanup_With_Try_Block";
	
	/** MethodInvocation with exceptions */
	public static final String MI_WITH_Ex = "MethodInvocation_With_Exceptions";
	
	
	/** Total Code Smell Type */
	public static final String[] CS_TOTAL_TYPE = new String[]{ CS_INGNORE_EXCEPTION,
		CS_DUMMY_HANDLER, CS_NESTED_TRY_BLOCK, CS_UNPROTECTED_MAIN, CS_CARELESS_CLEANUP, CS_OVER_LOGGING, CS_OVERWRITTEN_LEAD_EXCEPTION};

	/** Total Smell Type In Catch */
	public static final String[] CS_CATCH_TYPE = new String[]{ CS_INGNORE_EXCEPTION,
		CS_DUMMY_HANDLER, CS_OVER_LOGGING};
}
