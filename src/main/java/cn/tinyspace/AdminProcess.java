package cn.tinyspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.net.Socket;
import java.util.Set;

public class AdminProcess extends Thread {
    Socket client;
    InputStream in;
    OutputStream out;
    Logger logger = LoggerFactory.getLogger(AdminProcess.class);
    Thread childIn;
    Thread childOut;
    private String clientIp;
    private int localPort;

    public AdminProcess(int localPort, Socket s) { // constructor
        client = s;
        this.localPort = localPort;
        try {
            in = client.getInputStream();
            out = client.getOutputStream();
        } catch (Exception e) {
            logger.info("Exception: ", e);
        }
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public Socket getClient() {
        return client;
    }

    public void setClient(Socket client) {
        this.client = client;
    }

    public InputStream getIn() {
        return in;
    }

    public void setIn(InputStream in) {
        this.in = in;
    }

    public OutputStream getOut() {
        return out;
    }

    public void setOut(OutputStream out) {
        this.out = out;
    }

    public void close() {
        logger.info("关闭Admin连接: " + this.clientIp + ":" + this.localPort);
        try {
            in.close();
        } catch (Exception ee) {
        }
        try {
            out.close();
        } catch (Exception ee) {
        }
        try {
            client.close();
        } catch (Exception ee) {
        }
        try {
            this.interrupt();
        } catch (Exception ee) {
        }
    }

    public void run() {
        try {
            clientIp = client.getInetAddress().getHostAddress();
            MDC.put("remoteAddr", clientIp);
            MDC.put("localPort", "" + localPort);
        } catch (Exception e) {

        }

        try {

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            PrintStream out = new PrintStream(client.getOutputStream());
            out.println("请输入命令，输入quit结束，查看所有已连接主机输入list");
            out.println("命令格式为：host 34581|34681 UidPath PropName");
            out.println("如果某项为空，请输入null");
            while (true) {
                String line = br.readLine();
                if ("quit".equals(line)) {
                    out.println("bye bye");
                    Thread.sleep(1000L);
                    close();
                    return;
                } else if ("list".equals(line) || "ll".equals(line) || "ls".equals(line)) {
                    Set<String> allhost = ConnectionManager.getInstance().getAllHosts();
                    for (String host : allhost) {
                        out.println(host);
                    }
                    out.println(allhost.size() + " server connected");
                    continue;
                }
                String[] data = line.split("\\s+");
                if (data.length != 4) {
                    out.println("未知命令，请输入4个参数，例如");
                    out.println("host 34581 UidPath PropName");
                    out.println("如果某项为空，请输入null");
                } else {
                    // String key = clientIp + ":" + localPort;
                    String key = data[0] + ":" + data[1];
                    ProcessConnection conn = ConnectionManager.getInstance().getConnection(key);
                    if (conn != null) {
                        conn.getChildOut().addCommand(line);
                        out.println(data[2] + " " + data[3] + "命令已经发送");
                    } else {
                        out.println("没有找到活动连接");
                    }
                }
            }

        } catch (Exception e) {
            logger.info("Exception: ", e);

        }
    }
}
