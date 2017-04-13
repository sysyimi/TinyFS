package com.chunkserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Vector;
import java.net.BindException;
import java.net.ServerSocket;
//import java.util.Arrays;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.interfaces.ChunkServerInterface;

/**
 * implementation of interfaces at the chunkserver side
 * @author Shahram Ghandeharizadeh
 * need multiple ServerThreads(ServerSocket)?
 *
 */

public class ChunkServer implements ChunkServerInterface {
	final static String filePath = "csci485/";	//or C:\\newfile.txt
	public static long counter;

	private ServerSocket ss;
	private Socket s;
	private Vector<ChunkServerThread> threads;
	private static int port = 8888;
	private static int int_size = Integer.SIZE/Byte.SIZE;
	
	private static final boolean DEBUG_SERVER = false;
	private static final boolean DEBUG_THREAD = false;
	
	/**
	 * Initialize the chunk server
	 */
	public ChunkServer(){
		File dir = new File(filePath);
		File[] fs = dir.listFiles();

		if(fs.length == 0){
			counter = 0;
		}else{
			long[] cntrs = new long[fs.length];
			for (int j=0; j < cntrs.length; j++)
				cntrs[j] = Long.valueOf( fs[j].getName() ); 
			
			Arrays.sort(cntrs);
			counter = cntrs[cntrs.length - 1];
		}

		//start serversocket
		threads = new Vector<ChunkServerThread>();
		ss = null;
		try {
			ss = new ServerSocket(port);
			if (DEBUG_SERVER) System.out.println("Successfully started server on localhost:" + port);
			while (true) {
				if (DEBUG_SERVER) System.out.println("Waiting for connections...");
				s = ss.accept();
				if (DEBUG_SERVER) System.out.println("Connection from " + s.getInetAddress());
				ChunkServerThread cst = new ChunkServerThread(s, this);
				threads.add(cst);
			}
		} catch (BindException be) {
			if (DEBUG_SERVER) System.out.println("A server is already running on this port");
		} catch (IOException ioe) {
			if (DEBUG_SERVER) System.out.println("Exception while accepting connections on server");
			ioe.printStackTrace();
		} finally {
			if (ss != null) {
				try { ss.close(); }
				catch (IOException ioe) { if (DEBUG_SERVER)System.out.println("Exception while closing server"); }
			}
		}
	}
	
	/**
	 * Each chunk is corresponding to a file.
	 * Return the chunk handle of the last chunk in the file.
	 */
	public String createChunk() {
		counter++;
		return String.valueOf(counter);
	}
	
	/**
	 * Write the byte array to the chunk at the offset
	 * The byte array size should be no greater than 4KB
	 */
	public boolean writeChunk(String ChunkHandle, byte[] payload, int offset) {
		try {
			//If the file corresponding to ChunkHandle does not exist then create it before writing into it
			RandomAccessFile raf = new RandomAccessFile(filePath + ChunkHandle, "rw");
			raf.seek(offset);
			raf.write(payload, 0, payload.length);
			raf.close();
			return true;
		} catch (IOException ex) {
			ex.printStackTrace();
			return false;
		}
	}
	
	/**
	 * read the chunk at the specific offset
	 * include the length to be read in the payload itself (1st 4 bytes)
	 */
	public byte[] readChunk(String ChunkHandle, int offset, int NumberOfBytes) {
		try {
			//If the file for the chunk does not exist the return null
			boolean exists = (new File(filePath + ChunkHandle)).exists();
			if (exists == false) return null;
			
			//File for the chunk exists then go ahead and read it
			byte[] data = new byte[NumberOfBytes];
			RandomAccessFile raf = new RandomAccessFile(filePath + ChunkHandle, "rw");
			raf.seek(offset);
			raf.read(data, 0, NumberOfBytes);
			raf.close();
			return data;
		} catch (IOException ex){
			ex.printStackTrace();
			return null;
		}
	}
	
	private class ChunkServerThread extends Thread{
		private ChunkServer server;
		private Socket s;
		private ObjectInputStream ois;
		private ObjectOutputStream oos;
		private DataInputStream dis;
		private DataOutputStream dos;
		
		public ChunkServerThread(Socket s, ChunkServer server) {
			this.s = s;
			this.server = server;
			try{
				ois = new ObjectInputStream(s.getInputStream());
				oos = new ObjectOutputStream(s.getOutputStream());
				dis = new DataInputStream(ois);
				dos = new DataOutputStream(oos);
				this.start();
			} catch (IOException ioe) {
				if (DEBUG_THREAD) System.out.println("Error while creating thread");
				ioe.printStackTrace();
			}
		}
		
		public void run() {
			if(ss == null || s == null) return;
			char cmd = 0;
			while (true) {
				try {
					cmd = dis.readChar();
					switch(cmd){
						case('c'):
							if (DEBUG_THREAD) System.out.println("received createChunk request");
							String ret_payload_c = createChunk();
							byte[] payload_c = ret_payload_c.getBytes(Charset.forName("UTF-8"));
							dos.writeInt(payload_c.length);
							dos.write(payload_c, 0, payload_c.length);
							dos.flush();
							oos.flush();
							if (DEBUG_THREAD) System.out.println("payload [" + ret_payload_c + "][" + ret_payload_c.length() + "]");
							break;
						case('r'):
							if (DEBUG_THREAD) System.out.println("received read request");
							String r_handle = readChunkhandle();
							int r_offset = dis.readInt();
							int numBytes = dis.readInt();
							byte[] ret_payload_r = readChunk(r_handle, r_offset, numBytes);
							dos.writeInt(ret_payload_r.length);
							oos.write(ret_payload_r, 0, ret_payload_r.length);
							dos.flush();
							oos.flush();
							break;
						case('w'):
							if (DEBUG_THREAD) System.out.println("received write request");
							String w_handle = readChunkhandle();
							int w_length = dis.readInt();
							byte[] data = new byte[w_length];
							for (int k = 0; k < w_length; k++) {
								data[k] = dis.readByte();
							}
							int w_offset = dis.readInt();
							boolean ret_payload = writeChunk(w_handle, data, w_offset);
							dos.writeBoolean(ret_payload);
							dos.flush();
							oos.flush();
							break;
						default:
							if (DEBUG_THREAD) System.out.println("received other request: [ " + cmd + " ]");
							break;
					} /*end switch(cmd)*/
				} catch (IOException ioe) {
					try {
						dos.close(); dis.close(); 
						if (DEBUG_THREAD) System.out.println("Closed dos/ios");
					} catch (IOException ioe2) {
						if (DEBUG_THREAD) System.out.println("error closing dos/ios in this thread");
					}
					break;
				}
			} /*endwhile*/
		}

		private String readChunkhandle() {
			try{
				int hlen = dis.readInt();
				if (DEBUG_THREAD) System.out.println("chunkhandle len: " + hlen);
				byte[] chkh = new byte[hlen];
				for (int j = 0; j < hlen; j++) {
					chkh[j] = dis.readByte();
				}
				String r_handle = new String(chkh, Charset.forName("UTF-8"));
				if (DEBUG_THREAD) System.out.println("chunkhandle " + r_handle);
				return r_handle;
			} catch (IOException ioe) {
				return null;
			}
		}
	}
	
	public static void main(String[] args){
		ChunkServer cs = new ChunkServer();
	}
}
