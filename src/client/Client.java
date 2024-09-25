package client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class Client {
	private DatagramSocket socket;

	private String name, address;
	private int port;
	private InetAddress ip;
	private Thread send;
	
	private int ID = -1;
	
	private String key;

	public Client(String name, String address, int port, String key) {
		this.name = name;
		this.address = address;
		this.port = port;
		this.key = key;
	}

	public String getName() {
		return name;
	}

	public String getAddress() {
		return address;
	}

	public int getPort() {
		return port;
	}

	public boolean openConnection(String address) {
		try {
			socket = new DatagramSocket();
			ip = InetAddress.getByName(address);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return false;
		} catch (SocketException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public String receive() {
		byte[] data = new byte[1024];
		DatagramPacket packet = new DatagramPacket(data, data.length);
		try {
			socket.receive(packet);
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] datab = packet.getData();
		coder(datab);
		String message = new String(datab);
		return message;
	}

	public void send(byte[] data) {
		coder(data);
		sendwithoutcoder(data);
	}
	
	public void sendwithoutcoder(final byte[] data) {
		send = new Thread("Send") {
			public void run() {
				DatagramPacket packet = new DatagramPacket(data, data.length, ip, port);
				try {
					socket.send(packet);
				} catch (IOException e) {
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
	
	public void close() {
		new Thread() {
			public void run() {
				synchronized (socket) {
					socket.close();
				}
			}
		}.start();
	}
	
	public void setID(int ID) {
		this.ID = ID;
	}

	public int getID() {
		return ID;
	}
}