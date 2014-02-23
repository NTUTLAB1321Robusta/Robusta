package ntut.csie.analyzer.careless;

public class FirstLevelChildStatementExample {

	public void methodWithTwoStatements() {
		System.out.println("Hi");
		;
	}

	public void methodWithEmptyBlock() {

	}

	public void methodWithACommentUsingSemiColon() {
		// ;
	}

	public void methodWithBlockCommentUsingSemiColon() {
		/*
		 * ;
		 */
	}

	public void methodWithTwoStatementsInATry() {
		try {
			System.out.println("");
			int i = 1;
		} finally {

		}
	}

	public void methodWithTwoStatementsInATryAndFinally() {
		try {
			System.out.println("");
		} finally {
			System.out.println("Finally");
		}
	}

	public void methodWithTwoStatementsInAndOutATry() {
		try {
			System.out.println("in");
		} finally {

		}
		System.out.println("out");
	}

	public void methodWithNestedTry() {
		try {
			System.out.println("most outter try");
			try {
				System.out.println("inner try");
				try {
					System.out.println("most inner try");
				} finally {
					;
				}
			} finally {
				;
			}
		} finally {
			;
		}
	}

	public void methodWithTwoTry() {
		try {
			System.out.println("in");
		} finally {

		}

		try {
			System.out.println("in");
		} finally {

		}
		System.out.println("out");
	}

	public void methodWithNestedBlocks() {
		boolean i = false;

		if (i) {
			System.out.println("most outter try");
			try {
				System.out.println("inner try");
				try {
					System.out.println("most inner try");
				} finally {
					;
				}
			} finally {
				;
			}
		}
	}

	/**
	 * The outerMethodInvocation contains an methodInvocation as an argument
	 */
	public void methodWithMethodInvocationAsArgument() {
		outerMethodInvocation(methodInvocationAsArgument());
	}

	public void outerMethodInvocation(int i) {
		System.out.println(i);
	}

	private int methodInvocationAsArgument() {
		return 5;
	}
}
