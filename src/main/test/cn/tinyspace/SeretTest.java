package cn.tinyspace;

import cn.tinyspace.utils.AES;
import cn.tinyspace.utils.BytesUtils;

import java.nio.charset.StandardCharsets;

public class SeretTest {

    public static void main(String[] args) {
        byte[] aesKey = BytesUtils.generateRandomBytes(32);
        String initStr = "沙发拉快速减肥啦睡觉多放辣三大军阀";
        AES aes = new AES();
        byte[] encrypt = AES.encrypt(initStr.getBytes(StandardCharsets.UTF_8), aesKey);
        byte[] decrypt = AES.decrypt(encrypt, aesKey);
        System.out.println(new String(decrypt, StandardCharsets.UTF_8));
    }
}
