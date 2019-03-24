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


    public FileManager(String id, String path, int rd) {
        this.id = id;
        this.path = path;
        this.rd = rd;
    }

    public ArrayList<Chunk> splitFile() throws IOException {
        File file = new File(this.path);

        int partCounter = 1;
        ArrayList<Chunk> chunks = new ArrayList<Chunk>();

        int sizeOfFiles = 1000 * 64;// 64KB
        byte[] buffer = new byte[sizeOfFiles];

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