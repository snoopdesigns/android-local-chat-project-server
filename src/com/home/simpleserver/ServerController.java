package com.home.simpleserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
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
    ArrayList<userInfo> users = new ArrayList<userInfo>();
    ArrayList<groupInfo> groups = new ArrayList<groupInfo>();
	ServerController(String key,String iv)
    {
		this.key = key;
		this.iv = iv;
		groups.add(new groupInfo("ABCD"));
		groups.add(new groupInfo("0001"));
    	int port = 54321;
	    server = new ChatServer(port,this,key,iv);
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
	private void checkPresenceTimerMethod()
	{
	    Thread t = new Thread(checkPresence);
	    t.start();
	}
	private Runnable checkPresence = new Runnable() {
		public void run() {
			tMessage mes;
		    for(int i=0;i<users.size();i++)
		    {
		    	Log.info(TAG, "cheking presence of user "+users.get(i).name);
		    	if(users.get(i).checkedPresence==false)
		    	{
		    		if(users.get(i).doubleCheck)
		    		{
		    			Log.info(TAG, "Delete user "+users.get(i).name +" due presence check");
		    			users.remove(i);
		    		}
		    		users.get(i).doubleCheck = true;
		    		continue;
		    	}
		    	else
		    	{
		    		users.get(i).checkedPresence = false;
			    	mes = new tMessage(users.get(i).name,"checkpresence","");
					String send = parser.createMessage(mes);
					server.sendMessage(send, users.get(i).address, users.get(i).port);
		    	}
		    }
		    sendPresenceNotification();
		}
	};
	public ArrayList<userInfo> getUserList()
	{
		return this.users;
	}
	public void sendMessageToUser(int userid,String message)
	{
		tMessage mes = new tMessage(users.get(userid).name,"",message);
		String send = parser.createMessage(mes);
		server.sendMessage(send, users.get(userid).address,users.get(userid).port);
	}
	public void sendMessageToUserFromUser(String user1,String user2,String message)
	{
		tMessage mes = new tMessage(user1,"privatemessage_"+user2,message);
		String send = parser.createMessage(mes);
		server.sendMessage(send, users.get(this.getUserIdByName(user1)).address,
				users.get(this.getUserIdByName(user1)).port);
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
			this.sendBroadcastGroupMessage(mes.data,users.get(this.getUserIdByName(mes.name)).group,mes.name);
		}
		if(mes.action.contains("privatemessage"))
		{
			this.sendMessageToUserFromUser(mes.action.split("_")[1],mes.name, mes.data);
		}
		if(mes.action.equals("checkpresence"))
		{
			Log.info(TAG, "setting checkPresence to true, user: "+mes.name);
			users.get(this.getUserIdByName(mes.name)).checkedPresence = true;
		}
		if(mes.action.equals("updatestatus"))
		{
			users.get(this.getUserIdByName(mes.name)).status = mes.data;
			this.sendStatusUpdateMessage(mes.name, mes.data);
		}
		if(mes.action.equals("sound"))
		{
			this.sendBroadcastGroupSound(mes.data,users.get(this.getUserIdByName(mes.name)).group,mes.name);
		}
	}
	public void handleDebuggerMessage(String action)
	{
		Log.info(TAG, "Message from debugger: "+action);
		if(action.equals("exit"))
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
		}
	}
	public void sendStatusUpdateMessage(String username,String status)
	{
		for(int i=0;i<users.size();i++)
		{
			tMessage mes = new tMessage(users.get(i).name,"updatestatus_"+username,status);
			String send = parser.createMessage(mes);
			server.sendMessage(send, users.get(i).address,users.get(i).port);
		}
	}
	public void sendAckMessage(String username,String action)
	{
		Log.info(TAG, "sending ack message to "+username+", action = "+action);
		int userId = this.getUserIdByName(username);
		tMessage mes = new tMessage(users.get(userId).name,"ack",action);
		String send = parser.createMessage(mes);
		Log.info(TAG, send);
		server.sendMessage(send, users.get(userId).address,users.get(userId).port);
	}
	public boolean isUserInList(String username)
	{
		for(int i=0;i<users.size();i++)
		{
			if(users.get(i).name.equals(username))
				return true;
		}
		return false;
	}
	public void registerUser(String name,InetAddress address, int port)
	{
		Log.info(TAG, "Registering new user: " + name);
		userInfo user = new userInfo(name,address,port);
		if(this.isUserInList(name))
		{
			this.sendStatusUpdateMessage(name, "online");
			users.remove(this.getUserIdByName(name));
			users.add(user);
		}
		else
		{
			users.add(user);
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
		for(int i=0;i<this.users.size();i++)
		{
			status = users.get(i).status;
			tMessage mes = new tMessage(name,"updatestatus_"+users.get(i).name,status);
			String send = parser.createMessage(mes);
			server.sendMessage(send,address,port);
		}
	}
	public void registerUserGroup(String username,String groupname)
	{
		users.get(this.getUserIdByName(username)).group = groupname;
		this.sendAckMessage(username, "join");
	}
	public void deregisterUserGroup(String username)
	{
		users.get(this.getUserIdByName(username)).group = "";
		this.sendAckMessage(username, "left");
	}
	
	public void sendBroadcastGroupSound(String sound,String groupname,String username)
	{
		Log.info(TAG, "Sending sound to group:"+groupname);
		for(int i=0;i<users.size();i++)
		{
			if(users.get(i).group.equals(groupname))
			{
				tMessage mes = new tMessage(users.get(i).name,"sound_"+username,sound);
				String send = parser.createMessage(mes);
				server.sendMessage(send, users.get(i).address,users.get(i).port);
			}
		}
	}
	
	public void sendBroadcastGroupMessage(String message,String groupname,String username)
	{
		Log.info(TAG, "Sending message to group:"+groupname);
		for(int i=0;i<users.size();i++)
		{
			if(users.get(i).group.equals(groupname))
			{
				tMessage mes = new tMessage(users.get(i).name,"groupmessage_"+username,message);
				String send = parser.createMessage(mes);
				Log.info(TAG, "Sending group message:"+message+" to "+ users.get(i).name);
				server.sendMessage(send, users.get(i).address,users.get(i).port);
			}
		}
	}
	public void sendGroupList()
	{
		for(int i=0;i<users.size();i++)
		{
			tMessage mes = new tMessage(users.get(i).name,"groups",this.getGroups());
			String send = parser.createMessage(mes);
			Log.info(TAG, "Sending group list to:"+users.get(i).name);
			server.sendMessage(send, users.get(i).address,users.get(i).port);
		}
	}
	public void sendPresenceNotification()
	{
		for(int i=0;i<users.size();i++)
		{
			tMessage mes = new tMessage(users.get(i).name,"presence",this.getOnlineUsers(users.get(i).name));
			String send = parser.createMessage(mes);
			Log.info(TAG, "Sending presence to:"+users.get(i).name);
			server.sendMessage(send, users.get(i).address,users.get(i).port);
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
		for(int i=0;i<users.size();i++)
		{
			if(users.get(i).name.equals(username)==false)
			{
				if(!usersString.equals(""))
				{
					usersString+=(","+users.get(i).name);
				}
				else
				{
					usersString+=users.get(i).name;
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
		for(int i=0;i<users.size();i++)
		{
			if(users.get(i).name.equals(name))
			{
				return i;
			}
		}
		return -1;
	}
}
