package ntut.csie.jdt.util;

public class BoundaryChecker {

	private int lowerBound;
	private int upperBound;

	public BoundaryChecker(int lowerBound, int upperBound) {
		if (lowerBound >= upperBound) {
			throw new IllegalArgumentException();
		}

		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public boolean isInInterval(int value) {
		return (value > lowerBound) && (value < upperBound);
	}
}
