package ntut.csie.failFastUT.UnprotectedMain;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

public class NoEHBlockInMainExample {
	public static void main(String[] args){
		demo();
		System.out.println("For unprotectedMainExample");
	}
	public static void demo() {
		java.sql.Connection conn = null;
		FileWriter fw;
		try {
			fw = new FileWriter("test.txt");
			fw.write("test");
	        fw.flush();
	        fw.close();
	        conn = DriverManager.getConnection("test0","test1","test2");
	        
		}catch (IOException e) {
			e.printStackTrace();
		}catch (SQLException e) {
			e.printStackTrace();
		}
	}
	private static void throwEx() throws SQLException {
		throw new SQLException();
	}
}
