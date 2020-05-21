package serversocket.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import serversocket.models.User;

public class Server extends Thread {
	private ServerSocket serverSocket;
	private ArrayList<ServerWorker> serverWorkers = new ArrayList<ServerWorker>();
	private HashSet<String> usedUsernames = new HashSet<String>();
	private HashMap<String, User> loginPairs = new HashMap<String, User>();
	private HashSet<User> onlineUsers = new HashSet<User>();

	public Server(int port) {
		final int PORT = port;
		try {
			serverSocket = new ServerSocket(PORT);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		handleConnections();
		super.run();
	}

	// Handles all the accepting of connections
	private void handleConnections() {
		try {
			while (true) {
				System.out.println("Waiting...");
				Socket clientSocket = serverSocket.accept();
				// Initializes a server worker thread to handle each specific connection into
				// server
				ServerWorker worker = new ServerWorker(this, clientSocket);
				worker.start();
				serverWorkers.add(worker);
				System.out.println("Accepted connections from " + clientSocket);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public ArrayList<ServerWorker> getServerWorkers() {
		return serverWorkers;
	}

	public HashSet<String> getUsedUsernames() {
		return usedUsernames;
	}

	public HashMap<String, User> getLoginPairs() {
		return loginPairs;
	}

	public HashSet<User> getOnlineUsers() {
		return onlineUsers;
	}

	public void addLoginPairs(String key, User user) {
		loginPairs.put(key, user);
	}

	public void addUsedUsernames(String username) {
		usedUsernames.add(username);
	}

	public void addOnlineUsers(User user) {
		onlineUsers.add(user);
	}
}
