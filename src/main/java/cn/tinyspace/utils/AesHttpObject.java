package cn.tinyspace.utils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static cn.tinyspace.utils.HttpUtils.lineSplit;

public class AesHttpObject {
    byte[] req;
    byte[] body;
    byte transFlag; //0:no body,1:aes,2:gzip,3:bigfile

    File tmpFile;

    public AesHttpObject(HttpObject src, byte[] aesKey) {
        try {
            ByteArrayOutputStream tmp = new ByteArrayOutputStream();
            tmp.write(src.getFirstLine().getBytes(StandardCharsets.UTF_8));
            tmp.write(lineSplit);
            List<String> headers = src.getHeaders();
            if (headers != null && headers.size() > 0) {
                for (String s : headers) {
                    tmp.write(s.getBytes(StandardCharsets.UTF_8));
                    tmp.write(lineSplit);
                }
            }
            tmp.close();
            this.req = AES.encrypt(tmp.toByteArray(), aesKey);


            if (src.getBodyFile() != null) {
                setTmpFile(src.getBodyFile());
                this.transFlag = 0x03;
            } else {
                if (src.getBody() == null || src.getBody().length == 0) {
                    this.transFlag = 0x00;
                } else if (src.getBody().length < 5 * 1024 * 1024) {
                    this.body = AES.encrypt(src.getBody(), aesKey);
                    this.transFlag = 0x01;
                } else {
                    this.body = BytesUtils.compress(src.getBody());
                    this.transFlag = 0x02;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getReq() {
        return req;
    }

    public void setReq(byte[] req) {
        this.req = req;
    }

    public byte getTransFlag() {
        return transFlag;
    }

    public void setTransFlag(byte transFlag) {
        this.transFlag = transFlag;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public File getTmpFile() {
        return tmpFile;
    }

    public void setTmpFile(File tmpFile) {
        this.tmpFile = tmpFile;
    }

    public HttpObject convertToHttpObject(byte[] aesKey) {
        if (this.req != null && this.req.length > 0) {
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();

                byte[] tmp = AES.decrypt(this.req, aesKey);
                if (tmp != null && tmp.length > 0) {
                    out.write(tmp);
                }

                if (this.body != null && this.body.length > 0) {
                    byte[] initbody = AES.decrypt(this.body, aesKey);
                    if (initbody != null && initbody.length > 0) {
                        out.write(lineSplit);
                        out.write(initbody);
                    }
                }
                out.flush();
                out.close();
                InputStream inputStream = new ByteArrayInputStream(out.toByteArray());
                return HttpUtils.parseReuqestHttpObject(inputStream);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }


}
