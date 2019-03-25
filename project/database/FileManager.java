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

                //String filePartName = String.format("%s.%03d", fileName, partCounter++);
                //File newFile = new File(file.getParent(), filePartName);
                //try (FileOutputStream out = new FileOutputStream(newFile)) {
                    //out.write(buffer, 0, bytesAmount);
                //}
            }
        }

        return chunks;
    }
    
}