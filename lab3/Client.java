package lab3;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private Client() {}
    
    public static void main(String[] args) {

        if(args.length != 4)
		{
            System.out.println("Wrong number of arguments.\nUsage: java lab3/Client <host_name> <remote_object_name> <oper> <opnd>*");
            // example: java lab3/Client localhost RemoteInterface REGISTER "87-UI-64 Andre"
			System.exit(-1);
        }
        try {
            Registry registry = LocateRegistry.getRegistry(args[0]);

            RemoteInterface stub = (RemoteInterface) registry.lookup(args[1]);

            String[] receivedSplited = args[3].split(" ");
            String response = "";

            switch (args[2].toUpperCase()) {
                case "REGISTER":
                    response = stub.registerUser(receivedSplited[0].trim(), receivedSplited[1].trim());
                    break;
                case "LOOKUP":
                    response = stub.getOwner(receivedSplited[0].trim());
                    break;
                default:
                    System.err.println("Server Error: Received unknown request.");
            }

            System.out.println("response: " + response);

        } catch (Exception e) {

            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}