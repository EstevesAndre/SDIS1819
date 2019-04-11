package project.database;

import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.AbstractMap;
import java.util.Map;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileOutputStream;
import project.database.Chunk;

public class FileManager implements java.io.Serializable {

    private static final int MAX_CHUNK_SIZE = 64000;

    private String fileID;
    private String path;
    private int rd;
    private int chunkNr;
    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public FileManager(String fileID, String path, int rd) {
        this.fileID = fileID;
        this.path = path;
        this.rd = rd;     
    }

    public FileManager(String fileID, String path, int rd, int chunkNr) {
        this.fileID = fileID;
        this.path = path;
        this.rd = rd;
        this.chunkNr = chunkNr;
    }

    public String getFileID() {
        return this.fileID;
    }
   
    public String getPath() {
        return path;
    }

    public int getDRD() {
        return rd;
    }

    public int getChunkNr() {
        return chunkNr;
    }
    
    public static ArrayList<Chunk> splitFile(String path) throws IOException {
        File file = new File(path);

        System.out.println(path);
        int partCounter = 0;
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();

        byte[] buffer = new byte[MAX_CHUNK_SIZE];

        //String fileName = file.getName();

        //try-with-resources to ensure closing stream
        try (FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis)) {

            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {
                chunks.add(new Chunk(partCounter, buffer, bytesAmount));
                partCounter++;
            }
        }
        catch(Exception e)
        {
            System.err.println("WARNING --> : File: \"" + path + "\" not found!\n");
        }
        
        return chunks;
    }
 
    public static void restoreFile(String peerID, String fileID, ConcurrentHashMap<Map.Entry<String,Integer>, byte[]> chunks) throws IOException {

        String path = peerID + "/restored/file.txt"; 

        File file = new File(path);

        try {
            if(!file.exists())
            {
                file.getParentFile().mkdirs();
                file.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(file, true);
            
            for(int chunkID = 0; chunkID < chunks.size(); chunkID++)
            {
                AbstractMap.SimpleEntry<String, Integer> chunk = new AbstractMap.SimpleEntry<String, Integer>(fileID, chunkID);
                byte[] part = chunks.get(chunk);

                fos.write(part);
            }

            fos.close();

        } catch(Exception e)
        {
            e.printStackTrace();
            System.err.println("Error while restoring File\n");
        }
    }
    
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}