package server;

import org.omg.CORBA.ORB;
import org.omg.CosNaming.NameComponent;
import org.omg.CosNaming.NamingContextExt;
import org.omg.CosNaming.NamingContextExtHelper;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;

import PdiRemoteAuth.Auth;
import PdiRemoteAuth.AuthHelper;

public class Server {

	public static String rootpoa_name = "RootPOA";
	public static String nameservice_name = "NameService";
	public static String auth_name = "Auth";
	
	public static void main(String[] args) {			
		
		try {
			// vytvoření a inicializace ORB
			ORB orb = ORB.init(args, null);
			
			// získání reference na RootPOA a jeho aktivování
			POA rootpoa = POAHelper.narrow(
					orb.resolve_initial_references(rootpoa_name));
			rootpoa.the_POAManager().activate();
			
			// vytvoření objektu pro zpracovaní autentizačních požadavků
			AuthImpl impl = new AuthImpl();
			// získání reference na objekt od sluhy
		    org.omg.CORBA.Object ref = rootpoa.servant_to_reference(impl);
		    Auth href = AuthHelper.narrow(ref);
			
			// získání služby NameSerivce
			org.omg.CORBA.Object objRef = orb.resolve_initial_references(nameservice_name);
			
			// Use NamingContextExt which is part of the Interoperable
		    // Naming Service (INS) specification.
			NamingContextExt ncRef = NamingContextExtHelper.narrow(objRef);
			
			// bind the Object Reference in Naming
		    NameComponent path[] = ncRef.to_name(auth_name);
		    ncRef.rebind(path, href);

		    System.out.println("Auth server ready and waiting ...");

		    orb.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
