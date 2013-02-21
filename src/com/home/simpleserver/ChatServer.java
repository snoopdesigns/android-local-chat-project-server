package com.home.simpleserver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;

public class ChatServer {
	public static final String TAG = "ChatServer";
    private DatagramSocket serverSock = null;
    private MessageParser parser = new MessageParser();
    private ServerController controller;
    private String key="";
    private String iv="";
    ObjectCrypter crypter;
    public ChatServer(int serverPort,ServerController cnt,String key,String iv) {
        this.controller = cnt;
        this.key = key;
        this.iv = iv;
        crypter = new ObjectCrypter(key.getBytes(),iv.getBytes());
        try {
            serverSock = new DatagramSocket(serverPort);
        }
        catch (IOException e){
            e.printStackTrace(System.err);
        }
    }
    public void sendUnencryptedMessage(String toSend,InetAddress address,int port)
    {
        try {
        	Log.info(TAG, "message write:"+toSend);
        	byte[] toSendByte = toSend.getBytes("UTF-8");
			serverSock.send(new DatagramPacket(toSendByte,toSendByte.length,address,port));
		} catch (IOException e) {
			Log.error(TAG,"Server:Exception writing to socket, e="+e);
			e.printStackTrace();
		}
    }
    public void sendMessage(String toSend,InetAddress address,int port)
    {
        try {
        	Log.info(TAG, "message write:"+toSend);
        	byte[] toSendByte = crypter.Encrypt(toSend);//toSend.getBytes("UTF-8");
			serverSock.send(new DatagramPacket(toSendByte,toSendByte.length,address,port));
		} catch (IOException e) {
			Log.error(TAG,"Server:Exception writing to socket, e="+e);
			e.printStackTrace();
		}
    }
    private static Object resizeArray (Object oldArray, int newSize) {
	    int oldSize = java.lang.reflect.Array.getLength(oldArray);
	    Class elementType = oldArray.getClass().getComponentType();
	    Object newArray = java.lang.reflect.Array.newInstance(elementType, newSize);
	    int preserveLength = Math.min(oldSize, newSize);
	    if (preserveLength > 0)
	    	System.arraycopy(oldArray, 0, newArray, 0, preserveLength);
	    return newArray; 
    }
    public void waitForConnections() {
        while (true) {
            try {
            	DatagramPacket packet = new DatagramPacket(new byte[65536],65536);
            	Log.info(TAG, "Waiting for new message..");
            	serverSock.receive(packet);
            	String dbgMessage = new String(packet.getData());
            	if(dbgMessage.contains("dbgclient"))
            	{
            		tMessage mes = parser.parseMessage(dbgMessage);
            		this.controller.handleMessage(mes, packet.getAddress(), packet.getPort());
            		continue;
            	}
            	byte[] byteData = (byte[])resizeArray(packet.getData(),packet.getLength());
                tMessage mes = parser.parseMessage(crypter.Decrypt(byteData));
                Log.info(TAG,"Received data="+crypter.Decrypt(byteData));
                this.controller.handleMessage(mes,packet.getAddress(),packet.getPort());
            }
            catch (IOException e){
                e.printStackTrace(System.err);
            }
            Log.error(TAG,"Finished with socket, waiting for next connection.");
        }
    }
}
