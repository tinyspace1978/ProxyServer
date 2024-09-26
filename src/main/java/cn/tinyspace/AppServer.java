package cn.tinyspace;
/**
 *Author: tinyspace(tinyspace@gmail.com)
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class AppServer {


    static Logger logger = LoggerFactory.getLogger(AppServer.class);
    private final int port;
    private ServerSocket listen;

    public AppServer(int port) {
        this.port = port;
    }

    /**
     * 启动主程序，根据接受的参数，是启动程序还是终止程序
     *
     * @param args String[]
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        if (args == null || args.length == 0) {
            System.out.println("usage AppServer server|client [serverHost] [userName] [userToken]");
            return;
        }
        boolean runServer = true;
        if (args != null && args.length > 0) {
            if (args[0].equalsIgnoreCase("client")) {
                runServer = false;
            }
            if (args.length >= 2) {
                Config.ServerHost = args[1];
            }
            if (args.length >= 4) {
                Config.UserName = args[2];
                Config.UserToken = args[3];
            }
        }
        //38080
        if (runServer) {
            int[] ports = new int[]{Config.P2PPort, Config.WebServerPort};
            for (int port : ports) {
                ConnectionManager.getInstance().getCachedExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        AppServer server = new AppServer(port);
                        server.run();
                    }
                });
            }
        } else {
            List<ClientServer> watch = new ArrayList<>();
            for (int i = 0; i < Config.MaxClients; i++) {
                ClientServer clientServer = new ClientServer(Config.P2PPort, null);
                ConnectionManager.getInstance().getCachedExecutor().execute(clientServer);
                try {
                    Thread.sleep(100L);
                } catch (Exception e) {
                }
                watch.add(clientServer);
            }
            new WatchDog(watch, Config.MaxClients, Config.P2PPort).start();
        }

    }

    /**
     * Create ServerSocket
     *
     * @return ServerSocket
     * @throws Exception
     */
    public ServerSocket getServer() throws Exception {
        logger.info("OS.name " + System.getProperty("os.name"));
        try {
            logger.info("Service will listen on:" + port);
            if (port == Config.P2PPort) {
                return new ServerSocket(port);
            } else {
                return new ServerSocket(port, 1024, InetAddress.getByName("127.0.0.1"));
            }
        } catch (Exception e) {
            logger.info("can't create socket : " + e);
            throw e;
        }
    }

    /**
     * 对于每个客户端请求，创建一个新的连接。
     */

    public void run() {
        try {
            //后面监听来自客户端的连接，以便终止进程。
            listen = getServer();
            while (true) {
                Socket client = listen.accept();
                String host = client.getInetAddress().getHostAddress();
                logger.info(port + " remote IP=" + host);
                if (port == Config.WebServerPort) {
                    HttpServerProcess cc = new HttpServerProcess(port, client);
                    ConnectionManager.getInstance().getCachedExecutor().execute(cc);
                } else if (port == Config.AdminServerPort) {
                    AdminProcess cc = new AdminProcess(port, client);
                    ConnectionManager.getInstance().getCachedExecutor().execute(cc);
                } else {
                    ProcessConnection cc = new ProcessConnection(port, client);
                    ConnectionManager.getInstance().getCachedExecutor().execute(cc);
                }
            }

        } catch (Exception e) {
            logger.info("Exception:", e);
        }
    }

    /**
     * 关闭所有连接。并关闭程序本身
     */
    public void close() {
        logger.info("关闭所有连接");
        try {
            if (listen != null && !listen.isClosed()) {
                logger.info("关闭listen");
                listen.close();
            }
        } catch (Exception ee) {
            logger.info(ee.toString());
        }
    }
}
