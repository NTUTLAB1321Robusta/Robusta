package ntut.csie.robusta.agile.exception;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface RTag {
	public static final int LEVEL_1_ERR_REPORTING = 1;

	int level() default 1;

	//we can't check whether class extends Throwable, so we can't use Class<Throwable> exception()
	Class<?> exception() default Throwable.class;
	
	/** The level of thrown exception */
	public static final String LEVEL = "level";
	
	/** The full qualified name of the exception */
	public static final String EXCEPTION = "exception";
}

