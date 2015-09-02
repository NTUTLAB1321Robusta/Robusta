package ntut.csie.robusta.marker;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;

public class InappropriateAnnotation extends Annotation {

	private static final String INAPPROPRIATE_ANNOTATION = "ntut.csie.robusta.inappropriateAnnotation";
	private final Position position;

	public InappropriateAnnotation(int offset, int length) {
		super(INAPPROPRIATE_ANNOTATION, false, null);
		position = new Position(offset, length);
	}
	
	public InappropriateAnnotation(int offset, int length, String description) {
		super(INAPPROPRIATE_ANNOTATION, false, description);
		position = new Position(offset, length);
	}

	public Position getPosition() {
		return position;
	}

}
