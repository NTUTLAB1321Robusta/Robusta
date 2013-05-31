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

	/**
	 * 測試包含try內的例外，只要會覆蓋前面的例外，都會被偵測出來
	 * 
	 * @Author pig
	 */
	public void overwrittenInTryBlock(String filePath, byte[] context)
			throws IOException {
		FileOutputStream fis = null;
		try {
			fis = new FileOutputStream(filePath);
			fis.write(context);
		} finally {
			try {
				fis.write(context); // Overwritten
			} finally {
				fis.close(); // Overwritten
			}
		}
	}

	/**
	 * 極端例子，即使完全沒動作的 try block，只要 finally 內有例外拋出，雖然實際上不會發生 overwritten lead exception
	 * 但因為偵測上的限制，工具暫時認為是 OW 壞味道
	 * @Author pig
	 */
	public void throwButWithoutExceptionBeforeWithDH() throws IOException {
		try {
		} finally {
			throw new IOException();  // Overwritten
		}
	}

	/**
	 * 雖然 finally 內有例外拋出，但因為 Catch 了 Exception 並只做 Dummy Handle
	 * 所以實際上不會發生 overwritten lead exception
	 * 但因為偵測上的限制，工具暫時認為是 OW 壞味道
	 * 
	 * @Author pig
	 */
	public void throwButWithoutExceptionBeforeWithDH(String filePath,
			byte[] context) throws IOException {
		FileOutputStream fis = null;
		try {
			fis = new FileOutputStream(filePath);
			fis.write(context);
		} catch (Exception e) {
			System.out.println("An exception in try block");
		} finally {
			fis.close(); // Overwritten
		}
	}

	/**
	 * 雖然 finally 內有例外拋出，但因為 Catch 了 Exception 並是 Empty Catch Block
	 * 所以實際上不會發生 overwritten lead exception
	 * 但因為偵測上的限制，工具暫時認為是 OW 壞味道
	 * 
	 * @Author pig
	 */
	public void throwButWithoutExceptionBeforeWithECB(String filePath,
			byte[] context) throws IOException {
		FileOutputStream fis = null;
		try {
			fis = new FileOutputStream(filePath);
			fis.write(context);
		} catch (Exception e) {
		} finally {
			fis.close(); // Overwritten
		}
	}

	/**
	 * 在 catch 附屬的 try block 內，有 try statement 但沒有 finally 的話
	 * 該 catch 內的正常行為都不會被標記為 OW
	 * 
	 * @Author pig
	 */
	public void tryWithCatchInTryBlock(String filePath,
			byte[] context) throws IOException {
		FileOutputStream fis = null;
		try {
			fis = new FileOutputStream(filePath);
			try {
				fis.write(context);
			} catch (IOException e) {
				throw new IOException();
			}
		} catch (Exception e) {
			throw new IOException();  // Not Overwritten
		}
	}

	/**
	 * Bug 相關範例
	 * 在 catch 附屬的 try block 內，只要有 finally 的話，無論其是否會拋例外
	 * 都不應使該 catch 內的正常行為被標記為 OW
	 * 
	 * @Author pig
	 */
	public void tryWithFinallyInTryBlock(String filePath,
			byte[] context) throws IOException {
		FileOutputStream fis = null;
		try {
			fis = new FileOutputStream(filePath);
			try {
				fis.write(context);
			} finally {
			}
		} catch (Exception e) {
			int i = 10;
			i++;
			if (i==10)
				throw new IOException();  // Not Overwritten
			throw new IOException();  // Not Overwritten
		}
	}


	/**
	 * Bug 相關範例
	 * 在 catch 前有 finally，但位於 附屬的 try block 之外時
	 * 不會使該 catch 內的正常行為被標記為 OW
	 * 
	 * @Author pig
	 */
	public static void tryWithFinallyOutOfTryBlock() throws IOException {
		try {
		} finally {
		}

		try {
		} catch (Exception e) {
			throw new IOException("Not an overwritten");  // Not Overwritten
		} finally {
		}
	}

	/**
	 * Bug 相關範例
	 * 在 catch 附屬的 try block 內，只要有 finally 的話
	 * 即使其位於其他 try statement 的 catch block 內
	 * 也不應使該 catch 內的正常行為被標記為 OW
	 * 
	 * @Author pig
	 */
	public static void multiNestedTryTryWithFinally1() throws IOException {
		try {
			try {
			} catch (Exception e) {
				try {
				} finally {
					throw new IOException("May cause wrong OW");  // Overwritten
				}
			}
		} catch (Exception e) {
			// Not Overwritten
			throw new IOException("Second effect wrong overwritten");
		}
	}

}
