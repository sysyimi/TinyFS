package com.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

import com.chunkserver.ChunkServer;
import com.interfaces.ClientInterface;

/**
 * implementation of interfaces at the client side
 * @author Shahram Ghandeharizadeh
 *
 * ask about making extra Message classes
 *
 */
public class Client implements ClientInterface /*extends Thread*/ {
	private Socket s;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private DataOutputStream dos;
	private DataInputStream dis;

	private static String host = "localhost";
	private static int port = 8888;
	private static int int_size = Integer.SIZE/Byte.SIZE;
	
	private static final boolean DEBUG = false;
	
	/**
	 * Initialize the client
	 */
	public Client(){
		s = null;
		try {
			s = new Socket(host, port);
			oos = new ObjectOutputStream(s.getOutputStream());
			ois = new ObjectInputStream(s.getInputStream());
			dos = new DataOutputStream(oos);
			dis = new DataInputStream(ois);
		} catch (IOException ioe) {
			System.out.println("Connection refused: " + host + ":" + port);
			try {
				if (s != null) s.close();
			} catch (IOException ioe2) {
				System.out.println("IOE when closing connection to " + host + ":" + port);
			}
		}
	}
	
	/**
	 * Create a chunk at the chunk server from the client side.
	 */
	public String createChunk() {
		if (s == null) {
			if (DEBUG) System.out.println("socket is null; fail to create chunk");
			return null;
		}
		try{
			dos.writeChar('c');
			dos.flush();
			oos.flush();
			if (DEBUG) System.out.println("REQUEST: create");
			int length = dis.readInt(); //length of payload
			if (DEBUG) System.out.println("received len " + length);
			byte[] str = new byte[length];
			for (int i = 0; i < length; i++) {
				str[i] = dis.readByte();
			}
			String ans = new String(str, Charset.forName("UTF-8"));
			if (DEBUG) System.out.println("response: [" + length + ": " + ans + "]");
			return ans;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null; //abort
		}
		//return cs.createChunk();
	}
	
	/**
	 * Write a chunk at the chunk server from the client side.
	 */
	public boolean writeChunk(String ChunkHandle, byte[] payload, int offset) {
		if(offset + payload.length > ChunkServer.ChunkSize){
			System.out.println("The chunk write should be within the range of the file, invalide chunk write!");
			return false;
		}
		try{
			byte[] new_payload = appendLengthToPayload(payload);
			dos.writeChar('w');
			dos.flush();
			if (DEBUG) System.out.println("REQUEST: write");
			byte[] chkh = ChunkHandle.getBytes(Charset.forName("UTF-8"));
			dos.writeInt(chkh.length);
			dos.write(chkh, 0, chkh.length);
			dos.write(new_payload, 0, new_payload.length);
			oos.writeInt(offset);
			dos.flush();
			oos.flush();
			//receive response
			boolean ans = dis.readBoolean();
			return ans;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return false; //abort
		}
		//return cs.writeChunk(ChunkHandle, payload, offset);
	}
	
	/**
	 * Read a chunk at the chunk server from the client side.
	 */
	public byte[] readChunk(String ChunkHandle, int offset, int NumberOfBytes) {
		if(NumberOfBytes + offset > ChunkServer.ChunkSize){
			System.out.println("The chunk read should be within the range of the file, invalide chunk read!");
			return null;
		}
		try {
			dos.writeChar('r');
			dos.flush();
			dos.writeInt(ChunkHandle.length());
			dos.writeBytes(ChunkHandle);
			dos.flush();
			oos.flush();
			oos.writeInt(offset);
			oos.writeInt(NumberOfBytes);
			oos.flush();
			//receive response
			int length = dis.readInt();
			byte[] ans = new byte[length];
			int i = 0;
			while (i < length) {
				ans[i] = dis.readByte();
				i++;
			}
			return ans;
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null; //abort bc failed to write all bytes properly
		}
		//return cs.readChunk(ChunkHandle, offset, NumberOfBytes);
	}

	/**
	 * Append the length of the payload (as an int) to the beginning 
	 * of the payload
	 */
	private byte[] appendLengthToPayload(byte[] payload) {
		byte[] plen = ByteBuffer.allocate(int_size).putInt(payload.length).array();
		ByteArrayOutputStream baos = new ByteArrayOutputStream(plen.length + payload.length);
		try {
			baos.write(plen);
			baos.write(payload);
			return baos.toByteArray();
		} catch(IOException ioe) {
			ioe.printStackTrace();
			return null; //error, do nothing (abort)
		}
	}

	private int retrieveLengthFromPayload(byte[] new_payload) {
		byte[] plen = ByteBuffer.allocate(int_size).array();
		for (int i = 0; i < int_size; i++) {
			plen[i] = new_payload[i];
		}
		return ByteBuffer.wrap(plen).getInt();
	}
/*
	public void run() {
		try {
			while (true) {
				// receive responses from chatserver (and funnel them to the right places...?)
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
*/

}
