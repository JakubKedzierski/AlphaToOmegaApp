package pictionary;

import client_side.PictionaryClient;
import server_side.PictionaryServer;

public class ConnectionTest {

	public static void main(String[] args) {
		new PictionaryServer();
		try {
			Thread.sleep(1000);
			new PictionaryClient("test1");
			Thread.sleep(400);
			new PictionaryClient("test2");
			Thread.sleep(400);
			new PictionaryClient("test2");
			Thread.sleep(400);
			new PictionaryClient("test4");

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

}
