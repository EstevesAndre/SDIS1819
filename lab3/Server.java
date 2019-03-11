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
		String replyMessage;

		if(validatePlateNumber(plateNumber) && !this.plateNumbers.containsKey(plateNumber)) {
			this.plateNumbers.put(plateNumber,ownerName);
			return replyMessage = Integer.toString(this.plateNumbers.size());
		}
		else
			return replyMessage = "-1";
	}

	public String test() {
		return "This is a test.";
	}

	private boolean validatePlateNumber(String platen) {
		Pattern pattern = Pattern.compile("(\\w\\w-){2}\\w{2}");
		Matcher matcher = pattern.matcher(platen);

		return matcher.matches();
	}
        
    public static void main(String args[]) {
        
        try {
            Server obj = new Server();
            RemoteInterface stub = (RemoteInterface) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry();
            registry.bind("RemoteInterface", stub);

			System.err.println("Server ready");
			
        } catch (Exception e) {

            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}