package cn.tinyspace;

import cn.tinyspace.utils.BytesUtils;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class TestServer {
    public static void main(String[] args) {


        System.out.println(Long.toHexString(2248146944L));
        Random random = new Random();

        int rand = 1000000 + random.nextInt(800000);
        String str = Integer.toHexString(rand).toUpperCase();
        System.out.println("随机数为：" + str);
        //返回8个字符
        byte[] randomChars = str.getBytes(StandardCharsets.UTF_8);

        byte[] pSSS256 = new byte[randomChars.length];
        for (int i = 0; i < pSSS256.length; i++) {
            pSSS256[i] = randomChars[i];
        }

        byte[] userBytes = "666444".getBytes(StandardCharsets.UTF_8);
        byte[] passBytes = "333555".getBytes(StandardCharsets.UTF_8);
        int nLenIn = userBytes.length;
        byte[] encodeUserInfo = new byte[nLenIn];
        byte[] encodePassInfo = new byte[nLenIn];
        BytesUtils.encodeUserPass(userBytes, nLenIn, encodeUserInfo, pSSS256);
        BytesUtils.encodeUserPass(passBytes, nLenIn, encodePassInfo, pSSS256);
        System.out.println("发送的数据:" + encodeUserInfo.length + "\n" + BytesUtils.formatByte(encodeUserInfo));
        System.out.println("发送的数据:" + encodePassInfo.length + "\n" + BytesUtils.formatByte(encodePassInfo));

        //模拟服务器收到这个信息后，服务器再解码
        byte[] checkUserInfo = new byte[nLenIn];
        BytesUtils.encodeUserPass(encodeUserInfo, nLenIn, checkUserInfo, pSSS256);

        System.out.println("服务器收到用户名后再次解码为：" + BytesUtils.formatByte(checkUserInfo));
        byte[] checkPassInfo = new byte[nLenIn];
        BytesUtils.encodeUserPass(encodePassInfo, nLenIn, checkPassInfo, pSSS256);
        System.out.println("服务器收到密码后再次解码为：" + BytesUtils.formatByte(checkPassInfo));
    }
}
