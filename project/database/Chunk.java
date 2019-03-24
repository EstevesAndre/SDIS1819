package project.database;

import java.net.*;
import java.io.*;

public class Chunk {
    private int id;
    private byte[] content;

    public Chunk(int id, byte[] content) {
        this.id = id;
        this.content = content;
    }

    public int getId(){
        return this.id;
    }

    public byte[] getContent() {
        return this.content;
    }
}