package com.home.simpleserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ServerController {
	public static final String TAG = "ServerController";
	private final int checkPresenceTimerDelay = 100000;
	private String key;
	private String iv;
	private static ChatServer server;
	private MessageParser parser = new MessageParser();
	dbConnectionHandler dbConnection = new dbConnectionHandler();
	private Timer checkPresenceTimer;
	private boolean isDebuggerConnected = false;
	private InetAddress debuggerAddress;
	private int debuggerPort;
    private class userInfo {
    	String name;
    	InetAddress address;
    	int port;
    	String group;
    	boolean checkedPresence;
    	boolean doubleCheck;
    	String status;
    	userInfo(String name,InetAddress address,int port)
    	{
    		this.name = name;
    		this.address = address;
    		this.port = port;
    		this.group = "";
    		this.status = "online";
    		this.checkedPresence = true;
    		this.doubleCheck = false;
    	}
    }
    private class groupInfo {
    	String name;
    	groupInfo(String name)
    	{
    		this.name = name;
    	}
    }
    //ArrayList<userInfo> users = new ArrayList<userInfo>();
    ArrayList<groupInfo> groups = new ArrayList<groupInfo>();
	ServerController(String key,String iv)
    {
		this.key = key;
		this.iv = iv;
		groups.add(new groupInfo("ABCD"));
		groups.add(new groupInfo("0001"));
    	int port = 54321;
	    server = new ChatServer(port,this,key,iv);
	    dbConnection.establishConnection("dbfile.db");
	    Runnable r = new Runnable(){
			public void run() {
				server.waitForConnections();
			}};
		Thread t = new Thread(r);
		t.start();
		checkPresenceTimer = new Timer();
        checkPresenceTimer.schedule(new TimerTask() 
        {
    	    public void run() {
    	    	//checkPresenceTimerMethod();
    	    }
    	},this.checkPresenceTimerDelay,this.checkPresenceTimerDelay);
    }
	/*public ArrayList<userInfo> getUserList()
	{
		return this.users;
	}*/
	public void sendMessageToUser(int userid,String message)
	{
		tMessage mes = new tMessage(this.dbConnection.getUsername(userid),"",message);
		String send = parser.createMessage(mes);
		server.sendMessage(send, this.dbConnection.getAddress(userid),this.dbConnection.getPort(userid));
	}
	public void sendMessageToUserFromUser(String user1,String user2,String message)
	{
		tMessage mes = new tMessage(user1,"privatemessage_"+user2,message);
		String send = parser.createMessage(mes);
		server.sendMessage(send,this.dbConnection.getAddress(this.dbConnection.getUserId(user1)),this.dbConnection.getPort(this.dbConnection.getUserId(user1)));
	}
	public void handleMessage(tMessage mes,InetAddress address,int port)
	{
		Log.info(TAG, "handling message "+mes.action + " "+mes.data +" "+mes.name);
		if(mes.name.equals("dbgclient"))
		{
			if(mes.action.equals("register"))
			{
				this.debuggerAddress = address;
				this.debuggerPort = port;
				this.isDebuggerConnected = true;
			}
			if(this.isDebuggerConnected)
			{
				this.handleDebuggerMessage(mes.action);
			}
			return;
		}
		if(mes.action.equals("register"))
		{
			this.registerUser(mes.name, address,port);
		}
		if(mes.action.equals("deregister"))
		{
			this.deregisterUser(mes.name);
		}
		if(mes.action.equals("join"))
		{
			this.registerUserGroup(mes.name,mes.data);
		}
		if(mes.action.equals("left"))
		{
			this.deregisterUserGroup(mes.name);
		}
		if(mes.action.equals("groupmessage"))
		{
			this.sendBroadcastGroupMessage(mes.data,this.dbConnection.getGroup(this.dbConnection.getUserId(mes.name)),mes.name);
		}
		if(mes.action.contains("privatemessage"))
		{
			this.sendMessageToUserFromUser(mes.action.split("_")[1],mes.name, mes.data);
		}
		if(mes.action.equals("checkpresence"))
		{
			Log.info(TAG, "setting checkPresence to true, user: "+mes.name);
			//users.get(this.getUserIdByName(mes.name)).checkedPresence = true;
		}
		if(mes.action.equals("updatestatus"))
		{
			this.dbConnection.setStatus(this.dbConnection.getUserId(mes.name), mes.data);
			this.sendStatusUpdateMessage(mes.name, mes.data);
		}
		if(mes.action.equals("sound"))
		{
			this.sendBroadcastGroupSound(mes.data,this.dbConnection.getGroup(this.dbConnection.getUserId(mes.name)),mes.name);
		}
	}
	public void handleDebuggerMessage(String action)
	{
		Log.info(TAG, "Message from debugger: "+action);
		/*if(action.equals("exit"))
		{
			this.isDebuggerConnected = false;
		}
		if(action.equals("users"))
		{
			String userMessage = "";
			for(int i=0;i<users.size();i++)
			{
				userMessage+=users.get(i).name + ",";
			}
			server.sendUnencryptedMessage(userMessage, this.debuggerAddress, this.debuggerPort);
		}
		if(action.contains("registeruser"))
		{
			Log.info(TAG, "Register new user: "+action.split("_")[1]);
			this.registerUser(action.split("_")[1], this.debuggerAddress, this.debuggerPort);
			server.sendUnencryptedMessage("Done", this.debuggerAddress, this.debuggerPort);
		}
		if(action.equals("clearusers"))
		{
			Log.info(TAG, "Deleting all users.");
			users.clear();
			server.sendUnencryptedMessage("Done", this.debuggerAddress, this.debuggerPort);
		}
		if(action.contains("deleteuser"))
		{
			String username = action.replace("deleteuser ", "");
			Log.info(TAG, "Deleting user "+username);
			users.remove(this.getUserIdByName(username));
			server.sendUnencryptedMessage("Done", this.debuggerAddress, this.debuggerPort);
			this.sendPresenceNotification();
		}*/
	}
	public void sendStatusUpdateMessage(String username,String status)
	{
		ArrayList<Integer> users = this.dbConnection.getUsers();
		for(int i=0;i<users.size();i++)
		{
			tMessage mes = new tMessage(this.dbConnection.getUsername(users.get(i)),"updatestatus_"+username,status);
			String send = parser.createMessage(mes);
			server.sendMessage(send, this.dbConnection.getAddress(users.get(i)),this.dbConnection.getPort(users.get(i)));
		}
	}
	public void sendAckMessage(String username,String action)
	{
		Log.info(TAG, "sending ack message to "+username+", action = "+action);
		int userId = this.dbConnection.getUserId(username);
		tMessage mes = new tMessage(this.dbConnection.getUsername(userId),"ack",action);
		String send = parser.createMessage(mes);
		Log.info(TAG, send);
		server.sendMessage(send, this.dbConnection.getAddress(userId),this.dbConnection.getPort(userId));
	}
	public boolean isUserInList(String username)
	{
		return this.dbConnection.isUserInDB(username);
	}
	public void registerUser(String name,InetAddress address, int port)
	{
		Log.info(TAG, "Registering new user: " + name);
		if(this.isUserInList(name))
		{
			this.sendStatusUpdateMessage(name, "online");
			this.dbConnection.deleteUser(name);
			this.dbConnection.createNewUser(name, address.toString().replace("/", ""), port);
		}
		else
		{
			this.dbConnection.createNewUser(name, address.toString().replace("/", ""), port);
		}
		this.sendAckMessage(name, "register");
		this.sendPresenceNotification();
		try {
				Thread.sleep(1000);
		} catch (InterruptedException e) {
				e.printStackTrace();
		}
		this.sendGroupList();
		String status = "";
		ArrayList<Integer> users = this.dbConnection.getUsers();
		for(int i=0;i<users.size();i++)
		{
			status = this.dbConnection.getStatus(users.get(i));
			tMessage mes = new tMessage(name,"updatestatus_"+this.dbConnection.getUsername(users.get(i)),status);
			String send = parser.createMessage(mes);
			server.sendMessage(send,address,port);
		}
	}
	public void registerUserGroup(String username,String groupname)
	{
		this.dbConnection.setGroup(this.dbConnection.getUserId(username), groupname);
		this.sendAckMessage(username, "join");
	}
	public void deregisterUserGroup(String username)
	{
		this.dbConnection.setGroup(this.dbConnection.getUserId(username), "no");
		this.sendAckMessage(username, "left");
	}
	
	public void sendBroadcastGroupSound(String sound,String groupname,String username)
	{
		Log.info(TAG, "Sending sound to group:"+groupname);
		ArrayList<Integer> users = this.dbConnection.getUsers();
		for(int i=0;i<users.size();i++)
		{
			if(this.dbConnection.getGroup(users.get(i)).equals(groupname))
			{
				tMessage mes = new tMessage(this.dbConnection.getUsername(users.get(i)),"sound_"+username,sound);
				String send = parser.createMessage(mes);
				server.sendMessage(send, this.dbConnection.getAddress(users.get(i)),this.dbConnection.getPort(users.get(i)));
			}
		}
	}
	
	public void sendBroadcastGroupMessage(String message,String groupname,String username)
	{
		Log.info(TAG, "Sending message to group:"+groupname);
		ArrayList<Integer> users = this.dbConnection.getUsers();
		for(int i=0;i<users.size();i++)
		{
			if(this.dbConnection.getGroup(users.get(i)).equals(groupname))
			{
				tMessage mes = new tMessage(this.dbConnection.getUsername(users.get(i)),"groupmessage_"+username,message);
				String send = parser.createMessage(mes);
				server.sendMessage(send, this.dbConnection.getAddress(users.get(i)),this.dbConnection.getPort(users.get(i)));
			}
		}
	}
	public void sendGroupList()
	{
		ArrayList<Integer> users = this.dbConnection.getUsers();
		for(int i=0;i<users.size();i++)
		{
			tMessage mes = new tMessage(this.dbConnection.getUsername(users.get(i)),"groups",this.getGroups());
			String send = parser.createMessage(mes);
			Log.info(TAG, "Sending group list to:"+this.dbConnection.getUsername(users.get(i)));
			server.sendMessage(send, this.dbConnection.getAddress(users.get(i)),this.dbConnection.getPort(users.get(i)));
		}
	}
	public void sendPresenceNotification()
	{
		ArrayList<Integer> users = this.dbConnection.getUsers();
		for(int i=0;i<users.size();i++)
		{
			tMessage mes = new tMessage(this.dbConnection.getUsername(users.get(i)),"presence",this.getOnlineUsers(this.dbConnection.getUsername(users.get(i))));
			String send = parser.createMessage(mes);
			Log.info(TAG, "Sending presence to:"+this.dbConnection.getUsername(users.get(i)));
			server.sendMessage(send, this.dbConnection.getAddress(users.get(i)),this.dbConnection.getPort(users.get(i)));
		}
	}
	public String getGroups()
	{
		String groupsString = "";
		for(int i=0;i<groups.size();i++)
		{
			if(!groupsString.equals(""))
			{
				groupsString+=(","+groups.get(i).name);
			}
			else
			{
				groupsString+=groups.get(i).name;
			}
		}
		return groupsString;
	}
	public String getOnlineUsers(String username)
	{
		String usersString = "";
		ArrayList<Integer> users = this.dbConnection.getUsers();
		for(int i=0;i<users.size();i++)
		{
			if(this.dbConnection.getUsername(users.get(i)).equals(username)==false)
			{
				if(!usersString.equals(""))
				{
					usersString+=(","+this.dbConnection.getUsername(users.get(i)));
				}
				else
				{
					usersString+=this.dbConnection.getUsername(users.get(i));
				}
			}
		}
		return usersString;
	}
	public void deregisterUser(String name)
	{
		this.sendAckMessage(name, "deregister");
		Log.info(TAG, "deregistering user: " + name);
		this.sendStatusUpdateMessage(name, "offline");
	}
	public int getUserIdByName(String name)
	{
		return this.dbConnection.getUserId(name);
	}
}
