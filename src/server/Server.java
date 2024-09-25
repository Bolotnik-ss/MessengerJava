package server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Server implements Runnable {
	private List<ServerClient> clients = new ArrayList<ServerClient>();
	private List<Integer> clientResponse = new ArrayList<Integer>();
	
	private DatagramSocket socket;
	private int port;
	private boolean running = false;
	private Thread run, manage, send, receive;
	
	private String key;
	
	private final int MAX_ATTEMPTS = 5;
	
	private boolean raw = false;
	private boolean encoding = true;
	
	public Server(int port, String key) {
		this.port = port;
		this.key = key;
		try {
			socket = new DatagramSocket(port);
		} catch(SocketException e) {
			e.printStackTrace();
		}
		run = new Thread(this, "Server");
		run.start();
	}

	public void run() {
		running = true;
		System.out.println("Server started on port " + port);
		System.out.println("Key: " + key);
		manageClients();
		receive();
		Scanner scanner = new Scanner(System.in);
		while (running) {
			String text = scanner.nextLine();
			if (!text.startsWith("/")) {
				String string = "/m/" + "Server: " + text + "/e/";
				sendToAll(string);
				history(string);
				continue;
			}
			text = text.substring(1);
			if (text.equals("raw")) {
				if (raw) System.out.println("Raw mode off.");
				else System.out.println("Raw mode on.");
				raw = !raw;
			} else if (text.equals("clients")) {
				System.out.println("Clients:");
				System.out.println("========");
				for (int i = 0; i < clients.size(); i++) {
					ServerClient c = clients.get(i);
					System.out.println(c.name + "(" + c.getID() + "): " + c.address.toString() + ":" + c.port);
				}
				System.out.println("========");
			} else if (text.startsWith("kick")) {
				String name = text.split(" ")[1];
				int id = -1;
				boolean number = true;
				try {
					id = Integer.parseInt(name);
				} catch (NumberFormatException e) {
					number = false;
				}
				if (number) {
					boolean exists = false;
					for (int i = 0; i < clients.size(); i++) {
						if (clients.get(i).getID() == id) {
							exists = true;
							break;
						}
					}
					if (exists) disconnect(id, 2);
					else System.out.println("Client " + id + " doesn't exist! Check ID number.");
				} else {
					for (int i = 0; i < clients.size(); i++) {
						ServerClient c = clients.get(i);
						if (name.equals(c.name)) {
							disconnect(c.getID(), 2);
							break;
						}
					}
				}
			} else if(text.equals("clearhistory")) {
				File file = new File("history.bin");
				if(file.delete()) {
					System.out.println("History has been cleared");
				}
				else {
					System.out.println("History has already been cleared");
				}
			} else if(text.equals("encoding")) {
				if (encoding) System.out.println("Encoding mode off.");
				else System.out.println("Encoding mode on.");
				encoding = !encoding;
				
			} else if (text.equals("help")) {
				printHelp();
			} else if (text.equals("quit")) {
				quit();
			} else {
				System.out.println("Unknown command.");
				printHelp();
			}
		}
		scanner.close();
	}
	
	private void printHelp() {
		System.out.println("Here is a list of all available commands:");
		System.out.println("=========================================");
		System.out.println("/raw - enables raw mode.");
		System.out.println("/clients - shows all connected clients.");
		System.out.println("/kick [users ID or username] - kicks a user.");
		System.out.println("/clearhistory - clears a history of messages");
		System.out.println("/encoding - disable encoding mode.");
		System.out.println("/help - shows this help message.");
		System.out.println("/quit - shuts down the server.");
		System.out.println("=========================================");
	}
	
	private void manageClients() {
		manage = new Thread("Manage") {
			public void run() {
				while(running) {
					sendToAll("/i/server/e/");
					sendStatus();
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					for (int i = 0; i < clients.size(); i++) {
						ServerClient c = clients.get(i);
						if (!clientResponse.contains(c.getID())) {
							if (c.attempt >= MAX_ATTEMPTS) {
								disconnect(c.getID(), 1);
							} else {
								c.attempt++;
							}
						} else {
							clientResponse.remove(new Integer(c.getID()));
							c.attempt = 0;
						}
					}
				}
			}
		};
		manage.start();
	}
	
	private void sendStatus() {
		if (clients.size() <= 0) return;
		String users = "/u/";
		for (int i = 0; i < clients.size() - 1; i++) {
			users += clients.get(i).name + "(" + clients.get(i).getID() + ")" + "/n/";
		}
		users += clients.get(clients.size() - 1).name + "(" + clients.get(clients.size() - 1).getID() + ")" +  "/e/";
		sendToAll(users);
	}
	
	private void receive() {
		receive = new Thread("Receive") {
			public void run() {
				while(running) {
					byte[] data = new byte[1024];
					DatagramPacket packet = new DatagramPacket(data, data.length);
					try {
						socket.receive(packet);
					} catch (SocketException e) {
					} catch (IOException e) {
						e.printStackTrace();
					}
					process(packet);
				}
			}
		};
		receive.start();
	}
	
	private void history(String message) {
		try {
			File file = new File("history.bin");
			file.createNewFile();
			FileOutputStream hystoryfile = new FileOutputStream(file, true);
			String mess = "/m/" + message.split("/m/|/e/")[1] + "/e/";
			byte[] mess_byte = mess.getBytes();
			coder(mess_byte);
			for(int i = 0; i < mess_byte.length; ++i) {
				hystoryfile.write(mess_byte[i]);
			}
			hystoryfile.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendhistory(ServerClient c) {
		try {
			File file = new File("history.bin");
			file.createNewFile();
			FileInputStream historyfile = new FileInputStream(file);
			int data;
			List<Byte> b = new ArrayList<Byte>();
			send("/m/История сообщений:/e/".getBytes(), c.address, c.port);
			c.attempt = 0;
			while((data = historyfile.read()) != -1) {
				b.add((byte)data);
				byte[] bdec = new byte[b.size()];
				for(int i = 0; i < b.size(); ++i) {
					bdec[i] = b.get(i);
				}
				coder(bdec);
				String s = new String(bdec);
				if(s.startsWith("/m/") && s.endsWith("/e/")) {
					String mess = "/m/" + s.split("/m/|/e/")[1] + "/e/";
					send(mess.getBytes(), c.address, c.port);
					b.clear();
					c.attempt = 0;
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			send("/m/После вашего подключения:/e/".getBytes(), c.address, c.port);
			c.attempt = 0;
			historyfile.close();
		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private void sendToAll(String message) {
		if (!raw && encoding && message.startsWith("/m/")) {
			String log = message.split("/m/|/e/")[1];
			System.out.println(log);
		}
		else if(!raw && !encoding && message.startsWith("/m/")) {
			String log = message.split("/m/|/e/")[1];
			byte[] enbyte = log.getBytes();
			coder(enbyte);
			String enstring = new String(enbyte);
			System.out.println(enstring);
		}
		for(int i = 0; i < clients.size(); i ++) {
			ServerClient client = clients.get(i);
			send(message.getBytes(), client.address, client.port);
		}
	}
	
	private void send(byte[] data, final InetAddress address, final int port) {
		coder(data);
		sendwithoutcoder(data, address, port);
	}
	
	private void sendwithoutcoder(final byte[] data, final InetAddress address, final int port) {
		send = new Thread("Send") {
			public void run() {
				DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
				try {
					socket.send(packet);
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		};
		send.start();
	}
	
	public void coder(byte[] data) {
		for(int i = 0; i < data.length; ++i) {
			byte[] keyb = key.getBytes();
			int j = i % keyb.length;
			data[i] ^= keyb[j]; 
		}
	}
	
	private void process(DatagramPacket packet) {
		byte[] packetb = packet.getData();
		if(raw && !encoding) {
			String enstring = new String(packetb);
			System.out.println(enstring);
		}
		coder(packetb);
		String string = new String(packetb);
		if(raw && encoding) System.out.println(string);
		if(string.startsWith("/c/")) {
			int id = UnicueIdentifier.getIdentifier();
			String name = string.split("/c/|/e/")[1];
			ServerClient c = new ServerClient(name, packet.getAddress(), packet.getPort(), id);
			clients.add(c);
			System.out.println("Try to connected by " + packet.getAddress() + ":" + packet.getPort() + ", user:" + name);
			System.out.println("ID: " + id);
			String ID = "/c/" + id + "/e/";
			send(ID.getBytes(), packet.getAddress(), packet.getPort());
			sendhistory(c);
		} else if(string.startsWith("/m/")) {
			sendToAll(string);
			history(string);
		} else if (string.startsWith("/d/")) {
			String id = string.split("/d/|/e/")[1];
			disconnect(Integer.parseInt(id), 0);
		} else if (string.startsWith("/i/")) {
			clientResponse.add(Integer.parseInt(string.split("/i/|/e/")[1]));
		} else {
			System.out.println(string);
		}
	}
	
	private void quit() {
		for (int i = 0; i < clients.size(); i++) {
			disconnect(clients.get(i).getID(), 0);
		}
		running = false;
		socket.close();
	}
	
	private void disconnect(int id, int status) {
		String log = "";
		String message = "";
		ServerClient c = null;
		boolean existed = false;
		for (int i = 0; i < clients.size(); i++) {
			if (clients.get(i).getID() == id) {
				c = clients.get(i);
				clients.remove(i);
				existed = true;
				break;
			}
		}
		if(!existed)return;
		if (status == 0) {
			log = "Client " + c.name + "(" + c.getID() + ") @ " + c.address.toString() + ":" + c.port + " was disconnected.";
			message = "/m/" + "Пользователь " + c.name + " (" + c.getID() + ") отключился от сервера" + "/e/";
		} else if (status == 1) {
			log = "Client " + c.name + "(" + c.getID() + ") @ " + c.address.toString() + ":" + c.port + " was timed out.";
			message = "/m/" + "Пользователь " + c.name + " (" + c.getID() + ") был отключен от сервера из-за непредвиденных обстоятельств" + "/e/";
		} else if (status == 2) {
			log = "Client " + c.name + "(" + c.getID() + ") @ " + c.address.toString() + ":" + c.port + " was kicked.";
			message = "/m/" + "Пользователь " + c.name + " (" + c.getID() + ") был удален администратором сервера" + "/e/";
			String er = "/d/" + "/e";
			send(er.getBytes(), c.address, c.port);
		}
		System.out.println(log);
		sendToAll(message);
		history(message);
	}
}
