package cn.tinyspace.utils;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class AES {

    private static final byte[] ivs = new byte[]{51, 50, 57, 101, 98, 119, 54, 54, 99, 98, 55, 52, 55, 58, 97, 56};

    /**
     * <p>
     * Description: AES加密，使用默认模式
     * </p>
     *
     * @param content
     * @param password
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     * @throws NoSuchPaddingException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidKeyException
     * @throws NoSuchProviderException
     */
    public static byte[] encrypt(byte[] content, byte[] password) {

        try {
            SecretKeySpec skeySpec = new SecretKeySpec(password, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(ivs);
            cipher.init(1, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(content);
            return encrypted;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static byte[] decrypt(byte[] content, byte[] password) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(password, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec(ivs);
            cipher.init(2, skeySpec, iv);
            byte[] encrypted = cipher.doFinal(content);
            return encrypted;
        } catch (Exception exception) {
            exception.printStackTrace();
            return null;
        }
    }
}
