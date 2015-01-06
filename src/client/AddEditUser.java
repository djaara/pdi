package client;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import PdiRemoteAuth.NoEnoughPrivileges;
import PdiRemoteAuth.User;
import PdiRemoteAuth.UserNotLoggedIn;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class AddEditUser extends JDialog implements ActionListener, KeyListener {
	
	private static final long serialVersionUID = 6910136932316013366L;

	private JTextField username;
	private JPasswordField pass1;
	private JPasswordField pass2;
	private JTextField rights;
	private JButton okButton;
	private JButton cancelButton;
	private Color okBgColor;
	private Color errBgColor;
	private MainWindow owner;
	private String usernameOrig;
	private boolean newuser = true;
	
	/**
	 * Vytvoření okna pro přidání či editaci uživatele
	 * @param owner kdo je volající aktuálního okna
	 */
	public AddEditUser(MainWindow owner) {
		super(owner);
		this.owner = owner;
		setTitle("Přidej nového uživatele");
		
		init();
		
		FormLayout fm = new FormLayout("3dlu, r:p, 3dlu, p, 3dlu",
				"3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 3dlu, p, 7dlu, p, 3dlu, p, 3dlu");
		PanelBuilder pb = new PanelBuilder(fm);
		CellConstraints cc = new CellConstraints();
		
		pb.addSeparator("Uživatel:", cc.xyw(2, 2, 3));
		pb.addLabel("Uživatelské jméno:", cc.xy(2, 4));
		pb.add(username, cc.xy(4, 4));
		pb.addLabel("Heslo:", cc.xy(2, 6));
		pb.add(pass1, cc.xy(4, 6));
		pb.addLabel("Heslo znovu:", cc.xy(2, 8));
		pb.add(pass2, cc.xy(4, 8));
		pb.addLabel("Práva:", cc.xy(2, 10));
		pb.add(rights, cc.xy(4, 10));
		
		pb.addSeparator("", cc.xyw(2, 12, 3));
		pb.add(cancelButton, cc.xy(2, 14));
		pb.add(okButton, cc.xy(4, 14));
		
		add(pb.getPanel());
		pack();
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		setModal(true);
	}
	
	/**
	 * Vytvoření okna pro přidání či editaci uživatele, zvolen mód editace
	 * uživatele.
	 * @param owner kdo je volající aktuálního okna
	 * @param u uživatel, který bude editován
	 */
	public AddEditUser(MainWindow owner, User u) {
		this(owner);
		
		System.err.println("Edit user: " + u.username);
		username.setText(u.username);
		usernameOrig = u.username;
		pass1.setText(u.password);
		pass2.setText(u.password);
		rights.setText(u.rights);
		newuser = false;
	}
	
	/**
	 * Zobrazení okna.
	 */
	public void showMe() {
		setVisible(true);
	}
	
	/**
	 * Inicializace GUI komponent okna
	 */
	private void init() {
		username = new JTextField(10);
		pass1 = new JPasswordField();
		pass1.addKeyListener(this);
		pass2 = new JPasswordField();
		pass2.addKeyListener(this);
		rights = new JTextField();
		rights.setDocument(new RightsDocument());
		okButton = new JButton("Budiž");
		okButton.addActionListener(this);
		cancelButton = new JButton("Zruš");
		cancelButton.addActionListener(this);
		
		okBgColor = pass2.getBackground();
		errBgColor = new Color(255, 192, 224);
	}
	
	/**
	 * Zavření okna.
	 */
	private void close() {
		setVisible(false);
		dispose();
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(okButton)) {
			try {
				if (newuser) {
					Client.getImpl().addUser(owner.getCookie(), 
							username.getText(), 
							new String(pass1.getPassword()), 
							rights.getText());
				} else {
					Client.getImpl().changeCredentials(owner.getCookie(),
							usernameOrig, 
							username.getText(), 
							new String(pass1.getPassword()), 
							rights.getText());
				}
				owner.getUsers();
				close();
			} catch (UserNotLoggedIn unli) {
				System.err.println("actionPerformed: " + unli.getMessage());
				owner.userNotLoggedIn();
			} catch (NoEnoughPrivileges nep) {
				System.err.println("actionPerformed: " + nep.getMessage());
				owner.noEnoughPrivileges();
			}
		}
		if (e.getSource().equals(cancelButton)) {
			close();
		}
	}

	public void keyPressed(KeyEvent e) {}
	public void keyTyped(KeyEvent e) {}
	
	public void keyReleased(KeyEvent e) {
		String p1 = new String(pass1.getPassword());
		String p2 = new String(pass2.getPassword());
		if (p1.length() > 0) {
			if (p1.equals(p2)) {
				pass2.setBackground(okBgColor);
				okButton.setEnabled(true);
			} else {
				pass2.setBackground(errBgColor);
				okButton.setEnabled(false);
			}
		}
	}
}
