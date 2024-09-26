package cn.tinyspace;

import java.util.HashMap;
import java.util.Map;

public class Config {
    public static String UserName = "tinyspace";
    public static String UserToken = "Ski98ux1Mox0";//You Need change this by startup arguments
    public static String ServerHost = "127.0.0.1";

    public static int WebServerPort = 34681;


    public static int AdminServerPort = 33433;

    public static int P2PPort = 38080;

    public static int MaxClients = 30;

    public static Map<String, String> HostMap = new HashMap<>();

}
