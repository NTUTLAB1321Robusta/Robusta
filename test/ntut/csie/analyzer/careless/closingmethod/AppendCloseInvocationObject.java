package ntut.csie.analyzer.careless.closingmethod;

import java.io.IOException;

public class AppendCloseInvocationObject {
	ClassImplementCloseable obj;
	public AppendCloseInvocationObject(){
		obj = new ClassImplementCloseable();
	}
	public ClassImplementCloseable getClosableObj() throws IOException{
		return obj;
	}
}
