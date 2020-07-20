package jason.tcpdemo;

import android.app.Application;

import jason.tcpdemo.coms.TcpClient;
import jason.tcpdemo.coms.TcpServer;

public class MyApp extends Application {
    public static TcpServer tcpServer1 = null, tcpServer2 = null;
    public static TcpClient tcpClient = null;
    public String name;
    public static MainActivity mainActivity;
}
