package client;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultCaret;

import server.ServerClient;

public class ClientWindow extends JFrame implements Runnable {
	private static final long serialVersionUID = 1L;
	
	final int MAX_ATTEMPTS = 5;
	
	int attempt = 0;
	
	private JPanel contentPane;
	private JTextField txtMessage;
	private JTextArea txtrHistory;
	private DefaultCaret caret;
	private Thread run, listen, connection;
	private Client client;
	
	private boolean running = false;
	private JMenuBar menuBar;
	private JMenu mnFile;
	private JMenuItem mntmOnlineUsers;
	private JMenuItem mntmExit;

	private OnlineUsers users;

	public ClientWindow(String name, String address, int port, String key) {
		createWindow();
		client = new Client(name, address, port, key);
		boolean connect = client.openConnection(address);
		if (!connect) {
			console("Ошибка соединения, неверный формат ip адреса!");
		}
		console("Попытка соединения с " + address + ":" + port + ", пользователь: " + name);
		String connection = "/c/" + name + "/e/";
		client.send(connection.getBytes());
		users = new OnlineUsers();
		running = true;
		run = new Thread(this, "Runnig");
		run.start();
	}

	private void createWindow() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
		setTitle("Чат");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(880, 550);
		setLocationRelativeTo(null);
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);

		mnFile = new JMenu("Файл");
		menuBar.add(mnFile);

		mntmOnlineUsers = new JMenuItem("Онлайн");
		mntmOnlineUsers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				users.setVisible(true);
			}
		});
		mnFile.add(mntmOnlineUsers);

		mntmExit = new JMenuItem("Закрыть");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String disconnect = "/d/" + client.getID() + "/e/";
				client.send(disconnect.getBytes());
				running = false;
				client.close();
				dispose();
			}
		});
		mnFile.add(mntmExit);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		
		GridBagLayout gbl_contentPane = new GridBagLayout();
		gbl_contentPane.columnWidths = new int[] {0, 0};
		gbl_contentPane.rowHeights = new int[] {0, 0, 0};
		contentPane.setLayout(gbl_contentPane);
		
		txtrHistory = new JTextArea();
		txtrHistory.setEditable(false);
		JScrollPane scroll = new JScrollPane(txtrHistory);
		caret = (DefaultCaret)txtrHistory.getCaret();
		caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
		txtrHistory.setFont(new Font("Tahoma", Font.PLAIN, 13));
		GridBagConstraints gbc_txtrHistory = new GridBagConstraints();
		gbc_txtrHistory.insets = new Insets(20, 20, 5, 20);
		gbc_txtrHistory.fill = GridBagConstraints.BOTH;
		gbc_txtrHistory.gridx = 0;
		gbc_txtrHistory.gridy = 0;
		gbc_txtrHistory.gridwidth = 2;
		gbc_txtrHistory.weightx = 1;
		gbc_txtrHistory.weighty = 1;
		contentPane.add(scroll, gbc_txtrHistory);
		
		txtMessage = new JTextField();
		txtMessage.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				if(e.getKeyCode() == KeyEvent.VK_ENTER) {
					send();
				}
			}
		});
		GridBagConstraints gbc_txtMessage = new GridBagConstraints();
		gbc_txtMessage.insets = new Insets(5, 20, 20, 5);
		gbc_txtMessage.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtMessage.gridx = 0;
		gbc_txtMessage.gridy = 1;
		gbc_txtMessage.weightx = 1;
		gbc_txtMessage.weighty = 0;
		contentPane.add(txtMessage, gbc_txtMessage);
		txtMessage.setColumns(10);
		
		JButton btnSend = new JButton("Отправить");
		btnSend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				send();
			}
		});
		GridBagConstraints gbc_btnSend = new GridBagConstraints();
		gbc_btnSend.insets = new Insets(5, 5, 20, 20);
		gbc_btnSend.gridx = 1;
		gbc_btnSend.gridy = 1;
		gbc_btnSend.weightx = 0;
		gbc_btnSend.weighty = 0;
		contentPane.add(btnSend, gbc_btnSend);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				String disconnect = "/d/" + client.getID() + "/e/";
				client.send(disconnect.getBytes());
				running = false;
				client.close();
				dispose();
			}
		});
		
		setVisible(true);
		
		txtMessage.requestFocusInWindow();
	}
	
	public void run() {
		listen();
		connectionServer();
	}

	public void console(String message) {
		txtrHistory.append(message + "\n\r");
		txtrHistory.setCaretPosition(txtrHistory.getDocument().getLength());
	}
	
	private void send() {
		String message = txtMessage.getText();
		if(message.equals("")) return;
		message = client.getName() + "(" + client.getID() + ")" + ": " + message;
		message = "/m/" + message + "/e/";
		client.send(message.getBytes());
		txtMessage.setText("");
	}
	
	public void listen() {
		listen = new Thread("Listen") {
			public void run() {
				while (running) {
					String message = client.receive();
					if (message.startsWith("/c/")) {
						attempt = 0;
						client.setID(Integer.parseInt(message.split("/c/|/e/")[1]));
						console("Успешное соединение с сервером! ID: " + client.getID());
						String text = "/m/" + "Пользователь " + client.getName() + "(" + client.getID() + ") подключился к серверу" + "/e/";
						client.send(text.getBytes());
					} else if(message.startsWith("/m/")) {
						attempt = 0;
						String text = message.split("/m/|/e/")[1];
						console(text);
					} else if(message.startsWith("/i/")) {
						attempt = 0;
						String text = "/i/" + client.getID() + "/e/";
						client.send(text.getBytes());
					} else if (message.startsWith("/u/")) {
						attempt = 0;
						String[] u = message.split("/u/|/n/|/e/");
						users.update(Arrays.copyOfRange(u, 1, u.length - 1));
					} else if(message.startsWith("/d/")) {
						attempt = 0;
						console("Вы были удалены администратором сервера");
						running = false;
						client.close();
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						dispose();
					}
				}
			}
		};
		listen.start();
	}
	
	private void connectionServer() {
		connection = new Thread("Connection") {
			public void run() {
				while(running) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if(attempt >= MAX_ATTEMPTS) {
						console("Превышен лимит времени подключения");
						running = false;
						client.close();
						try {
							Thread.sleep(10000);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						dispose();
					} else {
						++attempt;
					}
				}
			}
		};
		connection.start();
	}
}