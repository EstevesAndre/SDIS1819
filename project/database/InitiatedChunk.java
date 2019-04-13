package project.database;

import java.util.HashSet;

public class InitiatedChunk implements java.io.Serializable{
    private int observedRD;
    private int desiredRD;
    private HashSet<Integer> storers; // id's of the peers that backed up the chunk
    

    public InitiatedChunk(int desiredRD) {
        this.desiredRD = desiredRD;
        observedRD = 0;
        storers =  new HashSet<Integer>();
    }

    public void addStorer(int storer) {
        if(storers.add(storer)) {
            observedRD++;
        }
    }

    public int getObservedRD(){
        return observedRD;
    }

    public int getDesiredRD() {
        return desiredRD;
    }
}