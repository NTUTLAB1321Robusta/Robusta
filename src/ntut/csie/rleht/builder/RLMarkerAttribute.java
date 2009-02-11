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

	//ignore exception的type
	public static final String CS_INGNORE_EXCEPTION = "CS_INGNORE_EXCEPTION";
	
	//Dummy Handler的type
	public static final String CS_DUMMY_HANDLER = "CS_DUMMY_HANDLER";
	
	//Nested Try Block的type
	public static final String CS_NESTED_TRY_BLOCK = "CS_Nested_Try_Block";
	
	//Unprotected Main Program
	public static final String CS_UNPROTECTED_MAIN = "CS_Unprotected_Main_Program";
	
	//Spare Handler的type
	public static final String CS_SPARE_HANDLER = "CS_SPARE_HANDLER";
}
