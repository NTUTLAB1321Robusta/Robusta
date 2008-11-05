package ntut.csie.csdet.fixture;

public class MMFixture {
	public MMFixture(){}

	public String getSource(){
	StringBuffer buffer = new StringBuffer();

	
	buffer.append("package a.b.c;\n");
	buffer.append("\n");
	buffer.append("import java.io.FileOutputStream;\n");
	buffer.append("import java.io.IOException;\n");
	buffer.append("\n");
	
	buffer.append("public class MM {\n");
	buffer.append("	public void ignore(){\n");
	buffer.append("		try {");
	buffer.append("	System.out.println(\"Hello\");\n");
	buffer.append("	} catch (NumberFormatException e) {");
	buffer.append("	}\n");
	buffer.append("	try {\n");
	buffer.append("	} catch (Exception e) {\n");
	buffer.append("	}\n");
	buffer.append("	}\n");

	buffer.append("	public void ignore2(String path){\n");
	buffer.append("		try {\n");
	buffer.append("		FileOutputStream fos = new FileOutputStream(path);\n");
	buffer.append("		fos.close();\n");
	buffer.append("	} catch (IOException e) {\n");
	buffer.append("		try{\n");
	buffer.append("		System.out.println(\"second try-block\");\n");
	buffer.append("		}catch(Exception ex){\n");
	buffer.append("		}\n");
	buffer.append("		}finally{\n");
	buffer.append("		}\n");
	buffer.append("		}\n");

	buffer.append("	public void ignore3(){\n");
	buffer.append("		try{\n");
	buffer.append("	}catch(Exception e){\n");
	buffer.append("	System.out.println(\"no empty!!!!\");\n");
	buffer.append("	}finally{\n");
	buffer.append("	try{\n");
	buffer.append("	}catch(Exception e){\n");
	buffer.append("	}\n");
	buffer.append("	}\n");
	buffer.append("	}\n");
	buffer.append("	}\n");
	return buffer.toString();
	}
	
}
