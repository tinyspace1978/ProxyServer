package cn.tinyspace;

import cn.tinyspace.utils.HttpObject;
import cn.tinyspace.utils.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class HttpServerProcess extends Thread {
    Socket client;
    InputStream in;
    OutputStream out;
    BufferedReader is;
    Logger logger = LoggerFactory.getLogger(HttpServerProcess.class);
    Thread childIn;
    Thread childOut;
    private String clientIp;
    private int localPort;

    public HttpServerProcess(int localPort, Socket s) { // constructor
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

    public void close() {
        logger.info("关闭连接: " + this.clientIp + ":" + this.localPort);
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


    public void dealWebSocket(HttpObject object, ProcessConnection conn) throws IOException {
        HttpUtils.replaceHost(object.getHeaders(), true);
        //建立WebSocket的首次请求还是要使用AES
        HttpUtils.sendHttpRequestWithAes(object, conn.getOut(), null, conn.getAesKey());
        ConnectionManager.getInstance().getCachedExecutor().execute(new WebSocketDeal(in, conn.getOut(), this));
        WebSocketBlock.runLoop(conn.getIn(), out);
    }

    public void run() {
        try {
            clientIp = client.getInetAddress().getHostAddress();
            MDC.put("remoteAddr", clientIp);
            MDC.put("localPort", "" + localPort);
        } catch (Exception e) {

        }
        boolean websocket = false;
        boolean errorOccur = false;
        try {
            HttpObject object = HttpUtils.parseReuqestHttpObject(in);
            if (object != null && object.getFirstLine() != null) {
                websocket = HttpUtils.isWebSocket(object.getHeaders());

                ProcessConnection conn = ConnectionManager.getInstance().popConnection();
                if (conn != null) {
                    if (websocket) {
                        dealWebSocket(object, conn);//here will blocked
                        return;
                    }
                    logger.debug("receive request from " + clientIp + " " + object.getFirstLine() + ", now forward to:" + conn.getClientIp());
                    HttpObject resp = null;
                    try {
                        HttpUtils.replaceHost(object.getHeaders(), true); //replace host name header
                        resp = HttpUtils.sendHttpRequestWithAes(object, conn.getOut(), conn.getIn(), conn.getAesKey());
                    } catch (Exception e) {
                        logger.info("client server error:" + conn.getName(), 2);
                    } finally {
                        logger.debug("release:" + conn.getName());
                        if (object.getBodyFile() != null && object.getBodyFile().exists()) {
                            object.getBodyFile().delete();
                        }
                        conn.setBusy(false);
                    }

                    if (resp != null && resp.getFirstLine() != null) {

                        if (logger.isDebugEnabled()) {
                            if (resp.getBodyFile() != null) {
                                logger.debug("receive bigfile :" + conn.getClientIp() + " " + resp.getFirstLine() + ":::" + resp.getBodyFile().getAbsolutePath() + ":" + resp.getBodyFile().length());
                            } else if (resp.getBody() == null || resp.getBody().length == 0) {
                                logger.debug("receive " + conn.getClientIp() + "," + resp.getFirstLine() + ":::0");
                            } else {
                                logger.debug("receive " + conn.getClientIp() + "," + resp.getFirstLine() + ":::" + resp.getBody().length);
                            }
                        }
                        HttpUtils.replaceHost(resp.getHeaders(), false);//响应header转换
                        HttpUtils.sendHttpRequest(resp, out, null);
                        if (resp.getBodyFile() != null && resp.getBodyFile().exists()) {
                            resp.getBodyFile().delete();
                        }
                    } else {
                        logger.info("empty response from " + conn.getClientIp());
                    }
                    out.flush();
                }
            }
            close();

        } catch (Exception e) {
            logger.error("error", e);
            errorOccur = true;
        } finally {
            if (errorOccur) {
                close();
            }
        }

    }
}
