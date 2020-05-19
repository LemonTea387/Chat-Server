package serversocket.server;

public class ServerSide {
	public static void main(String[] args) {
		Server server  = new Server(10000);
		server.start();
	}
	

}
