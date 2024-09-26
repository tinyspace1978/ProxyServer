package cn.tinyspace;

import cn.tinyspace.utils.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientServer extends ProcessConnection {
    Logger logger = LoggerFactory.getLogger(ClientServer.class);

    public ClientServer(int localPort, Socket s) {
        super(localPort, s);
    }

    public void run() {

        Socket socket = new Socket();
        try {
            socket.setSoTimeout(6000);
            InetAddress address = null;
            address = InetAddress.getByName(Config.ServerHost);
            super.setClientIp(Config.ServerHost);
            socket.connect(new InetSocketAddress(address, super.getLocalPort()));
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();

            super.setClient(socket);
            super.setIn(in);
            super.setOut(out);

            int len = in.read();
            byte[] step2 = null;
            if (len > 0) {
                step2 = BytesUtils.readBigLen(in, len);
            }
            String respStr = new String(step2);
            byte[] randomChars = respStr.getBytes(StandardCharsets.UTF_8);
            byte[] pSSS256 = randomChars;
            byte[] userBytes = Config.UserName.getBytes(StandardCharsets.UTF_8);
            byte[] passBytes = Config.UserToken.getBytes(StandardCharsets.UTF_8);
            byte[] encodeUserInfo = new byte[userBytes.length];
            byte[] encodePassInfo = new byte[passBytes.length];
            BytesUtils.encodeUserPass(userBytes, userBytes.length, encodeUserInfo, pSSS256);
            BytesUtils.encodeUserPass(passBytes, passBytes.length, encodePassInfo, pSSS256);
            out.write(encodeUserInfo.length);
            out.write(encodeUserInfo);
            out.write(encodePassInfo.length);
            out.write(encodePassInfo);
            int respLen = in.read();
            int respectLen = len + encodeUserInfo.length + encodePassInfo.length;
            if (respLen != respectLen) {
                logger.warn("error: length mismatch respLen=" + respLen + ";respect:" + (respectLen));
                super.close();
                return;
            }
            byte[] aesKey = BytesUtils.generateRandomBytes(32);
            super.setAesKey(aesKey);
            out.write(aesKey.length);
            out.write(aesKey);
            byte clientType = (byte) in.read();
            logger.debug("handshake completed:" + clientType + ", waiting for data from sever.");
            ConnectionManager.getInstance().getCachedExecutor().execute(new ClientProcessInput(this));
        } catch (Exception ee) {
            logger.error("error", ee);
            try {
                super.close();
            } catch (Exception e) {
            }
        } finally {

        }
    }
}
