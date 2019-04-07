package project.database;

import java.util.HashSet;

public class InitiatedChunk {
    private int observedRD;
    private HashSet<Integer> storers; // id's of the peers that backed up the chunk
    

    public InitiatedChunk() {
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

}