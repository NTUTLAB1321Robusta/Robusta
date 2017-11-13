package ntut.csie.failFastUT.Empty;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.sun.corba.se.pept.transport.Connection;
public class emptyExample {
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
		}
	}
	private static void throwEx() throws SQLException {
		throw new SQLException();
	}
}
