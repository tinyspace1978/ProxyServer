package cn.tinyspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class WebSocketBlock {
    static Logger logger = LoggerFactory.getLogger(WebSocketBlock.class);

    public static void runLoop(InputStream in, OutputStream out) throws IOException {
        while (true) {
            if (in.available() > 0) {
                byte[] tmpBody = new byte[1024];
                int size = in.read(tmpBody, 0, 1024);
                if (size == 0) {
                    logger.info("get size=0");
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
                try {
                    Thread.sleep(100L);
                } catch (Exception e) {
                }
            }
        }
    }
}
