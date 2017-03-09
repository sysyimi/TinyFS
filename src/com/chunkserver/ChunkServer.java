package com.chunkserver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
//import java.util.Arrays;

import com.interfaces.ChunkServerInterface;

/**
 * implementation of interfaces at the chunkserver side
 * @author Shahram Ghandeharizadeh
 *
 */

public class ChunkServer implements ChunkServerInterface {
	final static String filePath = "C:\\Users\\sallywei\\Documents\\GitHub\\TinyFS\\csci485Disk"; //"C:\\Users\\shahram\\Documents\\TinyFS-2\\csci485Disk\\";	//or C:\\newfile.txt
	public static long counter;

	private FileOutputStream chunkWriter;
	//private RandomAccessFile chunkReader;
	private String handle, filename;
	private File newChunk, chunkServer;
	private byte[] chunk;
	
	/**
	 * Initialize the chunk server
	 */
	public ChunkServer(){
		/*read all files already existing on the chunk server: can be done with File.listFiles()*/
		chunkServer = new File(filePath);
		chunkServer.mkdirs();
		
		counter = chunkServer.listFiles().length;
	}
	
	/**
	 * Each chunk corresponds to a file.
	 * Return the chunk handle of the last chunk in the file.
	 *
	 * Return chunk handle for a newly created chunk
	 */
	public String createChunk() {
		handle = (counter + 1) + "";
		handle = handle.replaceAll(":+", "");
		newChunk = new File(filePath, handle);
		try {
			newChunk.createNewFile();
			counter++;
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		String success = (newChunk.exists()) ? ": SUCCEEDED" : ": FAILED";
		System.out.println("Attempted to create chunk " + newChunk.getPath() + success);
		return handle;
	}
	
	/**
	 * Write the byte array to the chunk at the specified offset
	 * The byte array size should be no greater than 4KB
	 * 
	 * Overwrite the content of the specified chunk with the provided payload
	 */
	public boolean writeChunk(String ChunkHandle, byte[] payload, int offset) {
		filename = filePath + "\\" + ChunkHandle;
		boolean isSuccess;
		try{
			chunkWriter = new FileOutputStream(filename);
			chunkWriter.write(payload, offset, ChunkSize);
			isSuccess = true;
			//System.out.println("Wrote successfully to chunk " + filename);
		} catch (IOException ioe) {
			isSuccess = false;
			System.out.println("Could not write to chunk " + filename);
		} finally {
			if (chunkWriter != null) {
				try { chunkWriter.close(); }
				catch (IOException e) { e.printStackTrace(); }
			}
		}
		return isSuccess;
	}
	
	/**
	 * read the chunk at the specific offset
	 * 
	 * Reads the specified num of bytes @ the starting offset of a chunk and returns an array of bytes
	 */
	public byte[] readChunk(String ChunkHandle, int offset, int NumberOfBytes) {
		filename = filePath + "\\" + ChunkHandle;
		chunk = new byte[ChunkSize];
		try( RandomAccessFile chunkReader = new RandomAccessFile(filename, "r") ){
			chunkReader.read(chunk, offset, NumberOfBytes);
			//System.out.println("Read successfully from chunk " + filename);
		} catch (IOException ioe) {
			System.out.println("Could not read from chunk " + filename);
			/*if (chunkReader != null) {
				try { chunkReader.close(); }
				catch (IOException e) { e.printStackTrace(); }
			}*/
			return null;
		}
		return chunk;
	}
	
	

}
