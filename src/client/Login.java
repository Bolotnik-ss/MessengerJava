package client;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.UIManager;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class Login extends JFrame {
	private static final long serialVersionUID = 1L;
	
	private JPanel contentPane;
	private JTextField txtName;
	private JTextField txtAddress;
	private JTextField txtPort;
	private JLabel lblName;
	private JLabel lblIpAddress;
	private JLabel lblPort;
	private JLabel lblAddressDesc;
	private JLabel lblPortDesc;

	public Login() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception e) {
			e.printStackTrace();
		}
		setResizable(false);
		setTitle("Авторизация");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(300, 450);
		setLocationRelativeTo(null);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		lblName = new JLabel("Имя:");
		lblName.setBounds(119, 34, 56, 16);
		contentPane.add(lblName);
		
		txtName = new JTextField();
		txtName.setBounds(64, 50, 165, 28);
		contentPane.add(txtName);
		txtName.setColumns(10);
		
		lblIpAddress = new JLabel("IP Адрес:");
		lblIpAddress.setBounds(119, 96, 56, 16);
		contentPane.add(lblIpAddress);
		
		txtAddress = new JTextField();
		txtAddress.setBounds(64, 116, 165, 28);
		contentPane.add(txtAddress);
		txtAddress.setColumns(10);
		
		lblAddressDesc = new JLabel("(\u043D\u0430\u043F\u0440\u0438\u043C\u0435\u0440 192.168.0.2)");
		lblAddressDesc.setBounds(77, 142, 171, 16);
		contentPane.add(lblAddressDesc);
		
		lblPort = new JLabel("Порт:");
		lblPort.setBounds(130, 171, 34, 16);
		contentPane.add(lblPort);
		
		txtPort = new JTextField();
		txtPort.setBounds(64, 191, 165, 28);
		contentPane.add(txtPort);
		txtPort.setColumns(10);
		
		lblPortDesc = new JLabel("(\u043D\u0430\u043F\u0440\u0438\u043C\u0435\u0440 8192)");
		lblPortDesc.setBounds(95, 218, 104, 16);
		contentPane.add(lblPortDesc);
		
		JButton btnLogin = new JButton("Войти");
		btnLogin.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String name = txtName.getText();
				String address = txtAddress.getText();
				int port = Integer.parseInt(txtPort.getText());
				String key = txtKey.getText();
				login(name, address, port, key);
			}
		});
		btnLogin.setBounds(88, 326, 117, 29);
		contentPane.add(btnLogin);
		
		txtKey = new JTextField();
		txtKey.setBounds(64, 266, 165, 28);
		contentPane.add(txtKey);
		txtKey.setColumns(10);
		
		JLabel lblKey = new JLabel("\u041A\u043B\u044E\u0447:");
		lblKey.setBounds(129, 247, 46, 16);
		contentPane.add(lblKey);
	}
	
	private JTextField txtKey;
	
	private void login(String name, String address, int port, String key) {
		dispose();
		new ClientWindow(name, address, port, key);
	}
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Login frame = new Login();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
}