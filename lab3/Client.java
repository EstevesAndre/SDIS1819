package lab3;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class Client {

    private Client() {}

    public static void main(String[] args) {

        String host = "localhost"; //(args.length < 1) ? null : args[0];

        try {
            Registry registry = LocateRegistry.getRegistry(host);

            RemoteInterface stub = (RemoteInterface) registry.lookup("RemoteInterface");

            String response = stub.test();

            System.out.println("response: " + response);

        } catch (Exception e) {

            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}