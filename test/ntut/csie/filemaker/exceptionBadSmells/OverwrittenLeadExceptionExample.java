package ntut.csie.filemaker.exceptionBadSmells;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import ntut.csie.robusta.agile.exception.RTag;
import ntut.csie.robusta.agile.exception.Robustness;

public class OverwrittenLeadExceptionExample {
	/**
	 * �S��Finally Block�A���|�Q�[�Wmark
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
	 * �|�QOverwrittenLeadExceptionVisitor�bfileOutputStream.close();�[�Wmark
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
	 * �|�QOverwrittenLeadExceptionVisitor�bfileOutputStream.close();�[�Wmark
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
	 * �|�QOverwrittenLeadExceptionVisitor�bfileOutputStream.close();�[�Wmark
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
	 * �|�QOverwrittenLeadExceptionVisitor�bfileOutputStream.close();�[�Wmark
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
	 * �@2��
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
	 * �@2��
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
	 * �@3��
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
	 * �@2��
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
	 * �@5��
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
	 * �@2��
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
	 * ���|�QOverwrittenLeadExceptionVisitor�[�Wmark
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
	 * �@3��
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
	 * �@5��
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
	 * ���ե]�ttry�����ҥ~�A�u�n�|�л\�e�����ҥ~�A���|�Q�����X��
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
	 * ���ݨҤl�A�Y�ϧ����S�ʧ@�� try block�A�u�n finally �����ҥ~�ߥX�A���M��ڤW���|�o�� overwritten lead exception
	 * ���]�������W������A�u��Ȯɻ{���O OW �a���D
	 * @Author pig
	 */
	public void throwButWithoutExceptionBeforeWithDH() throws IOException {
		try {
		} finally {
			throw new IOException();  // Overwritten
		}
	}

	/**
	 * ���M finally �����ҥ~�ߥX�A���]�� Catch �F Exception �åu�� Dummy Handle
	 * �ҥH��ڤW���|�o�� overwritten lead exception
	 * ���]�������W������A�u��Ȯɻ{���O OW �a���D
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
	 * ���M finally �����ҥ~�ߥX�A���]�� Catch �F Exception �ìO Empty Catch Block
	 * �ҥH��ڤW���|�o�� overwritten lead exception
	 * ���]�������W������A�u��Ȯɻ{���O OW �a���D
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
	 * �b catch ���ݪ� try block ���A�� try statement ���S�� finally ����
	 * �� catch �������`�欰�����|�Q�аO�� OW
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
	 * Bug �����d��
	 * �b catch ���ݪ� try block ���A�u�n�� finally ���ܡA�L�ר�O�_�|�ߨҥ~
	 * �������ϸ� catch �������`�欰�Q�аO�� OW
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
	 * Bug �����d��
	 * �b catch �e�� finally�A����� ���ݪ� try block ���~��
	 * ���|�ϸ� catch �������`�欰�Q�аO�� OW
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
	 * Bug �����d��
	 * �b catch ���ݪ� try block ���A�u�n�� finally ����
	 * �Y�Ϩ����L try statement �� catch block ��
	 * �]�����ϸ� catch �������`�欰�Q�аO�� OW
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
