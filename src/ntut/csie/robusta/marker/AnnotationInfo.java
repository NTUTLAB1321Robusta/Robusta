package ntut.csie.robusta.marker;

public class AnnotationInfo {

	private int startLine;
	private int startOffSet;
	private int length;
	private String description;

	public AnnotationInfo(int startLine, int startOffSet, int length,
			String description) {
		this.startLine = startLine;
		this.startOffSet = startOffSet;
		this.length = length;
		this.description = description;
	}

	public int getStartLine() {
		return startLine;
	}

	public int getStartOffSet() {
		return startOffSet;
	}

	public int getLength() {
		return length;
	}

	public String getDescription() {
		return description;
	}
}
