package cn.tinyspace;

import cn.tinyspace.utils.HttpObject;
import cn.tinyspace.utils.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.List;

public class ClientProcessInput extends BaseConnnection {

    Logger logger = LoggerFactory.getLogger(ClientProcessInput.class);

    public ClientProcessInput(ProcessConnection conn) {
        super(conn);
    }


    public void run() {
        try {
            MDC.put("remoteAddr", super.getClientIp());
            MDC.put("localPort", "" + super.getLocalPort());
        } catch (Exception e) {

        }

        InputStream in = super.getIn();
        ProcessConnection conn = super.getProcessConnection();
        boolean websocket = false;
        boolean errorOccur = false;

        while (true) {
            try {
                if (in.available() > 0) {
                    HttpObject object = HttpUtils.parseHttpObjectWithAes(in, conn.getAesKey());
                    if (object != null && object.getFirstLine() != null) {
                        if (conn != null) {
                            try {
                                String first = object.getFirstLine();
                                logger.debug("first:" + first);
                                String[] data = first.split("\\s+");
                                String host = null;
                                int port = 80;
                                boolean parseHostSucc = false;
                                if (data != null && data.length >= 2) {
                                    String uri = data[1];
                                    if (uri.startsWith("http")) {
                                        try {
                                            URL url = new URL(uri);
                                            host = url.getHost();
                                            port = url.getPort();
                                            parseHostSucc = true;
                                        } catch (Exception e) {
                                            logger.error("parse url error", e);
                                        }
                                    }
                                }
                                if (!parseHostSucc) {
                                    List<String> headers = object.getHeaders();
                                    for (String s : headers) {

                                        if (s.startsWith("Host")) {
                                            int p1 = s.indexOf(":");
                                            if (p1 > 0) {
                                                String val = s.substring(p1 + 1).trim();
                                                int p2 = val.indexOf(":");
                                                if (p2 > 0) {
                                                    host = val.substring(0, p2).trim();
                                                    port = Integer.parseInt(val.substring(p2 + 1));
                                                } else {
                                                    host = val.trim();
                                                }
                                                logger.info("parse Host=" + host + ";port=" + port);
                                            }
                                        }
                                    }
                                }
                                websocket = HttpUtils.isWebSocket(object.getHeaders());
                                try {
                                    Socket proxy = new Socket();
                                    proxy.setSoTimeout(6000);
                                    InetAddress address;
                                    address = InetAddress.getByName(host);
                                    proxy.connect(new InetSocketAddress(address, port));
                                    InputStream proxyin = proxy.getInputStream();
                                    OutputStream proxyout = proxy.getOutputStream();

                                    if (websocket) {
                                        HttpUtils.sendHttpRequest(object, proxyout, null);
                                        ConnectionManager.getInstance().getCachedExecutor().execute(new WebSocketDeal(in, proxyout, null));
                                        WebSocketBlock.runLoop(proxyin, conn.getOut());
                                        break;
                                    } else {
                                        HttpObject resp = HttpUtils.sendHttpRequest(object, proxyout, proxyin);
                                        try {
                                            proxyout.close();
                                            proxyin.close();
                                            proxy.close();
                                            if (object.getBodyFile() != null && object.getBodyFile().exists()) {
                                                object.getBodyFile().delete();
                                            }
                                        } catch (Exception ee) {
                                            logger.error("error", ee);
                                        }
                                        if (resp != null && resp.getFirstLine() != null) {
                                            HttpUtils.addContentLengthHeader(resp);
                                            logger.debug("send to conn.getOut:" + resp.getFirstLine());
                                            HttpUtils.sendHttpRequestWithAes(resp, conn.getOut(), null, conn.getAesKey());
                                            if (resp.getBodyFile() != null && resp.getBodyFile().exists()) {
                                                resp.getBodyFile().delete();
                                            }
                                        }
                                    }

                                } catch (Exception e2) {
                                    logger.error("error", e2);
                                }
                            } catch (Exception ee) {
                                logger.error("error", ee);
                            } finally {
                                conn.setBusy(false);
                            }
                            conn.setLastInTime(System.currentTimeMillis());
                        }
                    } else {
                        logger.debug("send heartbeat");
                        conn.getOut().write(HttpUtils.ZeroBytes);
                    }
                } else {
                    try {
                        Thread.sleep(100L);
                    } catch (Exception e) {
                        logger.error("error", e);
                    }
                }
            } catch (java.net.SocketException e) {
                logger.error("error,断开链接,需要重连", e);
                if (!websocket) {
                    closeAll();
                    break;
                }
            } catch (Exception e) {
                logger.error("error", e);
                if (!websocket) {
                    closeAll();
                    break;
                }
            }


        }
    }
}
