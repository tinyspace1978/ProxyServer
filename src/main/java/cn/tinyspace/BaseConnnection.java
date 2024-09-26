package cn.tinyspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public abstract class BaseConnnection extends Thread {
    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    Logger logger = LoggerFactory.getLogger(BaseConnnection.class);
    Socket client;
    SocketChannel socketChannel;
    private int localPort;
    private String clientIp;
    private InputStream in;
    private OutputStream out;
    private ProcessConnection processConnection;
    private String threadName;

    public BaseConnnection(ProcessConnection conn) {
        this.processConnection = conn;
        this.localPort = conn.getLocalPort();
        this.clientIp = conn.getClientIp();
        this.in = conn.getIn();
        this.out = conn.getOut();
        this.client = conn.getClient();
        threadName = conn.getName();
    }

    public void addCommand(String command) {
        try {
            queue.offer(command);
        } catch (Exception e) {
            logger.error("offer error", e);
        }
    }

    public String pollCommand() {
        try {
            return queue.poll();
        } catch (Exception e) {
            logger.error("offer error", e);
        }
        return null;
    }

    public void closeAll() {
        if (client != null) {
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
        } else if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (Exception ee) {
            }
        }
        String key = clientIp + ":" + localPort;
        ConnectionManager.getInstance().removeConnection(key, threadName);
    }

    public int getLocalPort() {
        return localPort;
    }

    public void setLocalPort(int localPort) {
        this.localPort = localPort;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
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

    public Socket getClient() {
        return client;
    }

    public void setClient(Socket client) {
        this.client = client;
    }

    public ProcessConnection getProcessConnection() {
        return processConnection;
    }

    public void setProcessConnection(ProcessConnection processConnection) {
        this.processConnection = processConnection;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }


}
