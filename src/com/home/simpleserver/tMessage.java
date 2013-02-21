package com.home.simpleserver;


public class tMessage
{
	public static final String TAG = "MessageParser";
	String name;
	String data;
	String action;
	tMessage(String name,String action,String data)
	{
		this.name = name;
		this.action = action;
		this.data = data;
	}
	public void printMessage()
	{
		Log.info(TAG,"Name = "+name+", data = "+data);
	}
}
