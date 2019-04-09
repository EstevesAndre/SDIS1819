package project.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.ArrayList;

public interface RemoteInterface extends Remote {

    public void backupOperation(ArrayList<String> arguments) throws Exception, RemoteException;

    public void restoreOperation(ArrayList<String> arguments) throws Exception, RemoteException;
    
    public void deleteOperation(ArrayList<String> arguments) throws Exception, RemoteException;
    
    public void reclaimOperation(ArrayList<String> arguments) throws Exception, RemoteException;

    public void stateOperation(ArrayList<String> arguments) throws Exception, RemoteException;

}