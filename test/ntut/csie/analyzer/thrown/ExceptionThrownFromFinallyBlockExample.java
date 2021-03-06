package ntut.csie.analyzer.thrown;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;

public class ExceptionThrownFromFinallyBlockExample {

	/**
	 * Without Finally Block, so it won't be marked
	 */
	public void withoutFinally(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void methodDeclaredExceptionInFinallySimpleCase(File outputFile)
			throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
		try {
		} finally {
			fileOutputStream.close(); // ThrownInFinally // Safe for CC
		}
	}

	public void methodDeclaredExceptionInFinally(byte[] context, File outputFile)
			throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			fileOutputStream.close(); // ThrownInFinally // Safe for CC
		}
	}

	/**
	 * Eclipse will warn "finally block does not complete normally". And
	 * Robusta should warn TEIFB bad smell, too.
	 */
	@SuppressWarnings("finally")
	public void throwExceptionInFinally(byte[] context, File outputFile)
			throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			throw new IOException(); // ThrownInFinally
		}
	}

	public void thrownStatementInIfStatementWithoutBlock(byte[] context,
			File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (fileOutputStream != null)
				fileOutputStream.close(); // ThrownInFinally // Safe for CC
		}
	}

	public void thrownStatementInWhileStatementWithBlock(byte[] context,
			File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			while (fileOutputStream != null) {
				fileOutputStream.close(); // ThrownInFinally // Unsafe for CC
				throw new IOException(); // ThrownInFinally
			}
		}
	}

	/**
	 * Without the keyword "throw" an exception won't be thrown even though we create exception object, such as "new Exception"
	 */
	public void newExceptionWithoutKeywordThrow() {
		try {
			throw new RuntimeException();
		} finally {
			new Exception(); // Not ThrownInFinally
		}
	}

	public void thrownStatementInCatchOrFinallyBlockInFinally(byte[] context,
			File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
			} catch (RuntimeException e) {
				System.out.println(e.toString());
				throw e; // ThrownInFinally
			} finally {
				fileOutputStream.close(); // ThrownInFinally // Safe for CC??
			}
		}
	}

	public void thrownStatementInTryBlockInFinally(byte[] context,
			File outputFile) throws Exception {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
		} catch (Exception outE) {
			// Ignore
		} finally {
			try {
				try {
					// Will be caught, not thrownInFinally
					fileOutputStream.write(context);
				} catch (Exception e) {
					int i = 10;
					if (i == 10) {
						// Will be caught at the catch below, not thrownInFinally
						throw new RuntimeException(e);
					} else if (i == 15) {
						// Will be caught at the catch below, not thrownInFinally
						throw new IllegalArgumentException(e);
					} else {
						// Won't be caught, is thrownInFinally
						throw new Exception(e);
					}
				}
			} catch (RuntimeException e) {
			}
		}
	}

	public void complexExampleWithTEIFB(byte[] context, File outputFile)
			throws IOException {
		FileOutputStream fileOutputStream = null, fileOutputStream2 = null, fileOutputStream3 = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				fileOutputStream.close(); // Safe for CC
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch (RuntimeException e) {
				throw e; // ThrowsInFinally
			} catch (FileNotFoundException e) {
				e.notify();
			} catch (IOException e) {
				throw new RuntimeException(e); // ThrowsInFinally
			} finally {
				fileOutputStream.close(); // ThrowsInFinally // Safe for CC
				try {
					fileOutputStream3 = new FileOutputStream(outputFile);
					fileOutputStream3.write(context);
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e); // ThrowsInFinally
				} catch (IOException e) {
					throw e; // ThrowsInFinally
				} finally {
					fileOutputStream3.close(); // ThrowsInFinally // Safe for CC
				}
			}
		}

		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			try {
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch (FileNotFoundException e1) {
				throw new RuntimeException(e1);
			} catch (IOException e1) {
				throw new RuntimeException(e1);
			} finally {
				fileOutputStream2.close(); // ThrownInFinally // Safe for CC
			}
			throw e;
		} finally {
			try {
				fileOutputStream.close(); // Safe for CC
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch (FileNotFoundException e) {
				e.notify();
			} catch (IOException e) {
				throw new RuntimeException(e); // ThrownInFinally
			} finally {
				fileOutputStream.close(); // ThrownInFinally // Safe for CC
			}
		}
	}

	/**
	 * There isn't any ThrowsInFinally
	 */
	public void complexExampleWithoutTEIFB(byte[] context, File outputFile)
			throws IOException {
		FileOutputStream fileOutputStream = null, fileOutputStream2 = null, fileOutputStream3 = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			try {
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch (FileNotFoundException e1) {
				throw e1;
			} catch (IOException e1) {
				e1.printStackTrace();
			} finally {
				try {
					/*
					 * IOException and FileNotFoundException will be caught
					 */
					fileOutputStream3 = new FileOutputStream(outputFile);
					fileOutputStream3.write(context);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					try {
						// IOException will be caught by catch block
						fileOutputStream3.close(); // Safe for CC
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		} finally {
			try {
				/*
				 * IOException and FileNotFoundException will be caught
				 */
				fileOutputStream.close();
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch (IOException e) {
				e.notify();
			} finally {
				try {
					/*
					 * IOException and FileNotFoundException will be caught
					 */
					fileOutputStream.close();
					fileOutputStream3 = new FileOutputStream(outputFile);
					fileOutputStream3.write(context);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						// IOException will be caught by catch block
						fileOutputStream3.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * Example Bug before new rule at 2013/11/19. After exit the finally
	 * block above, the thrown exception in catch block should not be marked as a
	 * TEIFB
	 */
	public static void tryWithFinallyOutOfTryBlock() throws IOException {
		try {
		} finally {
		}

		try {
		} catch (Exception e) {
			// Not ThrownInFinally
			throw new IOException("Not an thrownInFinally");
		} finally {
		}
	}

	/**
	 * Example Bug before new rule at 2013/11/19. After exit the finally
	 * block above, the thrown exception in catch block should not be marked as a
	 * TEIFB
	 */
	@SuppressWarnings("finally")
	public static void multiNestedTryTryWithFinally1() throws IOException {
		try {
			try {
			} catch (Exception e) {
				try {
				} finally {
					throw new IOException("May cause wrong TEIFB"); // ThrownInFinally
				}
			}
		} catch (Exception e) {
			// Not ThrownInFinally
			throw new IOException("Second effect wrong ThrownInFinally");
		}
	}

	/**
	 * SuperMethodInvocation should be detected
	 */
	public FilterInputStream superMethodInvocation()
			throws FileNotFoundException {
		return new FilterInputStream(new FileInputStream(new File(""))) {
			public void close() throws IOException {
			}

			protected void finalize() throws Throwable {
				try {
					close();
				} finally {
					super.finalize(); // ThrownInFinally
				}
			}
		};
	}

	/**
	 * Example Bug before new rule at 2013/11/19.
	 */
	public void antGTExample2(byte[] context, File outputFile)
			throws IOException {
		FileOutputStream fileOutputStream = null, fileOutputStream2 = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
			try {
				fileOutputStream2 = new FileOutputStream(outputFile);
			} finally {
				if (fileOutputStream != null)
					throw new RuntimeException(); // ThrownInFinally
			}
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			FileOutputStream fileOutputStream3 = null;
			try {
				/*
				 * Four statements's exception are IOException only. And it will
				 * be caught by catch block
				 */
				fileOutputStream3 = new FileOutputStream(outputFile);
				fileOutputStream.close(); // Unsafe for CC
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch (IOException e) {
				throw new RuntimeException(e); // ThrownInFinally
			} finally {
				fileOutputStream3.close(); // ThrownInFinally // Safe for CC??
			}
			if (fileOutputStream3.hashCode() != 0)
				throw new IOException(); // ThrownInFinally
		}
	}

	/**
	 * Bug example.
	 */
	public void throwInFinallyInEachBlockOfTryStatement(byte[] context,
			File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			try {
				fileOutputStream = new FileOutputStream(outputFile);
				fileOutputStream.write(context);
			} catch (IOException e) {
			} finally {
				fileOutputStream.close(); // ThrownInFinally // Safe for CC
			}
		} catch (IOException outE) {
			try {
				fileOutputStream = new FileOutputStream(outputFile);
				fileOutputStream.write(context);
			} catch (IOException e) {
			} finally {
				fileOutputStream.close(); // ThrownInFinally // Safe for CC
			}
		} finally {
			try {
				fileOutputStream = new FileOutputStream(outputFile);
				fileOutputStream.write(context);
			} catch (IOException e) {
			} finally {
				fileOutputStream.close(); // ThrownInFinally // Safe for CC
			}
		}
	}

	/**
	 * Bug example.
	 */
	public void throwInEachBlockOfTryStatementInFinally(byte[] context,
			File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
		} catch (Exception outE) {
			// Ignore
		} finally {
			try {
				try {
					// Will be caught, not thrownInFinally
					fileOutputStream.write(context);
				} catch (Exception e) {
					int i = 10;
					if (i == 10) {
						// Won't be caught, is thrownInFinally
						throw new RuntimeException(e);
					} else {
						// Will be caught, not thrownInFinally
						throw new IllegalArgumentException(e);
					}
				}
			} catch (IllegalArgumentException e) {
				try {
					// Will be caught, not thrownInFinally
					fileOutputStream.write(context);
				} catch (Exception innerE) {
					// Won't be caught, is thrownInFinally
					throw new RuntimeException(innerE);
				}
			} finally {
				try {
					// Will be caught, not thrownInFinally
					fileOutputStream.write(context);
				} catch (Exception e) {
					// Won't be caught, is thrownInFinally
					throw new RuntimeException(e);
				}
			}
		}
	}

	/**
	 * Thrown in constructor
	 */
	public ExceptionThrownFromFinallyBlockExample(File outputFile)
			throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
		try {
		} finally {
			fileOutputStream.close(); // ThrownInFinally // Safe for CC
		}
	}

	/**
	 * Thrown in initializer
	 */
	{
		FileOutputStream fileOutputStream = null;
		try {
		} finally {
			fileOutputStream.close(); // ThrownInFinally
		}
	}
}
