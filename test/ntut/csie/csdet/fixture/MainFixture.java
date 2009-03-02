package ntut.csie.csdet.fixture;

public class MainFixture {
	
	
	public String getSource(){
		StringBuffer buffer = new StringBuffer();
		buffer.append("package a.b.c;\n");
		buffer.append("\n");
		
		buffer.append("public class MainFixture {\n");
		
		buffer.append("	public static void main (String[] args) {\n");
		buffer.append("     String num = \"1\";\n");
		buffer.append("		try {");
		buffer.append("	        int x = Integer.parseInt(num);\n");
		buffer.append("	    } catch (Exception e) {");
		buffer.append("	         e.printStackTrace();"); 
		buffer.append("	    }\n");
		buffer.append("	}\n");

		buffer.append("	}\n");
		return buffer.toString();
	}
}
