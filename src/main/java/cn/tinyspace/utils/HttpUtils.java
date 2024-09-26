package cn.tinyspace.utils;

import cn.tinyspace.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class HttpUtils {

    public final static long BigFileSize = 10 * 1024 * 1024;
    public final static byte[] lineSplit = "\r\n".getBytes(StandardCharsets.UTF_8);
    public final static byte[] ZeroBytes = new byte[]{0x00, 0x00, 0x00, 0x00};
    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class);

    public static String readLine(InputStream in) throws IOException {
        byte[] bytes = readLineUsingByte(in);
        return (new String(bytes, StandardCharsets.UTF_8)).trim();
    }

    public static byte[] readLineUsingByte(InputStream in) throws IOException {
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        int loop = 0;
        int max = 4096;
        while (loop < max) {

            int b = in.read();
            if (b == 13) { //跳过\r
                tmp.write(b);
                continue;
            } else if (b == 10) { //b=10为 \n
                tmp.write(b);
                break;
            } else if (b == -1) { //b=10为 \n
                break;
            }
            tmp.write(b);
            loop++;
        }
        return tmp.toByteArray();
    }

    public static void replaceHost(List<String> headers, boolean forClient) {

        if (headers != null && headers.size() > 0) {
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i);
                String newHeader = header;
                if (forClient && header.startsWith("Host")) {
                    Set<String> oldSet = Config.HostMap.keySet();
                    for (String serverName : oldSet) {
                        newHeader = newHeader.replace(serverName, Config.HostMap.get(serverName));
                    }
                } else if (forClient && header.startsWith("Connection: keep-alive")) {
                    newHeader = "Connection: close";
                } else if (!forClient && header.startsWith("Location")) {
                    Set<String> oldSet = Config.HostMap.keySet();
                    for (String serverName : oldSet) {
                        newHeader = newHeader.replace(Config.HostMap.get(serverName), serverName);
                    }
                }
                if (!header.equals(newHeader)) {
                    headers.remove(i);
                    if (i == (headers.size() - 1)) {
                        headers.add(newHeader);
                    } else {
                        headers.add(i, newHeader);
                    }
                    break;
                }
            }
        }

    }

    /**
     * 在反向代理通道中，连接是不会关闭的，所以要明确的加上 Content-Lenght
     *
     * @param resp
     */
    public static void addContentLengthHeader(HttpObject resp) {
        //Content-Length
        if (resp == null || resp.getFirstLine() == null) {
            return;
        }
        if (resp.getHeaders() == null) {
            resp.setHeaders(new ArrayList<>());
        }
        long len;
        if (resp.getBodyFile() != null && resp.getBodyFile().exists()) {
            len = resp.getUnzipLen();
        } else {
            len = (resp.getBody() == null) ? 0 : resp.getBody().length;
        }
        List<String> headers = resp.getHeaders();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            if (header.startsWith("Content-Length")) {
                headers.remove(i);
                break;
            }
        }
        headers.add("Content-Length: " + len);
    }

    public static ByteArrayOutputStream readChunkedBody(InputStream in) throws IOException {

        long number = 0;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        while (true) {
            byte[] bytes = HttpUtils.readLineUsingByte(in);
            String headerSize = (new String(bytes, StandardCharsets.UTF_8)).trim();
            if (headerSize.length() == 0) {
                logger.error("读取到新的空行，这个不应该出现！！");
                continue;
            } else {
                number += (bytes.length);
            }
            out.write(bytes);

            int len = Integer.parseInt(headerSize, 16);
            if (len > 0) {
                byte[] trunked = BytesUtils.readBigLen(in, len);
                out.write(trunked);
                number += (trunked.length);
                bytes = HttpUtils.readLineUsingByte(in);
                String blankSize = (new String(bytes, StandardCharsets.UTF_8)).trim();
                if (blankSize != null && blankSize.length() > 0) {
                    logger.error("读取到新的非空行，这个不应该出现！！");
                }
                out.write(bytes);
                number += bytes.length;
            } else if (len == 0) { //长度为0，说明trunk结束
                bytes = HttpUtils.readLineUsingByte(in);
                String blankSize = (new String(bytes, StandardCharsets.UTF_8)).trim();
                out.write(bytes);
                number += bytes.length;
                if (blankSize != null && blankSize.length() > 0) {
                    logger.error("读取到新的非空行，这个不应该出现！！");
                }
                break;
            }
        }
        out.close();
        logger.debug("read readChunkedBody,len=" + number);
        return out;
    }

    public static HttpObject parseReuqestHttpObject(InputStream in) throws IOException {
        HttpObject httpObject = new HttpObject();
        String firstLine = null;
        List<String> headers = new ArrayList<>();
        byte[] body = null;
        File tmpFile = null;
        int numLine = 0;
        int dataLen = -1;
        while (true) {
            String header = null;
            if (firstLine == null) {
                firstLine = readLine(in);
                if (firstLine == null) {
                    break;
                }
                firstLine = firstLine.trim();
                logger.debug("first=" + firstLine);
                if (!firstLine.startsWith("POST") && !firstLine.startsWith("GET") && !firstLine.startsWith("OPTION")) {
                    if (!firstLine.equals("heart")) {
                        logger.info("收到心跳信息" + firstLine);
                    }
                    return httpObject;
                }
                continue;
            }

            if (numLine == 0) {
                header = readLine(in);
                logger.debug("header=" + header);
                if (header.length() > 0) {
                    headers.add(header);
                    if (header.startsWith("Content-Length")) {
                        try {
                            int pos = header.indexOf(":");
                            if (pos > 0) {
                                String val = header.substring(pos + 1).trim();
                                dataLen = Integer.parseInt(val);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (header.length() == 0) {
                    numLine = 1;
                    if (dataLen <= 0) {
                        break;
                    } else {
                        OutputStream tmp;
                        if (dataLen > BigFileSize) {
                            tmpFile = File.createTempFile("stc", ".gz");
                            httpObject.setBodyFile(tmpFile);
                            logger.info("use tmpFile as body:" + tmpFile.getAbsolutePath());
                            tmp = new GZIPOutputStream(new FileOutputStream(tmpFile));
                            httpObject.setUnzipLen(dataLen);
                        } else {
                            tmp = new ByteArrayOutputStream();
                        }
                        int total = 0;
                        while (true) {
                            byte[] tmpBody = new byte[1024];
                            int size = in.read(tmpBody, 0, 1024);
                            if (size == 0) {
                                break;
                            }
                            if (size < 0) {
                                throw new IOException("socket broken");
                            }
                            total = total + size;
                            tmp.write(tmpBody, 0, size);
                            if (total >= dataLen) {
                                break;
                            }
                        }
                        tmp.flush();
                        tmp.close();
                        if (tmp instanceof ByteArrayOutputStream) {
                            body = ((ByteArrayOutputStream) tmp).toByteArray();
                        }
                        break;
                    }
                }
            } else {
                logger.error("进入到奇怪的循环，目前numLine=" + numLine + ";dataLen=" + dataLen + ";firstLine=" + firstLine);
                try {
                    Thread.sleep(100);
                } catch (Exception x) {
                }
            }
        }
        httpObject.setFirstLine(firstLine);
        httpObject.setBody(body);
        httpObject.setHeaders(headers);
        httpObject.setDataLen(dataLen);
        httpObject.setBodyFile(tmpFile);
        return httpObject;
    }


    public static HttpObject parseResponseHttpObject(InputStream in) throws IOException {
        HttpObject httpObject = new HttpObject();
        String firstLine = null;
        List<String> headers = new ArrayList<>();
        byte[] body = null;
        int numLine = 0;
        int dataLen = -1;
        boolean isGzip = false;
        boolean trunked = false;
        boolean isConnectionClose = false;
        boolean isDebug = false;
        File tmpFile = null;
        while (true) {
            String header = null;
            if (firstLine == null) {
                firstLine = readLine(in);
                if (firstLine == null) {
                    break;
                }
                firstLine = firstLine.trim();
                if (firstLine.contains("HTTP/1.1 304")) {
                    isDebug = true;
                    dataLen = 0;
                }
                logger.info(firstLine);
                if (!firstLine.startsWith("HTTP")) {
                    if (!firstLine.equals("heart")) {
                        logger.info("收到心跳信息" + firstLine);
                    }
                    return httpObject;
                }
                continue;
            }

            if (numLine == 0) {
                header = readLine(in);
                if (isDebug && logger.isInfoEnabled()) {
                    logger.info(header);
                }
                if (header.length() > 0) {
                    headers.add(header);
                    if (header.startsWith("Content-Length")) {
                        try {
                            int pos = header.indexOf(":");
                            if (pos > 0) {
                                String val = header.substring(pos + 1).trim();
                                dataLen = Integer.parseInt(val);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else if (header.startsWith("Connection:") && header.contains("close")) {
                        isConnectionClose = true;
                    } else if (header.startsWith("Content-Encoding") && header.contains("gzip")) {
                        isGzip = true;
                        //Transfer-Encoding: chunked
                    } else if (header.startsWith("Transfer-Encoding") && header.contains("chunked")) {
                        trunked = true;
                    }
                } else if (header.length() == 0) {
                    numLine = 1;
                    if (dataLen == 0) {
                        break;
                    } else if (dataLen > 0) {//有明确的Content-Length
                        OutputStream tmp;
                        if (dataLen > BigFileSize) {
                            tmpFile = File.createTempFile("stc", ".gz");
                            httpObject.setBodyFile(tmpFile);
                            httpObject.setUnzipLen(dataLen);
                            logger.info("use tmpFile as body:" + tmpFile.getAbsolutePath() + ";unzipLen=" + dataLen);
                            tmp = new GZIPOutputStream(new FileOutputStream(tmpFile));
                        } else {
                            tmp = new ByteArrayOutputStream();
                        }
                        int total = 0;
                        while (true) {
                            byte[] tmpBody = new byte[1024];
                            int size = in.read(tmpBody, 0, 1024);
                            if (isDebug && logger.isInfoEnabled()) {
                                logger.info("read size:" + size);
                            }
                            if (size == 0) {
                                break;
                            }
                            if (size < 0) {
                                throw new IOException("socket broken");
                            }
                            total = total + size;
                            tmp.write(tmpBody, 0, size);
                            if (total >= dataLen) {
                                break;
                            }
                        }
                        tmp.flush();
                        tmp.close();
                        if (tmp instanceof ByteArrayOutputStream) {
                            body = ((ByteArrayOutputStream) tmp).toByteArray();
                        } else {
                            logger.info("use tmpFile as body:" + tmpFile.getAbsolutePath() + ";zipLen=" + tmpFile.length());
                        }
                        break;
                    } else if (trunked) { // 按照trunked读取
                        ByteArrayOutputStream trunkBody = readChunkedBody(in);
                        body = trunkBody.toByteArray();
                        break;
                    } else {
                        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
                        while (true) {
                            byte[] tmpBody = new byte[1024];
                            int size = in.read(tmpBody, 0, 1024);
                            if (isDebug && logger.isInfoEnabled()) {
                                logger.info("read size:" + size);
                            }
                            if (size < 0) {
                                break;
                            }
                            if (size > 0) {
                                tmp.write(tmpBody, 0, size);
                            }
                            //isConnectionClose表明服务器会主动关闭，需要等到size<0
                            if (in.available() == 0 && !isConnectionClose) {
                                break;
                            }
                        }
                        tmp.close();
                        body = tmp.toByteArray();
                        break;
                    }
                }
            } else {
                logger.info("进入到奇怪的循环，目前numLine=" + numLine + ";dataLen=" + dataLen);
                try {
                    Thread.sleep(100);
                } catch (Exception x) {
                }
            }
        }
        httpObject.setFirstLine(firstLine);
        httpObject.setBody(body);
        httpObject.setHeaders(headers);
        httpObject.setDataLen(dataLen);
        httpObject.setBodyFile(tmpFile);
        return httpObject;

    }


    public static HttpObject sendHttpRequest(HttpObject src, OutputStream out, InputStream in) throws IOException {
        HttpObject httpObject = new HttpObject();

        do {
            logger.debug(src.getFirstLine());
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            tmp.write(src.getFirstLine().getBytes(StandardCharsets.UTF_8));
            tmp.write(lineSplit);
            List<String> headers = src.getHeaders();
            if (headers != null && headers.size() > 0) {
                for (String s : headers) {
                    logger.debug(s);
                    tmp.write(s.getBytes(StandardCharsets.UTF_8));
                    tmp.write(lineSplit);
                }
            }
            tmp.close();
            out.write(tmp.toByteArray());
            File bodyFile = src.getBodyFile();
            if (bodyFile != null && bodyFile.exists()) {
                out.write(lineSplit);
                long fileSize = bodyFile.length();
                FileInputStream tmpIo = new FileInputStream(bodyFile);
                BytesUtils.copyData(tmpIo, out, fileSize);
                tmpIo.close();
            } else if (src.getBody() != null && src.getBody().length > 0) {
                out.write(lineSplit);
                out.write(src.getBody());
            } else {
                out.write(lineSplit);
            }
            out.flush();
            if (in != null) {
                httpObject = HttpUtils.parseResponseHttpObject(in);
            }
        } while (false);
        return httpObject;
    }


    public static HttpObject sendHttpRequestWithAes(HttpObject src, OutputStream out, InputStream in, byte[] password) throws IOException {
        HttpObject httpObject = new HttpObject();

        do {
            logger.debug(src.getFirstLine());
            AesHttpObject aesObj = new AesHttpObject(src, password);
            if (aesObj != null) {
                byte[] reqLen = BytesUtils.unsignedIntToBytes(1L * aesObj.getReq().length);
                out.write(reqLen);
                out.write(aesObj.getReq());
                out.write(aesObj.getTransFlag()); //0:no body,1:aes,2:gzip,3:bigfile
                File tmp = aesObj.getTmpFile();
                if (tmp != null && tmp.exists()) {
                    long fileSize = tmp.length();
                    byte[] bodyLen = BytesUtils.unsignedIntToBytes(fileSize);
                    out.write(bodyLen);
                    byte[] unzipLen = BytesUtils.unsignedIntToBytes(src.getUnzipLen());
                    out.write(unzipLen);
                    FileInputStream tmpIo = new FileInputStream(tmp);
                    BytesUtils.copyData(tmpIo, out, fileSize);
                    tmpIo.close();
                } else if (aesObj.getBody() != null && aesObj.getBody().length > 0) {
                    byte[] bodyLen = BytesUtils.unsignedIntToBytes(1L * aesObj.getBody().length);
                    out.write(bodyLen);
                    out.write(aesObj.getBody());
                }
                out.flush();
                if (in != null) {
                    httpObject = parseHttpObjectWithAes(in, password);
                }
            }


        } while (false);
        return httpObject;
    }

    public static HttpObject parseHttpObjectWithAes(InputStream in, byte[] password) throws IOException {

        HttpObject result = new HttpObject();
        byte[] reqLen = BytesUtils.readBigLen(in, 4);
        long len = BytesUtils.byteArrayToLong(reqLen);
        if (len == 0) {//receive heartbeat
            return null;
        }
        File tmpFile = null;
        byte[] req = BytesUtils.readBigLen(in, len);
        byte[] initReq = AES.decrypt(req, password);
        String request = new String(initReq, StandardCharsets.UTF_8);
        String[] datas = request.split("\n");
        if (datas != null && datas.length > 0) {
            result.setFirstLine(datas[0].trim());
            List<String> headers = new ArrayList<>();
            for (int i = 1; i < datas.length; i++) {
                headers.add(datas[i].trim());
            }
            result.setHeaders(headers);
        }
        int transFlag = in.read();
        if (transFlag == 0) {
            //0:no body,1:aes,2:gzip,3:bigfile
        } else {
            byte[] bodyLen = BytesUtils.readBigLen(in, 4);
            long len2 = BytesUtils.byteArrayToLong(bodyLen);
            if (len2 > 0) {
                if (transFlag == 1) {
                    byte[] body = BytesUtils.readBigLen(in, len2);
                    byte[] initBody = AES.decrypt(body, password);
                    result.setBody(initBody);

                } else if (transFlag == 2) {
                    byte[] body = BytesUtils.readBigLen(in, len2);
                    byte[] initBody = BytesUtils.uncompress(body);
                    result.setBody(initBody);
                } else if (transFlag == 3) {
                    byte[] unzipBodyLen = BytesUtils.readBigLen(in, 4);
                    long unziplen2 = BytesUtils.byteArrayToLong(unzipBodyLen);
                    tmpFile = File.createTempFile("cts", ".tmp");
                    logger.debug("use tmpFile as body:" + tmpFile.getAbsolutePath());
                    OutputStream tmpOut = new FileOutputStream(tmpFile);
                    GZIPInputStream gzipInputStream = new GZIPInputStream(in);
                    BytesUtils.copyData(gzipInputStream, tmpOut, unziplen2);
                    tmpOut.flush();
                    tmpOut.close();
                    result.setBodyFile(tmpFile);
                }
            }
        }
        return result;
    }

    public static boolean isWebSocket(List<String> headers) {
        if (headers != null && headers.size() > 0) {
            for (String header : headers) {
                if (header != null && header.toLowerCase().contains("websocket")) {
                    return true;
                }
            }
        }
        return false;
    }

}
