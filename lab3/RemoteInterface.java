package lab3;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {
    String registerUser(String plateNumber, String ownerName) throws RemoteException;

    String getOwner(String plateNumber) throws RemoteException;

    String test() throws RemoteException;
}