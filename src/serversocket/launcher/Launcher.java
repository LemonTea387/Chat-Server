package serversocket.launcher;

import serversocket.server.Server;

public class Launcher {
	public static void main(String[] args) {
		Server server  = new Server(10000);
		server.start();
	}
}
