package ntut.csie.filemaker.exceptionBadSmells;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

public class OverwrittenLeadExceptionExample {
	/**
	 * 沒有Finally Block，不會被加上mark
	 * @param context
	 * @param outputFile
	 */
	public void withoutFinally(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * 會被OverwrittenLeadExceptionVisitor在fileOutputStream.close();加上mark
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void withFinally(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			fileOutputStream.close();	// Overwritten
		}
	}
	
	/**
	 * 會被OverwrittenLeadExceptionVisitor在fileOutputStream.close();加上mark
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void closeStreamInIfStatementWithFinally(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if(fileOutputStream != null)
				fileOutputStream.close();	// Overwritten
		}
	}
	
	/**
	 * 會被OverwrittenLeadExceptionVisitor在fileOutputStream.close();加上mark
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void closeStreamInWhileStatementWithFinally(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			while(fileOutputStream != null) {
				fileOutputStream.close();	// Overwritten
			}
		}
	}
	
	/**
	 * 會被OverwrittenLeadExceptionVisitor在fileOutputStream.close();加上mark
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	public void closeStreamInNestedTryStatementWithoutFinally(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				fileOutputStream.close();
			} catch(IOException e) {
				throw new RuntimeException(e);	// Overwritten
			}
		}
	}
	
	/**
	 * 共2個
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void closeStreamInNestedTryStatementWithFinally(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				fileOutputStream.close();
			} catch(IOException e) {
				throw new RuntimeException(e);	// Overwritten
			} finally {
				fileOutputStream.close();	// Overwritten
			}
		}
	}
	
	/**
	 * 共2個
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void closeStreamInNestedTryStatementWithoutCatch(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				fileOutputStream.close();	// Overwritten
			} finally {
				fileOutputStream.close();	// Overwritten
			}
		}
	}
	
	/**
	 * 共3個
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void closeStreamInNestedTryStatementInCatchWithFinally(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null, fileOutputStream2 = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			try {
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e1) {
				throw new RuntimeException(e1);
			} catch(IOException e1) {
				throw new RuntimeException(e1);
			} finally {
				fileOutputStream2.close();	// Overwritten
			}
			throw e;
		} finally {
			try {
				fileOutputStream.close();
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e) {
				e.notify();
			} catch(IOException e) { 
				throw new RuntimeException(e);	// Overwritten
			} finally {
				fileOutputStream.close();	// Overwritten
			}
		}
	}
	
	/**
	 * 共2個
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void closeStreamInNestedTryStatementInCatchWithoutFinally(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null, fileOutputStream2 = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			try {
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e1) {
				throw new RuntimeException(e1);
			} catch(IOException e1) {
				throw new RuntimeException(e1);
			}
		} finally {
			try {
				fileOutputStream.close();
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e) {
				e.notify();
			} catch(IOException e) { 
				throw new RuntimeException(e);	// Overwritten
			} finally {
				fileOutputStream.close();	// Overwritten
			}
		}
	}
	
	/**
	 * 共5個
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void closeStreamInThreeNestedTryStatement(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null, fileOutputStream2 = null, fileOutputStream3 = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				fileOutputStream.close();
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e) {
				e.notify();
			} catch(IOException e) { 
				throw new RuntimeException(e);	// Overwritten
			} finally {
				fileOutputStream.close();	// Overwritten
				try {
					fileOutputStream3 = new FileOutputStream(outputFile);
					fileOutputStream3.write(context);
				} catch(FileNotFoundException e) {
					throw new RuntimeException(e);	// Overwritten
				} catch(IOException e) {
					throw new RuntimeException(e);	// Overwritten
				} finally {
					fileOutputStream3.close();	// Overwritten
				}
			}
		}
	}
	
	/**
	 * 共2個
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void closeStreamInThreeNestedTryStatementInCatch(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null, fileOutputStream2 = null, fileOutputStream3 = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			try {
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e1) {
				throw new RuntimeException(e1);
			} catch(IOException e1) {
				throw new RuntimeException(e1);
			} finally {
				try {
					fileOutputStream3 = new FileOutputStream(outputFile);
					fileOutputStream3.write(context);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					throw new RuntimeException(e1);	// Overwritten
				} finally {
					try {
						fileOutputStream3.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
			throw e;
		} finally {
			try {
				fileOutputStream.close();
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e) {
				e.notify();
			} catch(IOException e) { 
				throw new RuntimeException(e);	// Overwritten
			} finally {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 不會被OverwrittenLeadExceptionVisitor加上mark
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void closeStreamInThreeNestedTryStatementInBoth(byte[] context, File outputFile) {
		FileOutputStream fileOutputStream = null, fileOutputStream2 = null, fileOutputStream3 = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			try {
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e1) {
				e1.printStackTrace();
			} catch(IOException e1) {
				e1.printStackTrace();
			} finally {
				try {
					fileOutputStream3 = new FileOutputStream(outputFile);
					fileOutputStream3.write(context);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} finally {
					try {
						fileOutputStream3.close();
					} catch (IOException e1) {
						e1.printStackTrace();
					}
				}
			}
		} finally {
			try {
				fileOutputStream.close();
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e) {
				e.notify();
			} catch(IOException e) { 
				e.printStackTrace();
			} finally {
				try {
					fileOutputStream.close();
					fileOutputStream3 = new FileOutputStream(outputFile);
					fileOutputStream3.write(context);
				} catch(FileNotFoundException e) {
					e.printStackTrace();
				} catch(IOException e) {
					e.printStackTrace();
				} finally {
					try {
						fileOutputStream3.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	/**
	 * 共3個
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void closeStreamInThreeNestedTryStatementInCatch2(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null, fileOutputStream2 = null, fileOutputStream3 = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			try {
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e1) {
				throw new RuntimeException(e1);
			} catch(IOException e1) {
				try {
					fileOutputStream3 = new FileOutputStream(outputFile);
					fileOutputStream3.write(context);
				} catch (FileNotFoundException e2) {
					e2.printStackTrace();
				} catch (IOException e2) {
					throw new RuntimeException(e2);
				} finally {
					try {
						fileOutputStream3.close();
					} catch (IOException e2) {
						throw e2;					// Overwritten
					}
				}
				throw e1;
			} finally {
				fileOutputStream.close();			// Overwritten
			}
			throw e;
		} finally {
			try {
				fileOutputStream.close();
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e) {
				e.notify();
			} catch(IOException e) { 
				throw new RuntimeException(e);	// Overwritten
			} finally {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 共5個
	 * @param context
	 * @param outputFile
	 * @throws IOException
	 */
	@Robustness(value = { @RTag(level = 1, exception = java.io.IOException.class) })
	public void closeStreamInThreeNestedTryStatement2(byte[] context, File outputFile) throws IOException {
		FileOutputStream fileOutputStream = null, fileOutputStream2 = null, fileOutputStream3 = null;
		try {
			fileOutputStream = new FileOutputStream(outputFile);
			fileOutputStream.write(context);
		} catch(FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				fileOutputStream.close();
				fileOutputStream2 = new FileOutputStream(outputFile);
				fileOutputStream2.write(context);
			} catch(FileNotFoundException e) {
				try {
					fileOutputStream3 = new FileOutputStream(outputFile);
					fileOutputStream3.write(context);
				} catch(FileNotFoundException e1) {
					throw new RuntimeException(e1);	// Overwritten
				} catch(IOException e1) {
					throw new RuntimeException(e1);	// Overwritten
				} finally {
					fileOutputStream3.close();	// Overwritten
				}
			} catch(IOException e) { 
				throw new RuntimeException(e);	// Overwritten
			} finally {
				fileOutputStream.close();	// Overwritten
			}
		}
	}
}
