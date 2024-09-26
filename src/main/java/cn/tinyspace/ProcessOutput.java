package cn.tinyspace;

import cn.tinyspace.utils.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.OutputStream;

public class ProcessOutput extends BaseConnnection {

    Logger logger = LoggerFactory.getLogger(ProcessOutput.class);

    public ProcessOutput(ProcessConnection conn) {
        super(conn);
    }

    public void run() {
        try {
            MDC.put("remoteAddr", super.getClientIp());
            MDC.put("localPort", "" + super.getLocalPort());
        } catch (Exception e) {

        }

        int num = 0;
        OutputStream out = super.getOut();
        ProcessConnection conn = super.getProcessConnection();
        while (true) {
            try {
                if (!conn.isBusy()) {
                    if ((System.currentTimeMillis() - conn.getLastOutTime()) > 20000L) {
                        conn.setBusy(true);
                        if (num % 50 == 0) {
                            logger.debug("send heartbeat");
                        }
                        num++;
                        out.write(HttpUtils.ZeroBytes);
                        conn.setLastOutTime(System.currentTimeMillis());
                        conn.setBusy(false);
                        continue;
                    }
                }
                Thread.sleep(1000L);
            } catch (java.net.SocketException e) {
                logger.error("error", e);
                closeAll();
                break;
            } catch (Exception e) {
                logger.error("error", e);
            }
        }


    }
}
