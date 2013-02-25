package com.home.simpleserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.swing.DefaultListModel;
import javax.swing.JList;

public class MainWindow {
	 public static final String TAG = "ServerMainWindow";
	 public static JList list;
	 public static DefaultListModel listModel;
	 private static ServerController controller;
	 private static String key = "02345780185";
	 private static String iv = "93842901";
	 public static void main(String argv[]) {
		 Log.info(TAG,"Starting server..");
		 controller = new ServerController(key,iv);
		 Log.info(TAG, "Server started successfully.");
	 }
}
