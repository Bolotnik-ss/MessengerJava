package server;

public class ServerMain {
	private int port;
	private static String key;
	private Server server;
	
	public ServerMain(int port, String key) {
		this.port = port;
		this.key = key;
		server = new Server(this.port, this.key);
	}
	
	public static void main(String[] args) {
		int port;
		if(args.length != 2) {
			System.out.println("Usage: java -jar Server.jar [port] [key]");
			return;
		}
		else {
			port = Integer.parseInt(args[0]);
			key = args[1];
		}
		new ServerMain(port, key);
	}
}