package serversocket.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import serversocket.models.User;

public class ServerWorker extends Thread {
	private Server server;
	private Socket clientSocket;
	private InputStream clientInput;
	private OutputStream clientOutput;
	private BufferedReader bf;
	private HashMap<String, User> loginPairs;
	private HashSet<User> onlineUsers;
	private HashSet<String> usedUsernames;
	private ArrayList<ServerWorker> serverWorkers;
	private User loggedIn;

	ServerWorker(Server server, Socket clientSocket) {
		this.server = server;
		this.clientSocket = clientSocket;
	}

	@Override
	public void run() {
		try {
			handleClients();
		} catch (IOException e) {
			e.printStackTrace();
		}
		super.run();
	}

	// Quick accessible method to send message through outputstream
	public void sendMessage(String message) throws IOException {
		clientOutput.flush();
		clientOutput.write((message + "\n").getBytes());
	}

	// Quick accessible method to broadcast message to every logged in instance
	public void broadcastMessage(String message){
		serverWorkers.forEach((worker) -> {
			if (worker.getLoggedIn() != null && worker.getLoggedIn() != this.getLoggedIn()) {
				try {
					worker.sendMessage(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	// Ran initially and keeps running until quit flag is set
	private void handleClients() throws IOException {
		String input;
		String[] tokens;

		clientInput = clientSocket.getInputStream();
		clientOutput = clientSocket.getOutputStream();
		onlineUsers = server.getOnlineUsers();
		serverWorkers = server.getServerWorkers();
		loginPairs = server.getLoginPairs();
		bf = new BufferedReader(new InputStreamReader(clientInput));
		while (!clientSocket.isClosed() && (input = bf.readLine().trim()) != null) {
			tokens = input.split(" ", 3);
			if (!(tokens.length < 3)) {
				handleCommands(tokens);
			} else {
				handleCommands(input);
			}
		}
	}

	// Closes everything properly
	private void handleShutdown() throws IOException {
		bf.close();
		clientInput.close();
		clientOutput.close();
		clientSocket.close();
	}

	// Specially for handling single token commands and default response
	private void handleCommands(String input) throws IOException {
		switch (input.toLowerCase()) {
		case "quit":
			handleShutdown();
			break;
		case "logout":
			handleLogout();
			break;
		case "help":
			displayHelp();
			break;
		case "list":
			displayAllUsers();
			break;

		default:
			sendMessage("Usage : <Command> <Attribute/person> <Content>");
		}
	}

	// Specially for handling 3 token commands
	private void handleCommands(String[] tokens) throws IOException {
		// Separates input into 3 tokens
		String command = tokens[0].toLowerCase();
		String attribute = tokens[1];
		String content = tokens[2];

		// handle Input
		switch (command) {
		case "login":
			handleLogin(attribute, content);
			break;
		case "register":
			handleRegisterUser(attribute, content);
			break;
		case "message":
			if (!handleMessageUser(attribute, content)) {
				sendMessage("User is offline/Message is not sent");
			}
			break;
		default:
			sendMessage("Please use appropriate commands!");
		}

	}

	// Handles the login
	private void handleLogin(String attribute, String content) throws IOException {
		if ((loggedIn = verifyUser(attribute, content)) != null) {
			server.addOnlineUsers(loggedIn);
			sendMessage("Success");
			broadcastMessage("Online " + attribute);
		} else {
			sendMessage("Failed");
		}
	}

	// Messages a user that's online
	private boolean handleMessageUser(String recipientName, String message) throws IOException {
		User currentUser = this.getLoggedIn();
		if (currentUser == null) {
			sendMessage("Not logged in!");
			return false;
		}
		for (ServerWorker worker : serverWorkers) {
			User user = worker.getLoggedIn();
			if (user.getUsername().equalsIgnoreCase(recipientName) && onlineUsers.contains(user)) {
				String messageBody = "message " + currentUser.getUsername() + " : " + message;
				worker.sendMessage(messageBody);
				return true;
			}
		}
		return false;
	}

	// Displays help messages
	private void displayHelp() throws IOException {
		String[] helpCommands = new String[] { "help - Displays this help menu",
				"register <Username> <Password> - Register as a new user",
				"login <Username> <Password> - Login as a user", "logout - Logs you out to main menu",
				"list - Lists all online users", "message <Recipient> <Message> - Message someone",
				"quit - Exit program" };
		for (String helpCommand : helpCommands) {
			sendMessage(helpCommand);
		}
	}

	// Lists all online users
	private void displayAllUsers() throws IOException {
		int onlineCount = server.getOnlineUsers().size();
		for (User onlineUser : server.getOnlineUsers()) {
			sendMessage(onlineUser.getUsername());
		}
		sendMessage("Total " + onlineCount + " online.");
	}

	// Resets the reference to the User object, clearing the User logged in
	private void handleLogout() throws IOException {
		if (loggedIn != null) {
			sendMessage("Logged out user : " + loggedIn.getUsername());
			broadcastMessage("Offline " + loggedIn.getUsername());
			onlineUsers.remove(loggedIn);
			loggedIn = null;
		} else {
			sendMessage("Not logged in!");
		}
	}

	// Registers a new user
	private void handleRegisterUser(String username, String password) throws IOException {
		usedUsernames = server.getUsedUsernames();
		String key = username + "-" + password;
		if (!(usedUsernames.contains(username))) {
			loginPairs.put(key, new User(username, password));
			usedUsernames.add(username);
		} else
			sendMessage("User already exists!");

	}

	// Verifies Login Credentials
	private User verifyUser(String username, String password) throws IOException {
		String key = username + "-" + password;
		if (!loginPairs.isEmpty()) {
			if (loginPairs.containsKey(key)) {
				return loginPairs.get(key);
			} else {
				return null;
			}
		} else {
			sendMessage("There are currently no registered users!");
			return null;
		}
	}

	public User getLoggedIn() {
		return loggedIn;
	}

}
