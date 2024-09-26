package cn.tinyspace;

import cn.tinyspace.utils.BytesUtils;
import cn.tinyspace.utils.HttpObject;
import cn.tinyspace.utils.HttpUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChunkedTest {
    public static void main(String[] args) throws Exception {
        try {
            Socket proxy = new Socket();
            proxy.setSoTimeout(6000);
            InetAddress address;
            address = InetAddress.getByName("monitor.tinyspace.cn");
            proxy.connect(new InetSocketAddress(address, 80));
            InputStream proxyin = proxy.getInputStream();
            OutputStream proxyout = proxy.getOutputStream();
            HttpObject object = new HttpObject();
            object.setFirstLine("GET http://monitor.tinyspace.cn/dashboard/vendor/datatables/dataTables.bs4-custom.css HTTP/1.1");
            List<String> headers = new ArrayList<>();
            headers.add("Host: monitor.tinyspace.cn");
            headers.add("Connection: keep-alive");
            headers.add("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36");
            headers.add("Accept: text/css,*/*;q=0.1");
            headers.add("Referer: http://monitor.tinyspace.cn/system/check");
            headers.add("Accept-Encoding: gzip, deflate");
            headers.add("Accept-Language: zh-CN,zh;q=0.9");
            headers.add("Cookie: Hm_lvt_b8c9dc063b87afe8061a100d3719ed46=1726317737,1726359924; LDBID=77f7f086-567a-4441-b6f3-a53d7be621f0");
            object.setHeaders(headers);
            HttpObject resp = HttpUtils.sendHttpRequest(object, proxyout, proxyin);
            try {
                proxyout.close();
                proxyin.close();
                proxy.close();
                if (object.getBodyFile() != null && object.getBodyFile().exists()) {
                    object.getBodyFile().delete();
                }
                System.out.println(Integer.toHexString(550));

                System.out.println(resp.getFirstLine());
                byte[] body = resp.getBody();
                System.out.println(BytesUtils.formatByte(body));
                int total = body.length;
                int number = 0;
                if (body != null && body.length > 0) {
                    InputStream in = new ByteArrayInputStream(body);
                    while (true) {
                        String headerSize = HttpUtils.readLine(in);
                        number += (2 + headerSize.length());
                        int len = Integer.parseInt(headerSize);
                        System.out.println("len=" + len);
                        byte[] trunked = new byte[len];
                        int size = in.read(trunked, 0, len);
                        number += 2 + size;
                        System.out.println("size=" + size);
                        if (len == 0) {
                            System.out.println(" number=" + number);
                            break;
                        }
                    }

                }
            } catch (Exception ee) {
                ee.printStackTrace();
            }

        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }
}

