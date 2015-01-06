package server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.Vector;

import PdiRemoteAuth.AuthPOA;
import PdiRemoteAuth.NoEnoughPrivileges;
import PdiRemoteAuth.UnknownUserOrBadPass;
import PdiRemoteAuth.User;
import PdiRemoteAuth.UserNotLoggedIn;

class AuthImpl extends AuthPOA {

	private static final String db_driver_name = "org.apache.derby.jdbc.EmbeddedDriver";
	private static final String db_data_dir_property = "derby.system.home";
	private static final String db_connection_string = "jdbc:derby:Auth";
	private static final String db_create_append = ";create=true";
	private static final String db_data_dir = System.getProperty("user.home") + "/.PDI";
	private static final String db_create_table_auth =
		"CREATE TABLE auth (id		INTEGER NOT NULL" +
		"							PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
		"					login	VARCHAR(256) NOT NULL UNIQUE," +
		"					pass	VARCHAR(32) NOT NULL," +
		"					rights	VARCHAR(32) NOT NULL)";
	private static final String db_create_table_cookie =
		"CREATE TABLE cookie (id	INTEGER NOT NULL" +
		"							PRIMARY KEY GENERATED ALWAYS AS IDENTITY," +
		"					  up	INTEGER NOT NULL REFERENCES auth(id)," +
		"					  cs	VARCHAR(256) NOT NULL UNIQUE," +
		"					  valid BIGINT NOT NULL)";
	private static final String db_create_administrator =
		"INSERT INTO auth (login, pass, rights) VALUES ('administrator', '12345', 'A')";
	private static final String db_create_test_user =
		"INSERT INTO auth (login, pass, rights) VALUES ('test', 'test', 'U')";
	
	private static final String db_login_sql =
		"SELECT * FROM auth WHERE login = ? AND pass = ?";
	private static final String db_cookie_new_sql =
		"INSERT INTO cookie (cs, up, valid) VALUES (?, ?, ?)";
	private static final String db_cookie_update_sql =
		"UPDATE cookie SET valid = ? WHERE id = ?";
	private static final int cookie_timeout = 300;
	private static final String db_cookie_validity = 
		"SELECT * FROM cookie WHERE cs = ? AND valid > ?";
	private static final String db_search_cookie =
		"SELECT id, cs FROM cookie WHERE up = ? AND valid > ?";
	private static final String db_auth_rights =
		"SELECT rights FROM auth WHERE id = ?";
	private static final String db_auth_user_update =
		"UPDATE auth SET pass = ? WHERE id = ? AND login = ?";
	private static final String db_auth_admin_update = 
		"UPDATE auth SET pass = ?, login = ?, rights = ? WHERE login = ?";
	private static final String db_add_new_user = 
		"INSERT INTO auth (login, pass, rights) VALUES (?, ?, ?)";
	private static final String db_cookie_delete_old =
		"DELETE FROM cookie WHERE valid < ?";
	private static final String db_get_users_admin =
		"SELECT * FROM auth ORDER BY login";
	private static final String db_logout_user =
		"DELETE FROM cookie WHERE cs = ?";
	private static final String db_remove_user =
		"DELETE FROM auth WHERE login = ?";
	
	private static final String[] create_db = {db_create_table_auth,
											db_create_table_cookie,
											db_create_test_user,
											db_create_administrator};
	
	
	
	Connection sqlconnection;
	PreparedStatement login_ps;
	PreparedStatement cookie_new_ps;
	PreparedStatement cookie_update_ps;
	PreparedStatement cookie_validity_ps;
	PreparedStatement cookie_search_ps;
	PreparedStatement cookie_delete_old;
	PreparedStatement auth_rights_ps;
	PreparedStatement auth_update_user_ps;
	PreparedStatement auth_update_admin_ps;
	PreparedStatement auth_new_user_ps;
	PreparedStatement get_users_ps;
	PreparedStatement logout_ps;
	PreparedStatement remove_ps;
	Random random;

	/**
	 * Připojení k databázi v módu vytvoření databáze. Dojde k vytvoření
	 * databáze a založení všech potřebných tabulek.
	 * @throws SQLException
	 */
	private void createDB() throws SQLException {
		sqlconnection = DriverManager.getConnection(db_connection_string + 
				db_create_append);

		// Založení všech potřebných tabulek
		for (String sql : create_db) {
			System.err.println(sql);
			sqlconnection.createStatement().execute(sql);
		}
	}
	
	/**
	 * Nastavení datového adresáře pro databázi, připojení k databázi.
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	private void connectDB() throws ClassNotFoundException, SQLException {
		// nastavení datového adresáře
		System.setProperty(db_data_dir_property, db_data_dir);
		System.err.println("db data directory: "
				+ System.getProperty(db_data_dir_property));

		// načtení ovladače databáze
		Class.forName(db_driver_name);
		System.err.println("db driver loaded...");
		
		// vytvoření připojení k databázi
		try {
			sqlconnection = DriverManager.getConnection(db_connection_string);
		} catch (SQLException slqe) {
			createDB();
		} finally {
			System.err.println("db connected...");
		}
	}
	
	/**
	 * Vytvoří prepared statement pro komunikaci s databází.
	 * @param ps	prepared statement pro uložení
	 * @param sql	sql dotaz
	 * @throws SQLException
	 */
	private PreparedStatement createPreparedStatement(String sql)
		throws SQLException {
		System.err.println("Creating prepared statement: " + sql);
		return(sqlconnection.prepareStatement(sql));
	}
	
	/**
	 * Konstruktor třídy AuthImpl, zajišťuje potřebné datové připojení.
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public AuthImpl() throws ClassNotFoundException, SQLException {
		random = new Random();
		connectDB();
		login_ps = createPreparedStatement(db_login_sql);
		cookie_new_ps = createPreparedStatement(db_cookie_new_sql);
		cookie_update_ps = createPreparedStatement(db_cookie_update_sql);
		cookie_validity_ps = createPreparedStatement(db_cookie_validity);
		cookie_search_ps = createPreparedStatement(db_search_cookie);
		auth_rights_ps = createPreparedStatement(db_auth_rights);
		auth_update_user_ps = createPreparedStatement(db_auth_user_update);
		auth_update_admin_ps = createPreparedStatement(db_auth_admin_update);
		auth_new_user_ps = createPreparedStatement(db_add_new_user);
		cookie_delete_old = createPreparedStatement(db_cookie_delete_old);
		get_users_ps = createPreparedStatement(db_get_users_admin);
		logout_ps = createPreparedStatement(db_logout_user);
		remove_ps = createPreparedStatement(db_remove_user);
	}

	/**
	 * Vygenerování sušenky
	 * @return
	 */
	private String generateCookie() {
		byte[] rand = new byte[128];
		random.nextBytes(rand);
		StringBuilder sb = new StringBuilder();
		for (byte b : rand) {
			sb.append(String.format("%02x", b));
		}
		return(sb.toString());
	}
	
	/**
	 * Pro zadané id uživatele vrátí seznam oprávnění.
	 * @param id id uživatele
	 * @return seznam oprávnění uživatele pokud je uživatel nalezen, jinal null
	 * @throws SQLException
	 */
	private String getRights(int id) throws SQLException {
		System.err.print("  Getting rights for id(" + id + "): ");
		auth_rights_ps.setInt(1, id);
		ResultSet rs = auth_rights_ps.executeQuery();
		String rights = null;
		if (rs.next()) {
			rights = rs.getString("rights");
		}
		System.err.println(rights);
		rs.close();
		return(rights);
	}
	
	/**
	 * Přidání uživatele.
	 * @param cookie sušenka pro ověření uživatele
	 * @param username uživatelské jméno nového uživatele
	 * @param pass heslo nového uživatele
	 * @throws NoEnoughPrivileges
	 */
	public boolean addUser(String cookie, String username, String pass,
			String newrights)
			throws NoEnoughPrivileges {
		System.err.println("Add user:");
		
		int id;
		try {
			if (( id = checkCookie(cookie))> -1) {
				String rights = getRights(id);
				if (rights != null && rights.contains("A")) {
					auth_new_user_ps.setString(1, username);
					auth_new_user_ps.setString(2, pass);
					auth_new_user_ps.setString(3, newrights);
					System.err.println( "  Add new user row count: " + 
							auth_new_user_ps.executeUpdate());
					return(true);
				} else {
					throw new NoEnoughPrivileges("Only administrator can add " +
							"new user!");
				}
			}
		} catch (SQLException sqle) {
			if (sqle.getClass().toString().
					contains("SQLIntegrityConstraintViolationException")) {
				System.err.println("  User \"" + username + "\" allready exists!");
				throw new NoEnoughPrivileges("User " + username +
						" allready exists");
			} else {
				sqle.printStackTrace();
			}
		}
		return(false);
	}

	/**
	 * Ověření platnosti autentizační cookie
	 * @param cookie cookie, jejíž platnost má být ověřena
	 * @return	id uživatele, kterému cookie patří pro platnou cookie,
	 * 			-1 pro neplatnou cookie
	 * @throws SQLException
	 */
	private int checkCookie(String cookie) throws SQLException {
		System.err.println("  Checking cookie...");
		long currentTime = System.currentTimeMillis()/1000;
		cookie_validity_ps.setString(1, cookie);
		cookie_validity_ps.setLong(2, currentTime);
		ResultSet rs = cookie_validity_ps.executeQuery();
		if (rs.next()) {
			updateCookie(rs.getInt("id"));
			cookie_delete_old.setLong(1, currentTime);
			System.err.println("  Deleting old cookies(" + 
					cookie_delete_old.executeUpdate()+")");
			int up = rs.getInt("up");
			rs.close();
			return(up);
		}
		return(-1);
	}
	
	/**
	 * Změna uživatelského hesla (jména).
	 * @param cookie identifikace přihlášeného uživatele
	 * @param user uživatelské jméno
	 * @param newuser nové uživatelské jméno
	 * @param newpass nové uživatelské heslo
	 * @throws NoEnoughtPrivileges
	 */
	public boolean changeCredentials(String cookie, String user,
			String newuser, String newpass, String newrights) 
			throws NoEnoughPrivileges, UserNotLoggedIn {
		System.err.println("Change Credentials:");

		try {
			int id;
			if ((id = checkCookie(cookie)) > -1) {
				String rights = getRights(id);
				if (rights != null){
					if (rights.contains("A")) {
						System.err.print("  Admin mode: ");
						if (newuser.length() == 0 || newrights.length() == 0) {
							auth_update_user_ps.setString(1, newpass);
							auth_update_user_ps.setInt(2, id);
							auth_update_user_ps.setString(3, user);
							System.err.println("Updated count (user): " + 
									auth_update_user_ps.executeUpdate());
						} else {
							auth_update_admin_ps.setString(1, newpass);
							auth_update_admin_ps.setString(2, newuser);
							auth_update_admin_ps.setString(3, newrights);
							auth_update_admin_ps.setString(4, user);
							System.err.println("Updated count (admin): " + 
								auth_update_admin_ps.executeUpdate());
						}
						return(true);
					} else if (rights.contains("U")) {
						System.err.print("  User mode: ");
						if (newuser.length() > 0) {
							System.err.println("User cannot change his username!");
							throw new NoEnoughPrivileges("User cannot change " +
									"his username...");
						}
						if (newrights.length() > 0) {
							System.err.println("User cannot change his rights!");
							throw new NoEnoughPrivileges("User cannot change " +
									"his rights...");
						}
						auth_update_user_ps.setString(1, newpass);
						auth_update_user_ps.setInt(2, id);
						auth_update_user_ps.setString(3, user);
						System.err.println("Updated count (user): " + 
								auth_update_user_ps.executeUpdate());
						return(true);
					}
				}
			} else {
				throw new UserNotLoggedIn("User_not_logged_in");
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			return(false);
		}
		return(false);
	}

	/**
	 * Získání seznamu uživatelů.
	 * @param cookie identifikace klienta, který se snaží získat seznam uživatelů.
	 * @throws NoEnoughPrivileges
	 * @return User[] seznam uživatelů
	 */
	public User[] getUsers(String cookie) throws NoEnoughPrivileges,
			UserNotLoggedIn {
		System.err.println("Get users:");
		try {
			int id;
			if ((id = checkCookie(cookie)) > - 1) {
				if (getRights(id).contains("A")) {
					ResultSet rs = get_users_ps.executeQuery();
					Vector<User> users = new Vector<User>();
					while (rs.next()) {
						User u = new User(rs.getString("login"), 
										  rs.getString("pass"),
										  rs.getString("rights"));
						users.add(u);
					}
					User[] a = new User[1];
					rs.close();
					return(users.toArray(a));
				} else {
					System.err.println("  Only admin can list users...");
					throw new NoEnoughPrivileges("Only admin can list users...");
				}
			} else {
				throw new UserNotLoggedIn("User_not_logged_in");
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			throw new NoEnoughPrivileges("Data beckend error...");
		}
	}

	/**
	 * Nastaví nový timeout pro danou cookie.
	 * @param id
	 * @throws SQLException
	 */
	public void updateCookie(int id) throws SQLException {
		System.err.println("  Updating cookie("+id+")");
		cookie_update_ps.setInt(1, id);
		cookie_update_ps.setLong(2, (System.currentTimeMillis()/1000) + cookie_timeout);
		cookie_update_ps.execute();
	}
	
	/**
	 * Pokus o autentizaci vůči serveru.
	 * @param username uživatelské jméno
	 * @param password heslo uživatele
	 * @throws UnknownUserOrBadPass
	 */
	public String tryAuth(String username, String password)
			throws UnknownUserOrBadPass {
		System.err.println("Try auth:");
		String cookie;
		try {
			login_ps.setString(1, username);
			login_ps.setString(2, password);
			ResultSet rs = login_ps.executeQuery();
			if (rs.next()) {
//				ResultSet ll = sqlconnection.createStatement().executeQuery("SELECT * FROM cookie");
//				while (ll.next()) {
//					System.err.println(ll.getInt("id") + " " + ll.getString("cs") + " " + ll.getLong("valid"));
//				}
				cookie_search_ps.setInt(1, rs.getInt("id"));
				cookie_search_ps.setLong(2, System.currentTimeMillis()/1000);
				ResultSet cs = cookie_search_ps.executeQuery();
				if (cs.next()) {
					cookie = cs.getString("cs");
					updateCookie(cs.getInt("id"));
				} else {
					cookie = generateCookie();
					cookie_new_ps.setString(1, cookie);
					cookie_new_ps.setInt(2, rs.getInt("id"));
					cookie_new_ps.setLong(3, (System.currentTimeMillis()/1000) + 
							cookie_timeout);
					cookie_new_ps.execute();
				}
				cs.close();
				rs.close();
			} else {
				rs.close();
				throw new UnknownUserOrBadPass("Wrong username or password!");
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
			throw new UnknownUserOrBadPass("Data backend error!");
		}
		return cookie;
	}

	/**
	 * Odhlášení uživatele
	 * @param cookie identifikace uživatele
	 * @throws UserNotLoggedIn
	 * @return boolean true, pokud se odhlášení podařilo v pořádku, jinak false
	 */
	public boolean logout(String cookie) throws UserNotLoggedIn {
		System.err.println("Logout:");
		try {
			if (checkCookie(cookie) > -1) {
				logout_ps.setString(1, cookie);
				if (logout_ps.executeUpdate() != 1) {
					System.err.println("  Delete cookie failed!");
					throw new UserNotLoggedIn("Delete cookie failed...");
				} else {
					System.err.println("  OK");
					return(true);
				}
			} else {
				System.err.println("  Auth cookie not found!");
				throw new UserNotLoggedIn("Auth cookie not found!");
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		return(false);
	}

	/**
	 * Odstranění uživatele
	 * @param cookie identifikace uživatele
	 * @param username jméno uživatele k odstranění
	 * @return true pokud se odstranění uživatele zdařilo, jinak false
	 * @throws NoEnoughPrivileges
	 */
	public boolean removeUser(String cookie, String username) throws NoEnoughPrivileges {
		System.err.println("Remove user:");
		try {
			int id;
			if ((id = checkCookie(cookie)) > -1) {
				if (getRights(id).contains("A")) {
					remove_ps.setString(1, username);
					if (remove_ps.executeUpdate() == 1) {
						return(true);
					} else {
						return(false);
					}
				} else {
					System.err.println("  Only admin can remove user!");
					throw new NoEnoughPrivileges("Only admin can remove user...");
				}
			}
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		return(false);
	}
}