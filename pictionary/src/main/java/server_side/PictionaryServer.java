package server_side;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import client_side.PictionaryClientException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import pictionary.pictionaryProtocolParser;

public class PictionaryServer implements Runnable {
	public static int SERVER_PORT = 25000;
	private ConcurrentLinkedQueue<ClientHandler> users = new ConcurrentLinkedQueue<ClientHandler>();
	private int userCount = 0;

	public PictionaryServer() {
		new Thread(this).start();
	}

	public static void main(String[] args) {
		new PictionaryServer();

	}

	public void run() {
		try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
			int port = serverSocket.getLocalPort();
			String address = InetAddress.getLocalHost().getHostAddress();
			System.out.println("Server starts on port  " + port);
			System.out.println("Host address: " + address);

			while (userCount < 4) {
				Socket clientSocket = serverSocket.accept();
				if (clientSocket != null) {
					System.out.println("Connection accepted");
					ClientHandler userHandler = new ClientHandler(this, clientSocket);
					users.add(userHandler);
					userCount++;
				}

			}

			while (true) {
			} // to keep thread alive

		} catch (IOException ioException) {
			System.out.println("Issues with listening thread");
		}

	}

	public ClientHandler getClientHandlerById(final String id) {
		for (ClientHandler handler : users) {
			if (handler.getUserId() == id) {
				return handler;
			}
		}
		return null;
	}

	public boolean isNameTaken(String userName) {
		for (ClientHandler handler : users) {
			if (userName.equals(handler.getUserId())) {
				return true;
			}
		}

		return false;
	}

	public int users() {
		return users.size();
	}

	public void addClientHandler(ClientHandler clientHandler) {
		users.add(clientHandler);
	}

	public void removeHandler(ClientHandler clientHandler) {
		users.remove(clientHandler);
		userCount--;
	}

	public ArrayList<String> getUsersIdList() {
		ArrayList<String> nameList = new ArrayList<String>();
		for (ClientHandler handler : users) {
			String name = handler.getUserId();
			nameList.add(name);
		}
		return nameList;
	}

}

@EqualsAndHashCode
class ClientHandler implements Runnable {

	private @Getter @Setter String userId = null;

	private PictionaryServer server = null;
	private Socket socket = null;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;

	ClientHandler(PictionaryServer server, Socket socket) {
		this.server = server;
		this.socket = socket;
		new Thread(this).start();
	}

	@Override
	public void run() {
		try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream input = new ObjectInputStream(socket.getInputStream());) {

			System.out.println("Client handler starts");
			outputStream = output;
			inputStream = input;
			String message = (String) inputStream.readObject();

			if (message != null) {
				try {
					newConnectionStartup(message);
				} catch (IOException fatalException) {
					server.removeHandler(this);
					return;
				}
			}

			while (true) {
				message = (String) input.readObject();
				parseProtocolMessage(message);
			}

			
		} catch (Exception exception) {
			System.out.println("Client connecetion failed");
			server.removeHandler(this);
			return;
		}

	}

	public void nameValidation(String userDeclaredName) throws PictionaryServerException, IOException {
		while (server.isNameTaken(userDeclaredName)) {

			sendMessageFromServerToClient("Error", "NameValidation");

			try {
				userDeclaredName = (String) inputStream.readObject();
			} catch (ClassNotFoundException exception) {
				System.out.println("Name validation went wrong");
			}

		}

		userId = userDeclaredName;
	}

	private void newConnectionStartup(String message) throws PictionaryServerException, IOException {

		ObjectMapper mapper = new ObjectMapper();
		JsonNode greetingMessage = mapper.readTree(message);
		if (greetingMessage.path("messageType").asText().equals("NameValidation")) {

			String userDeclaredName = greetingMessage.path("message").asText();

			if (server.isNameTaken(userDeclaredName)) {
				sendMessageFromServerToClient("Error", "NameValidation");
				
				try {
					String userNewName;
					userNewName = (String) inputStream.readObject();
					newConnectionStartup(userNewName);
				} catch (ClassNotFoundException e) {
					throw new PictionaryServerException("Name validation exception");
				}
				
			} else {
				userId = userDeclaredName;
			}
		} else {
			throw new PictionaryServerException("Message dosent contain name attribute.");
		}

	}

	public void sendMessageFromServerToClient(String messageType, String message) throws PictionaryServerException {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode node = mapper.createObjectNode();
			((ObjectNode) node).put("sender", "server");
			((ObjectNode) node).put("receiver", "client");
			((ObjectNode) node).put("messageType", messageType);
			((ObjectNode) node).put("message", message);
			String nameValidationMessage = mapper.writeValueAsString(node);
			sendMessageToClient(nameValidationMessage);

		} catch (JsonProcessingException exception) {
			System.out.println("JSON wrapping went wrong");
			throw new PictionaryServerException();
		}
	}

	public void sendMessageToClient(String message) throws PictionaryServerException {
		try {
			outputStream.writeObject(message);
		} catch (IOException exception) {
			throw new PictionaryServerException("Server failed, connection lost");
		}
	}
	
	public void parseProtocolMessage(String plainMessage) throws JacksonException, PictionaryServerException {
		HashMap<String, String> messageInfo=pictionaryProtocolParser.parseProtocol(plainMessage);
		
		if(messageInfo.get("receiver").equals("server")) {
			// do server stuff
		}
	}

	@Override
	public String toString() {
		return "Handler of" + userId + "\n";

	}

}