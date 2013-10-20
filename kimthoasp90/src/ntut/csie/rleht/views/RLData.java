package ntut.csie.rleht.views;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

public class RLData {
	public static final String CLASS_ROBUSTNESS = Robustness.class.getName();

	public static final String CLASS_RL = RTag.class.getName();
	
	public static final int LEVEL_MIN = 1;



	public static final int LEVEL_MAX = 3;
	
	private int level = RTag.LEVEL_1_ERR_REPORTING;

	private String exceptionType;

	public RLData(){
	}
	
	public RLData(int level, String type) {
		this.level = level;
		this.exceptionType = type;
	}

	public String getExceptionType() {
		return exceptionType;
	}

	public void setExceptionType(String exceptionType) {
		this.exceptionType = exceptionType;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}


	public static int[] getLevels() {
		int[] levels = new int[LEVEL_MAX - LEVEL_MIN + 1];
		for (int i = LEVEL_MIN; i <= LEVEL_MAX; i++) {
			levels[i - LEVEL_MIN] = i;
		}
		return levels;
	}

	public static boolean validLevel(int level) {
		return (level >= LEVEL_MIN && level <= LEVEL_MAX);
	}

	public static int levelSize() {
		return LEVEL_MAX - LEVEL_MIN + 1;
	}
}
