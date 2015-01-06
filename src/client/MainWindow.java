package client;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import PdiRemoteAuth.NoEnoughPrivileges;
import PdiRemoteAuth.UnknownUserOrBadPass;
import PdiRemoteAuth.UserNotLoggedIn;

public class MainWindow extends JFrame implements ActionListener, KeyListener {

	private static final long serialVersionUID = 2611344350042144298L;

	private static String programName = "PDI CORBA client";
	
	JTextField username;
	JPasswordField pass;
	JButton loginButton;
	JButton logoutButton;
	JLabel cookieLabel;
	JPasswordField pass1;
	JPasswordField pass2;
	JButton changePass;
	JButton getUsers;
	String cookie;
	Color errBgColor;
	Color okBgColor;
	JTable users;
	UsersListModel ulm;
	JButton addUser;
	JButton delUser;
	JButton edUser;
	
	/**
	 * Vytvoření hlavního okna se zadaným jménem.
	 * @param windowName jméno okna
	 */
	public MainWindow(String windowName) {
		super(windowName);
		init();
		
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		FormLayout fm = new FormLayout("3dlu, r:p, 3dlu, p, 7dlu, p, 3dlu, r:p, 3dlu, p, f:p:g, 3dlu, p, 3dlu",
				"5dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, 10dlu, 3dlu, p, 3dlu," +
				"p, 3dlu, p, 3dlu, p, 3dlu");
		CellConstraints cc = new CellConstraints();
		
		PanelBuilder pb = new PanelBuilder(fm);
		
		pb.addSeparator("Přihlášení:", cc.xyw(2, 2, 3));
		pb.addLabel("Uživatelské jméno:", cc.xy(2, 4));
		pb.add(username, cc.xy(4, 4));
		pb.addLabel("Heslo:", cc.xy(2, 6));
		pb.add(pass, cc.xy(4, 6));
		pb.add(logoutButton, cc.xy(2, 8));
		pb.add(loginButton, cc.xy(4, 8));
		pb.add(cookieLabel, cc.xyw(2, 10, 3));
		

		pb.addSeparator("Změna hesla:", cc.xyw(2, 12, 3));
		pb.addLabel("Heslo:", cc.xy(2, 14));
		pb.add(pass1, cc.xy(4, 14));
		pb.addLabel("Heslo znovu:", cc.xy(2, 16));
		pb.add(pass2, cc.xy(4, 16));
		pb.add(changePass, cc.xy(4, 18));

		pb.addSeparator("Seznam uživatelů:", cc.xyw(6, 2, 8));
		JScrollPane usersSP = new JScrollPane(users);
		usersSP.setPreferredSize(new Dimension(300, 100));
		pb.add(usersSP, cc.xywh(6, 4, 8, 13));
		pb.add(addUser, cc.xy(6, 18));
		pb.add(delUser, cc.xy(8, 18));
		pb.add(edUser, cc.xy(10, 18));
		pb.add(getUsers, cc.xy(13, 18));
		
		add(pb.getPanel());
		setResizable(false);
		pack();
	}
	
	/**
	 * Vytvoření hlavního okna s výchozím jménem okna
	 */
	public MainWindow() {
		this(programName);
	}

	/**
	 * Inicializace GUI komponent okna a nastavení výchozích vlastností
	 */
	private void init() {
		username = new JTextField(15);
		pass = new JPasswordField(15);
		loginButton = new JButton("Přihlásit");
		loginButton.addActionListener(this);
		logoutButton = new JButton("Odhlásit");
		logoutButton.addActionListener(this);
		logoutButton.setEnabled(false);
		cookieLabel = new JLabel();
		pass1 = new JPasswordField();
		pass1.setEnabled(false);
		pass1.addKeyListener(this);
		pass2 = new JPasswordField();
		pass2.setEnabled(false);
		pass2.addKeyListener(this);
		okBgColor = pass2.getBackground();
		changePass = new JButton("Změň");
		changePass.setEnabled(false);
		changePass.addActionListener(this);
		ulm = new UsersListModel();
		users = new JTable(ulm);
		getUsers = new JButton("Stáhni");
		getUsers.setEnabled(false);
		getUsers.addActionListener(this);
		
		addUser = new JButton("+");
		addUser.setEnabled(false);
		addUser.addActionListener(this);
		addUser.setToolTipText("Přidej uživatele");
		delUser = new JButton("-");
		delUser.setEnabled(false);
		delUser.addActionListener(this);
		delUser.setToolTipText("Smaž uživatele");
		edUser = new JButton("E");
		edUser.setEnabled(false);
		edUser.addActionListener(this);
		edUser.setToolTipText("Uprav uživatele");
		
		errBgColor = new Color(255, 192, 224);
	}
	
	/**
	 * Nastavení nové autentizační cookie (zajistí její uložení a zobrazení)
	 * @param cookie
	 */
	private void setCookie(String cookie) {
		this.cookie = cookie;
		cookieLabel.setText(cookie);
	}
	
	/**
	 * Přihlášení uživatele k CORBA autentizačnímu systému
	 */
	private void login() {
		if (username.getText().length() > 0) {
			try {
				setCookie(Client.getImpl().tryAuth(username.getText(),
						new String(pass.getPassword())));
				if (cookie != null) {
					loginButton.setEnabled(false);
					username.setEnabled(false);
					pass.setEnabled(false);
					logoutButton.setEnabled(true);
					pass1.setEnabled(true);
					pass2.setEnabled(true);
					getUsers.setEnabled(true);
				}
			} catch (UnknownUserOrBadPass ex) {
				System.err.println("login: " + ex.getMessage());
				JOptionPane.showMessageDialog(this, "Přihlášení se nezdařilo...",
						"Chybné přihlášení:",
						JOptionPane.WARNING_MESSAGE);
			}
		}
	}

	/**
	 * Po odpojení od CORBA autentizačního systému nastav gui do odpojeného stavu
	 */
	private void setLogouted() {
		loginButton.setEnabled(true);
		username.setEnabled(true);
		pass.setEnabled(true);
		logoutButton.setEnabled(false);
		pass1.setEnabled(false);
		pass1.setText("");
		pass2.setEnabled(false);
		pass2.setText("");
		changePass.setEnabled(false);
		getUsers.setEnabled(false);
		addUser.setEnabled(false);
		delUser.setEnabled(false);
		edUser.setEnabled(false);
		setCookie("");
		ulm.cleanData();
	}
	
	/**
	 * Proveď odhlášení od CORBA autentizačního systému
	 */
	private void logout() {
		try {
			Client.getImpl().logout(cookie);
			setLogouted();
		} catch (UserNotLoggedIn usnli) {
			System.err.println("logout: " + usnli.getMessage());
			userNotLoggedIn();
		} 
	}
	
	/**
	 * Změň heslo uživatele v CORBA autentizačních systému
	 */
	public void changePass() {
		try {
			if (!Client.getImpl().changeCredentials(cookie, username.getText(), 
				"", new String(pass2.getPassword()), "")) {
				JOptionPane.showMessageDialog(this, "Heslo se nepodařilo změnit...",
						"Chyba při změně hesla:",
						JOptionPane.WARNING_MESSAGE);
			}
			pass1.setText("");
			pass2.setText("");
		} catch (NoEnoughPrivileges noe) {
			System.err.println("changePass: " + noe.getMessage());
			noEnoughPrivileges();
		} catch (UserNotLoggedIn unli) {
			userNotLoggedIn();
		}
	}

	/**
	 * Informuj uživatele o tom, že k provedení dané akce nemá dostatečné
	 * oprávnění
	 */
	public void noEnoughPrivileges() {
		JOptionPane.showMessageDialog(this, "Nedostatečná oprávnění k provedení akce...",
				"Nedostatečná oprávnění:",
				JOptionPane.WARNING_MESSAGE);
	}

	/**
	 * Informu uživatel o tom, že není přihlášen a tak nemůže provádět zádné
	 * akce vyžadující přihlášeného uživatele
	 */
	public void userNotLoggedIn() {
		JOptionPane.showMessageDialog(this, "Uživatel nepřihlášen, nelze pokračovat...",
				"Uživatel nepřihlášen:",
				JOptionPane.WARNING_MESSAGE);
		setLogouted();
	}
	
	/**
	 * Stáhni seznam uživatelů vedených v systému
	 */
	public void getUsers() {
		try {
			ulm.setData(Client.getImpl().getUsers(cookie));
			addUser.setEnabled(true);
			delUser.setEnabled(true);
			edUser.setEnabled(true);
		} catch (NoEnoughPrivileges e) {
			System.err.println("getUsers: " + e.getMessage());
			noEnoughPrivileges();
		} catch (UserNotLoggedIn e) {
			System.err.println("getUsers: " + e.getMessage());
			userNotLoggedIn();
		}
	}
	
	/**
	 * Smaž uživatele vedeného v systému
	 */
	public void delUser() {
		int row = users.getSelectedRow();
		if (row > -1) {
			String[] buttons = {"Smaž", "Zruš"};
			
			int rw = JOptionPane.showOptionDialog(this, 
					"Odstranit uživatele: " + ulm.getValueAt(row, 0) + "?",
					"Odranění uživatele",
					JOptionPane.YES_NO_OPTION, 
					JOptionPane.QUESTION_MESSAGE, 
					null, 
					buttons, 
					buttons[1]);
			if (rw == JOptionPane.YES_OPTION) {
				try {
					if (Client.getImpl().removeUser(cookie, 
							(String)ulm.getValueAt(row, 0)) == false) {
						JOptionPane.showMessageDialog(this, "Uživatele " + 
								ulm.getValueAt(row, 0) + " nelze odstranit.",
								"Chyba při odstraňování uživatele",
								JOptionPane.WARNING_MESSAGE);
					}
					getUsers();
				} catch (NoEnoughPrivileges nep) {
					System.err.println("delUser: " + nep.getMessage());
					noEnoughPrivileges();
				} catch (UserNotLoggedIn e) {
					System.err.println("delUser: " + e.getMessage());
					userNotLoggedIn();
				}
			}
		}
	}

	/**
	 * Přidání nového uživatele
	 */
	public void addUser() { new AddEditUser(this).showMe(); }

	/**
	 * Editace uživatele
	 */
	public void edUser() {
		if (users.getSelectedRow() > -1) {
			new AddEditUser(this, ulm.users[users.getSelectedRow()]).showMe();
		}
	}

	/**
	 * Zpracování informace o tom, že byla stiskuna klávesa. Jde o kontrolu
	 * shody hesel a případně podbarvení políčka pro zopakování hesla.
	 */
	public void keyReleased(KeyEvent e) {
		String p1 = new String(pass1.getPassword());
		String p2 = new String(pass2.getPassword());
		if (p1.length() > 0) {
			if (p1.equals(p2)) {
				pass2.setBackground(okBgColor);
				changePass.setEnabled(true);
			} else {
				pass2.setBackground(errBgColor);
				changePass.setEnabled(false);
			}
		}
	}
	
	/**
	 * Získá cookie pro aktuálně přihlášeného uživatele.
	 * @return cookie přihlášeného uživatele
	 */
	public String getCookie() {
		return(cookie);
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(loginButton)) { login(); }
		if (e.getSource().equals(logoutButton)) { logout(); }
		if (e.getSource().equals(changePass)) { changePass(); }
		if (e.getSource().equals(getUsers)) { getUsers(); }
		if (e.getSource().equals(addUser)) { addUser(); }
		if (e.getSource().equals(delUser)) { delUser(); }
		if (e.getSource().equals(edUser)) { edUser(); }
	}
	
	public void keyTyped(KeyEvent e) {}
	public void keyPressed(KeyEvent e) {}
}
