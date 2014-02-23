package ntut.csie.analyzer.careless.closingmethod;

import java.io.IOException;

public class UserDefinedCarelessCleanupMethod {
	
	public void bark() {
	}
	
	public void rain() {
	}
	
	public void Shine() throws IOException {
		throw new IOException("There are some clouds");
	}

	public void Shine(int i) throws IOException {
		throw new IOException("There are some clouds =" + i);
	}
}
