package ntut.csie.robusta.agile.exception;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface RTag {
	public static final int LEVEL_1_ERR_REPORTING = 1;

	public static final int LEVEL_2_STATE_RECOVERY = 2;

	public static final int LEVEL_3_BEHAVIOR_RECOVERY = 3;
	
	int level() default 1;

	//Class不會判斷是否繼承於Throwable，所以不能用Class<Throwable> exception()
	Class<?> exception() default Throwable.class;
	
	/** The level of thrown exception */
	public static final String LEVEL = "level";
	
	/** The full qualified name of the exception */
	public static final String EXCEPTION = "exception";
}

