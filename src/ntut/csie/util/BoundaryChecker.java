package ntut.csie.util;

import org.eclipse.jdt.core.dom.ASTNode;

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

	public boolean isInOpenInterval(int value) {
		return (value > lowerBound) && (value < upperBound);
	}

	public boolean isInClosedInterval(int value) {
		return (value >= lowerBound) && (value <= upperBound);
	}

	/**
	 * If the start position of this node is in the given interval.
	 */
	public boolean isInOpenInterval(ASTNode node) {
		return isInOpenInterval(node.getStartPosition());
	}
	/**
	 * If the start position of this node is in the given interval.
	 */
	public boolean isInClosedInterval(ASTNode node) {
		return isInClosedInterval(node.getStartPosition());
	}
}
