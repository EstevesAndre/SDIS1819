package project.service;

import project.rmi.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;

public class TestApp {

    // Operation to be executed (Backup, Restore, Delete)
    private String operation;
    private ArrayList<String> operationArgs;
    
    private RemoteInterface RMIStub;

    public TestApp(String args[]) throws Exception {
        
        System.setProperty("java.net.preferIPv4Stack", "true");

        String[] accessPoint = args[0].split(" ");

        try {
            Registry registry = LocateRegistry.getRegistry(accessPoint[0]);
            RMIStub = (RemoteInterface) registry.lookup(accessPoint[1]);
        } 
        catch(Exception e) {
            System.err.println("ERROR --> " + this.getClass().toString() + ": Failed to initiate RMI with RemoteInterface Interface\n");
            System.exit(-2);
        }

        this.operation = args[1];
        this.operationArgs = new ArrayList<>();

        for(int i = 2; i < args.length; i++) {
            this.operationArgs.add(args[i]);
        }
    }

    // Usage: java project/service/TestApp <peer_ap> <sub_protocol> <opnd_1> <opnd_2> 
    // Example: java project/service/TestApp "localhost RemoteInterface" BACKUP ./file_test1.txt 2
    // Example: java project/service/TestApp "localhost RemoteInterface" DELETE ./file_test1.txt
    // Example: java project/service/TestApp "localhost RemoteInterface" RESTORE file_test1.txt
    // Example: java project/service/TestApp "localhost RemoteInterface2" RECLAIM 0
    public static void main(String args[]) throws Exception {

        if(args.length != 2 && args.length != 3 && args.length != 4)
		{
            System.err.println("Wrong number of arguments.\nUsage: java project/service/TestApp <peer_ap> <sub_protocol> <opnd_1>? <opnd_2>?\n");
            System.exit(-1);
        }
        
        TestApp testApp = new TestApp(args);

        testApp.invokeRequest();
    }

    private void invokeRequest() throws Exception {
        String info = null;

        try {
            switch(this.operation) {
                case "BACKUP":
                    info = this.RMIStub.backupOperation(this.operationArgs);
                break;
                case "BACKUPENH":
                    this.RMIStub.backupEnhOperation(this.operationArgs);
                break;
                case "RESTORE":
                    info = this.RMIStub.restoreOperation(this.operationArgs);
                break;
                case "DELETE":
                    this.RMIStub.deleteOperation(this.operationArgs);
                break;
                case "RECLAIM":
                    this.RMIStub.reclaimOperation(this.operationArgs);
                break;
                case "STATE":
                    info = this.RMIStub.stateOperation(this.operationArgs);
                break;
                default:
                    System.err.println("Wrong operation to make the request.\n" + 
                        "Make sure you use of the following requests:"
                            + "\n- BACKUP"
                            + "\n- RESTORE"
                            + "\n- DELETE"
                            + "\n- RECLAIM"
                            + "\n- STATE"
                            + "\r\n");
                    return;
            }
        }
        catch (java.rmi.RemoteException e) {
            System.err.println("ERROR --> " + this.getClass() + ": Unable to make request using RMI");
            System.exit(-2);
        }
        catch(Exception e)
        {
            System.err.println("ERROR --> " + this.getClass() + ": File not found!");
            System.exit(-2);
        }

        System.out.println("Request sent!\r\n");
        if(info != null) {
            System.out.println("Response:\n" + info);
        }
    }
}