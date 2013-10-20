package ntut.csie.robusta.agile.exception;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.CONSTRUCTOR;

;

@Documented
@Target( { METHOD, CONSTRUCTOR })
@Retention(RetentionPolicy.RUNTIME)
public @interface Robustness {
	static final String VALUE = "value"; 
	RTag[] value();
}

