package cn.tinyspace;

import cn.tinyspace.utils.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class ProcessConnection extends Thread {
    Socket client;
    InputStream in;
    OutputStream out;
    Logger logger = LoggerFactory.getLogger(ProcessConnection.class);
    BaseConnnection childIn;
    BaseConnnection childOut;
    boolean busy;
    private byte[] aesKey;
    private String clientIp;
    private int localPort;
    private long lastInTime;
    private long lastOutTime;

    public ProcessConnection(int localPort, Socket s) { // constructor
        client = s;
        this.localPort = localPort;
        this.lastInTime = System.currentTimeMillis();
        this.lastOutTime = System.currentTimeMillis();
        try {
            if (s != null) {
                in = client.getInputStream();
                out = client.getOutputStream();
            }
        } catch (Exception e) {
            logger.info("Exception: ", e);
        }
    }

    public byte[] getAesKey() {
        return aesKey;
    }

    public void setAesKey(byte[] aesKey) {
        this.aesKey = aesKey;
    }

    public BaseConnnection getChildIn() {
        return childIn;
    }

    public BaseConnnection getChildOut() {
        return childOut;
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

    public long getLastInTime() {
        return lastInTime;
    }

    public void setLastInTime(long lastInTime) {
        this.lastInTime = lastInTime;
    }

    public long getLastOutTime() {
        return lastOutTime;
    }

    public void setLastOutTime(long lastOutTime) {
        this.lastOutTime = lastOutTime;
    }

    public boolean isBroken() {
        return !client.isConnected();
    }

    public boolean isBusy() {
        return busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    public void close() {
        logger.info("Close connection: " + this.clientIp + ":" + this.localPort);
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
        String key = clientIp + ":" + localPort;
        try {
            logger.debug("start handshake");
            //step 1: return 8 random char
            Random random = new Random();
            int randNum = 1000000 + random.nextInt(800000);
            String str = Integer.toHexString(randNum).toUpperCase();
            byte[] randomChars = str.getBytes(StandardCharsets.UTF_8);
            int len = randomChars.length;
            out.write(len);
            out.write(randomChars);
            int userLen = in.read();
            byte[] userBytes = BytesUtils.readBigLen(in, userLen);
            int passLen = in.read();
            byte[] passBytes = BytesUtils.readBigLen(in, passLen);
            byte[] checkUserInfo = new byte[userLen];
            BytesUtils.encodeUserPass(userBytes, userLen, checkUserInfo, randomChars);
            byte[] checkPassInfo = new byte[passLen];
            BytesUtils.encodeUserPass(passBytes, passLen, checkPassInfo, randomChars);
            if (!(new String(checkUserInfo)).equalsIgnoreCase(Config.UserName)
                    || !(new String(checkPassInfo)).equalsIgnoreCase(Config.UserToken)) {
                logger.error("user/token does not match,UserName:" + new String(checkUserInfo) + ",Token:" + (new String(checkPassInfo)));
                close();
                return;
            }


            int rand = len + userLen + passLen;
            out.write(rand);
            int serverLen = in.read();
            byte[] aesKey = BytesUtils.readBigLen(in, serverLen);
            this.setAesKey(aesKey);
            logger.debug("AesKey:" + BytesUtils.formatByte(aesKey) + ":::" + new String(aesKey, StandardCharsets.UTF_8));
            ConnectionManager.getInstance().addConnection(key, this);
            out.write((byte) 1);
            out.flush();

            childIn = new ProcessInput(this);
            ConnectionManager.getInstance().getCachedExecutor().execute(childIn);

            childOut = new ProcessOutput(this);
            ConnectionManager.getInstance().getCachedExecutor().execute(childOut);
            //childOut.start();

        } catch (Exception e) {
            logger.info("Exception: ", e);
            ConnectionManager.getInstance().removeConnection(key, this.getName());
        }
    }
}
