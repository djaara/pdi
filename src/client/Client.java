package client;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;

import PdiRemoteAuth.Auth;
import PdiRemoteAuth.AuthHelper;

/**
 * Připojení k CORBA autentizačnímu serveru, umožňuje přihlášení uživatele,
 * změnu hesla a přidávání, mazání a editaci uživatele
 * @author djaara
 */
public class Client {

	private static Auth impl;
	private static MainWindow mw;
	public static String auth_name = "Auth";

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		mw = new MainWindow();
		mw.setVisible(true);
		
		try {
			// create and initialize the ORB
			ORB orb = ORB.init(args, null);

			// get the root naming context
			org.omg.CORBA.Object objRef = orb
					.resolve_initial_references("NameService");
			// Use NamingContextExt instead of NamingContext. This is
			// part of the Interoperable naming Service.
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);

			// resolve the Object Reference in Naming
			impl = AuthHelper.narrow(ncRef.resolve_str(auth_name));

		} catch (Exception e) {
			System.err.println("main: *****************************************");
			e.printStackTrace();
		}
	}
	
	/**
	 * Dej mi aktuální připojení ke CORBA systému
	 * @return Auth
	 */
	public static Auth getImpl() {
		return(impl);
	}	
}
