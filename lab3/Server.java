package lab3;

import java.net.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Timer;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
        
public class Server implements RemoteInterface {
		
	private HashMap<String, String> plateNumbers;

    public Server() {
		this.plateNumbers = new HashMap<String, String>();

	}

	public String getOwner(String plateNumber) {
		String replyMessage;

		if(validatePlateNumber(plateNumber))
		{
			String owner = this.plateNumbers.get(plateNumber);
			if(owner != null) {
				replyMessage = owner;
			}
			else {
				replyMessage = "NOT_FOUND";
			}	
		}
		else 
		{
			replyMessage = "NOT_FOUND";
		}

		return replyMessage;
	}

	public String registerUser(String plateNumber, String ownerName) {

		if(validatePlateNumber(plateNumber) && !this.plateNumbers.containsKey(plateNumber)) {
			this.plateNumbers.put(plateNumber,ownerName);
			return Integer.toString(this.plateNumbers.size());
		}
		else
			return "-1";
	}

	private boolean validatePlateNumber(String platen) {
		Pattern pattern = Pattern.compile("(\\w\\w-){2}\\w{2}");
		Matcher matcher = pattern.matcher(platen);

		return matcher.matches();
	}
        
    public static void main(String args[]) {
		
		if(args.length != 1)
		{
            System.out.println("Wrong number of arguments.\nUsage: java lab3/Server <remote_object_name>");
            //example: java lab3/Server RemoteInterface
			System.exit(-1);
		}
		
        try {
            Server obj = new Server();
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(args[0], stub);

			System.err.println("Server ready");
			
        } catch (Exception e) {

            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}