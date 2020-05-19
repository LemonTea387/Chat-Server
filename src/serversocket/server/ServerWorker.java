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

import serversocket.user.User;

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
		clientOutput.write(message.getBytes());
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

		clientOutput.write("Type \'help\' to get a list of all commands \n".getBytes());
		while ((input = bf.readLine()) != null) {
			tokens = input.split(" ", 3);
			if (!(tokens.length < 3)) {
				handleCommands(tokens);
			} else {
				handleCommands(input);
			}
		}
	}

	// Specially for handling single token commands and default response
	private void handleCommands(String input) throws IOException {
		String[] helpCommands = new String[] { "help - Displays this help menu \n",
				"register <Username> <Password> - Register as a new user \n",
				"login <Username> <Password> - Login as a user\n", "logout - Logs you out to main menu\n",
				"list - Lists all online users\n", "message <Recipient> <Message> - Message someone\n",
				"quit - Exit program\n" };
		if ("Quit".equalsIgnoreCase(input))
			clientSocket.close();
		else if ("Help".equalsIgnoreCase(input)) {
			for (String helpCommand : helpCommands) {
				clientOutput.write(helpCommand.getBytes());
			}
		} else if ("List".equalsIgnoreCase(input)) {
			listAllUsers();
		} else if ("Logout".equalsIgnoreCase(input)) {
			logoutUser();
		} else
			sendMessage("Usage : <Command> <Attribute/person> <Content> \n");
	}

	// Specially for handling 3 token commands
	private void handleCommands(String[] tokens) throws IOException {
		// Separates input into 3 tokens
		String command = tokens[0];
		String attribute = tokens[1];
		String content = tokens[2];

		// handle Input
		switch (command) {
		case "login":
			if ((loggedIn = verifyUser(attribute, content)) != null) {
				server.addOnlineUsers(loggedIn);
				sendMessage("Logged in as : " + loggedIn.getUsername() + "\n");
			} else
				sendMessage("Try again! \n");
			break;
		case "register":
			registerUser(attribute, content);
			break;
		case "message":
			messageUser(attribute, content);
			break;
		default:
			sendMessage("Please use appropriate commands!");
		}

	}

	private void messageUser(String recipientName, String message) throws IOException {
		for (ServerWorker worker : serverWorkers) {
			User currentUser = worker.getLoggedIn();
			if (currentUser.getUsername().equalsIgnoreCase(recipientName) && onlineUsers.contains(currentUser)) {
				worker.sendMessage(message);
			}
		}

	}

	private void listAllUsers() throws IOException {
		int onlineCount = server.getOnlineUsers().size();
		for (User onlineUser : server.getOnlineUsers()) {
			sendMessage(onlineUser.getUsername() + "\n");
		}
		sendMessage("Total " + onlineCount + " online. \n");
	}

	public User getLoggedIn() {
		return loggedIn;
	}

	// Resets the reference to the User object, clearing the User logged in
	private void logoutUser() throws IOException {
		if (loggedIn != null) {
			sendMessage("Logged out user : " + loggedIn.getUsername() + "\n");
			onlineUsers.remove(loggedIn);
			loggedIn = null;
		} else {
			sendMessage("Not logged in!");
		}
	}

	// Registers a new user
	private void registerUser(String username, String password) throws IOException {
		usedUsernames = server.getUsedUsernames();
		String key = username + "-" + password;
		if (!(usedUsernames.contains(username))) {
			loginPairs.put(key, new User(username, password));
			usedUsernames.add(username);
		} else
			sendMessage("User already exists!\n");

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
			sendMessage("There are currently no registered users!\n");
			return null;
		}

	}

}
