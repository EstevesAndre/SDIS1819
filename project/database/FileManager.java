package project.database;

import java.net.*;
import java.util.ArrayList;
import java.io.*;
import project.database.Chunk;

public class FileManager {

    private static final int MAX_CHUNK_SIZE = 64000;

    private String id;
    private String path;
    private int rd;
    private int numberOfChunks;

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public FileManager(String id, String path, int rd) {
        this.id = id;
        this.path = path;
        this.rd = rd;
    }

    public static ArrayList<Chunk> splitFile(String path) throws IOException {
        File file = new File(path);

        int partCounter = 0;
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();

        byte[] buffer = new byte[MAX_CHUNK_SIZE];

        //String fileName = file.getName();

        //try-with-resources to ensure closing stream
        try (FileInputStream fis = new FileInputStream(file);
        BufferedInputStream bis = new BufferedInputStream(fis)) {

            int bytesAmount = 0;
            while ((bytesAmount = bis.read(buffer)) > 0) {

                chunks.add(new Chunk(partCounter, buffer));
                partCounter++;
            }
        }
        catch(Exception e)
        {
            System.err.println("WARNING --> : File: \"" + path + "\" not found!\n");
        }

        return chunks;
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