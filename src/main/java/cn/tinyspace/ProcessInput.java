package cn.tinyspace;

import cn.tinyspace.utils.HttpObject;
import cn.tinyspace.utils.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.InputStream;

public class ProcessInput extends BaseConnnection {

    Logger logger = LoggerFactory.getLogger(ProcessInput.class);

    public ProcessInput(ProcessConnection conn) {
        super(conn);
    }


    public void run() {
        try {
            MDC.put("remoteAddr", super.getClientIp());
            MDC.put("localPort", "" + super.getLocalPort());
        } catch (Exception e) {

        }

        int num = 0;
        InputStream in = super.getIn();
        ProcessConnection conn = super.getProcessConnection();
        while (true) {
            try {
                if (!conn.isBusy()) {
                    //logger.info("heart beat 1，isBusy:" + conn.isBusy());
                    if ((System.currentTimeMillis() - conn.getLastInTime()) > 10000 && in.available() > 0) {
                        //logger.info("心跳2");
                        //conn.setBusy(true);
                        HttpObject object = HttpUtils.parseHttpObjectWithAes(in, conn.getAesKey());
                        if (object == null || object.getFirstLine() == null) {
                            if (num % 20 == 0) {
                                logger.debug("receive heartbeat");
                            }
                            num++;
                        } else {
                            logger.error("unknown package:" + object.getFirstLine());
                        }
                        conn.setLastInTime(System.currentTimeMillis());
                        conn.setBusy(false);
                        continue;
                    }
                }
                Thread.sleep(1000L);
            } catch (java.net.SocketException e) {
                logger.error("error,断开链接" + conn.getName(), e);
                closeAll();
                break;
            } catch (java.io.IOException e) {
                logger.error("error,断开链接" + conn.getName(), e);
                closeAll();
                break;
            } catch (Exception e) {
                logger.error("error" + conn.getName(), e);
                closeAll();
                break;
            }
        }
    }
}
