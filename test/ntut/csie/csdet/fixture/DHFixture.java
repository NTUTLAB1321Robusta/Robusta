package ntut.csie.csdet.fixture;

/**
 * µ¹´údummy handler¨Ï¥Î
 * @author chewei
 */

public class DHFixture {
	public DHFixture(){}
	
	
	public String getSource(){
	StringBuffer buffer = new StringBuffer();

	
	buffer.append("package a.b.c;\n");
	buffer.append("\n");
	buffer.append("import java.io.FileOutputStream;\n");
	buffer.append("import java.io.IOException;\n");
	buffer.append("\n");
	
	buffer.append("public class DHFixture {\n");
	
	buffer.append("	public void dummy(){\n");
	buffer.append("     String num = \"1\";\n");
	buffer.append("		try {");
	buffer.append("	        int x = Integer.parseInt(num);\n");
	buffer.append("	    } catch (NumberFormatException e) {");
	buffer.append("	         e.printStackTrace();"); //dummy handler
	buffer.append("	    }\n");
	buffer.append("	}\n");

	buffer.append("	public void dummy2(String path){\n");
	buffer.append("		try {\n");
	buffer.append("		    FileOutputStream fos = new FileOutputStream(path);\n");
	buffer.append("		    fos.close();\n");
	buffer.append("	    }catch (IOException e) {\n");
	buffer.append("		try{\n");
	buffer.append("		    System.out.println(\"second try-block\");\n");
	buffer.append("		}catch(Exception ex){\n");
	buffer.append("	        e.printStackTrace();"); //dummy handler
	buffer.append("		}\n");
	buffer.append("		}finally{\n");
	buffer.append("		}\n");
	buffer.append("	}\n");

	buffer.append("	public void dummy3(){\n");
	buffer.append("		try{\n");
	buffer.append("	    }catch(Exception e){\n");
	buffer.append("	         e.printStackTrace();\n");
	buffer.append("	        throw new RuntimeException(e);\n");
	buffer.append("	}finally{\n");
	buffer.append("	}\n");
	buffer.append("	}\n");
	buffer.append("	}\n");
	return buffer.toString();
	}
}
