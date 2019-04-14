package project.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.ArrayList;

public interface RemoteInterface extends Remote {

    public String backupOperation(ArrayList<String> arguments) throws Exception, RemoteException;

    public void backupEnhOperation(ArrayList<String> arguments) throws Exception, RemoteException;

    public String restoreOperation(ArrayList<String> arguments) throws Exception, RemoteException;
    
    public void deleteOperation(ArrayList<String> arguments) throws Exception, RemoteException;

    public void deleteEnhOperation(ArrayList<String> arguments) throws Exception, RemoteException;
    
    public void reclaimOperation(ArrayList<String> arguments) throws Exception, RemoteException;

    public String stateOperation(ArrayList<String> arguments) throws Exception, RemoteException;

}