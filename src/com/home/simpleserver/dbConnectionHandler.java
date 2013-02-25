package com.home.simpleserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class dbConnectionHandler {
	public static final String TAG = "dbConnectionHandler";
	Connection connection;
	Statement statement;
	dbConnectionHandler()
	{
		try {
			Class.forName("org.sqlite.JDBC");
			connection = DriverManager.getConnection("jdbc:sqlite:"+"/home/dimka/SimpleServer/dbfile.db");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public void establishConnection(String dbname)
	{
		try {
			statement = connection.createStatement();
			statement.execute("create table if not exists 'Users' ('id' INTEGER PRIMARY KEY AUTOINCREMENT, 'name' text, 'address' text, 'port' text,'talkgroup' text, 'status' text);");
			statement.execute("DELETE FROM Users");
			InetAddress inet;
			try {
				inet = InetAddress.getByName("193.125.22.1");
				System.out.println(inet.toString().replace("/", ""));
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public boolean isUserInDB(String username)
	{
		ResultSet rs;
		try {
			rs = statement.executeQuery("select * from Users WHERE name='"+username+"'");
			if (!rs.next() ) {
				return false;
			}
			else return true;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}
	public void createNewUser(String name, String address, int port)
	{
		try {
			Log.info(TAG, "User address: "+address.toString());
			statement.executeUpdate("INSERT INTO 'Users' ('name','address','port','talkgroup','status') values ('"+name+"','"+address.toString()+"','"+String.valueOf(port)+"','no','online');");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public void deleteUser(String name)
	{
		try {
			statement.executeUpdate("DELETE FROM 'Users' WHERE name='"+name+"'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getUserId(String name)
	{
		ResultSet rs;
		try {
			rs = statement.executeQuery("select * from Users WHERE name='"+name+"'");
			return Integer.parseInt(rs.getString("id"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return -1;
	}
	public ArrayList<Integer> getUsers()
	{
		ResultSet rs;
		ArrayList<Integer> results = new ArrayList<Integer>();
		try {
			rs = statement.executeQuery("select id from Users");
			while (rs.next())
			{
				results.add(rs.getInt("id"));
				Log.info(TAG,"Added: "+rs.getInt("id"));
			}
			return results;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return results;
	}
	public String getUsername(int id)
	{
		ResultSet rs;
		try {
			rs = statement.executeQuery("select * from Users WHERE id='"+id+"'");
			return rs.getString("name");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	public InetAddress getAddress(int id)
	{
		ResultSet rs;
		try {
			rs = statement.executeQuery("select * from Users WHERE id='"+id+"'");
			return InetAddress.getByName(rs.getString("address"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	public int getPort(int id)
	{
		ResultSet rs;
		try {
			rs = statement.executeQuery("select * from Users WHERE id='"+id+"'");
			return Integer.parseInt(rs.getString("port"));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	public String getGroup(int id)
	{
		ResultSet rs;
		try {
			rs = statement.executeQuery("select * from Users WHERE id='"+id+"'");
			return rs.getString("talkgroup");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	public String getStatus(int id)
	{
		ResultSet rs;
		try {
			rs = statement.executeQuery("select * from Users WHERE id='"+id+"'");
			return rs.getString("status");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "";
	}
	public void setGroup(int id, String group)
	{
		try {
			statement.execute("UPDATE Users SET talkgroup='"+group+"' where id='"+id+"'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	public void setStatus(int id, String status)
	{
		try {
			statement.execute("UPDATE Users SET status='"+status+"' where id='"+id+"'");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
