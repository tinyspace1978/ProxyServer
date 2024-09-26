package cn.tinyspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WebSocketDeal extends Thread {

    Logger logger = LoggerFactory.getLogger(WebSocketDeal.class);
    HttpServerProcess parentProcess;
    InputStream in;
    OutputStream out;

    public WebSocketDeal(InputStream in, OutputStream out, HttpServerProcess parentProcess) {
        this.in = in;
        this.out = out;
        this.parentProcess = parentProcess;
    }

    public void closeAll() {
        try {
            if (parentProcess != null) {
                parentProcess.close();
            }
        } catch (Exception e) {
        }
        if (in != null) {
            try {
                in.close();
            } catch (Exception e) {
            }
        }
        if (out != null) {
            try {
                out.close();
            } catch (Exception e) {
            }
        }
    }

    public void run() {
        while (true) {
            try {
                if (in.available() > 0) {
                    byte[] tmpBody = new byte[1024];
                    int size = in.read(tmpBody, 0, 1024);
                    if (size == 0) {
                        continue;
                    }
                    if (size < 0) {
                        throw new IOException("socket broken");
                    }
                    if (size > 0) {
                        out.write(tmpBody, 0, size);
                        out.flush();
                        logger.debug("get " + size + " data");
                    }
                } else {
                    Thread.sleep(100L);
                }
            } catch (java.net.SocketException e) {
                logger.error("error", e);
                closeAll();
                break;
            } catch (java.io.IOException e) {
                logger.error("error", e);
                closeAll();
                break;
            } catch (Exception e) {
                logger.error("error", e);
                closeAll();
                break;
            }
        }
    }
}
