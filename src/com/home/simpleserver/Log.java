package com.home.simpleserver;

public class Log {
	private static final String INFO = "[info]";
	private static final String ERROR = "[error]";
	private String filename;
    static void info(String tag,String message)
    {
    	System.out.println("["+tag+"]"+INFO+message);
    }
    static void error(String tag,String message)
    {
    	System.err.println("["+tag+"]"+ERROR+message);
    }
}
