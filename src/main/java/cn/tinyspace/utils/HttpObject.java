package cn.tinyspace.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HttpObject {
    String firstLine = null;
    List<String> headers = new ArrayList<>();
    byte[] body = null;
    File bodyFile;//当body内容超过10M时，使用bodyFile
    int numLine = 0;
    int dataLen = 0;
    long unzipLen = 0;
    boolean isPost = false;
    byte transFlag;

    public long getUnzipLen() {
        return unzipLen;
    }

    public void setUnzipLen(long unzipLen) {
        this.unzipLen = unzipLen;
    }

    public byte getTransFlag() {
        return transFlag;
    }

    public void setTransFlag(byte transFlag) {
        this.transFlag = transFlag;
    }

    public String getFirstLine() {
        return firstLine;
    }

    public void setFirstLine(String firstLine) {
        this.firstLine = firstLine;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public int getNumLine() {
        return numLine;
    }

    public void setNumLine(int numLine) {
        this.numLine = numLine;
    }

    public int getDataLen() {
        return dataLen;
    }

    public void setDataLen(int dataLen) {
        this.dataLen = dataLen;
    }

    public boolean isPost() {
        return isPost;
    }

    public void setPost(boolean post) {
        isPost = post;
    }

    public File getBodyFile() {
        return bodyFile;
    }

    public void setBodyFile(File bodyFile) {
        this.bodyFile = bodyFile;
    }
}
