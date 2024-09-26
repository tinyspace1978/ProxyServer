package cn.tinyspace.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class BytesUtils {

    public static final byte MAX_VALUE = (byte) 0xFF;
    private static final int UNSIGNED_MASK = 0xFF;
    static Logger logger = LoggerFactory.getLogger(BytesUtils.class);
    static char[] RandomLetters = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static byte[] generateRandomBytes(int size) {
        byte[] bytes = new byte[size];
        int len = RandomLetters.length;
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) (RandomLetters[random.nextInt(len)]);
        }
        return bytes;
    }


    public static byte[] reverseByte(byte[] byteLen) {
        byte[] byteRev = new byte[]{
                byteLen[3], byteLen[2], byteLen[1], byteLen[0]
        };
        return byteRev;
    }

    public static byte[] unsignedIntToBytes(long unsignedInt) {
        unsignedInt = unsignedInt & 0xFFFFFFFFL;
        return new byte[]{
                (byte) ((unsignedInt >> 24) & UNSIGNED_MASK),
                (byte) ((unsignedInt >> 16) & UNSIGNED_MASK),
                (byte) ((unsignedInt >> 8) & UNSIGNED_MASK),
                (byte) (unsignedInt & UNSIGNED_MASK)
        };
    }

    public static long byteArrayToLong(byte[] b) {
        return (((b[0] & UNSIGNED_MASK) << 24) |
                ((b[1] & UNSIGNED_MASK) << 16) |
                ((b[2] & UNSIGNED_MASK) << 8) |
                ((b[3] & UNSIGNED_MASK))
        ) & 0xFFFFFFFFL;
    }

    //BYTE* pDataIn, int nLenIn, BYTE* pDataOut, int nLenOut, const char* pSSS256

    /**
     * 因c++里 BYTE是unsigned int，范围为0~255
     * java里面的byte是-128~127。
     * 做加法和乘法的时候，需要先转成正整数int，再转为byte（保留单字节)
     */
    public static void encodeUserPass(byte[] pDataIn, int nLenIn, byte[] pDataOut, byte[] pSSS256) {
        int bySum = 0;
        int byJi = 3;
        int len = pSSS256.length;
        for (int i = 0; i < len; i++) {
            int sum = bySum + toUnsignedInt(pSSS256[i]);
            bySum = (sum & UNSIGNED_MASK);
            int by2 = (byJi * toUnsignedInt(pSSS256[i]));
            byJi = (by2 & UNSIGNED_MASK);

            if (byJi == 0) {
                byJi = 3;
            }
        }
        int[] bySSS = new int[256];
        for (int i = 0; i < len; i++) {
            bySSS[i] = pSSS256[i];
        }
        bySSS[len] = len;
        bySSS[len + 1] = bySum;
        bySSS[len + 2] = byJi;
        int nOffset = len;
        for (int i = 0; i < nLenIn; i++) {
            pDataOut[i] = (byte) toUnsignedInt((byte) (pDataIn[i] ^ ((bySSS[nOffset] / 2) + 1)));
            nOffset++;
            if (nOffset >= len + 3)
                nOffset = 0;
        }

    }

    public static int toUnsignedInt(byte value) {
        return value & UNSIGNED_MASK;
    }

    public static String formatByte(byte[] msg) {
        if (msg == null || msg.length == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer("[");
        for (int i = 0; i < msg.length; i++) {
            String s = Integer.toUnsignedString(Byte.toUnsignedInt(msg[i]), 16).toUpperCase();
            if (s.length() == 1) {
                s = "0" + s;
            }
            sb.append(s).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        try {
            sb.append("]");
        } catch (Exception e) {
        }
        return sb.toString();
    }


    public static byte[] readBigLen(InputStream in, long dataLen) {
        long total = 0;
        int step = (int) Math.min(dataLen, 2048);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            while (total < dataLen) {
                byte[] buffer = new byte[step];
                int size = in.read(buffer, 0, step);
                if (size > 0) {
                    outputStream.write(buffer, 0, size);
                    total += size;
                } else {
                    logger.error("错误！！size=" + size);
                }

                //logger.info("step=" + step + ",size=" + size + ",total=" + total);
                if (total >= dataLen || size < 0) {
                    break;
                }
                step = (int) Math.min(dataLen - total, 2048);
            }
        } catch (Exception e) {
            logger.error("read error", e);
        }
        return outputStream.toByteArray();
    }

    public static byte[] getStringContent(InputStream in, int len) {
        byte[] content = null;
        if (len > 0) {
            try {
                if (len == 255) {//0xFF
                    long dataLen = get4ByteInt(in, true);
                    return readBigLen(in, dataLen);
                } else {
                    content = new byte[len];
                    in.read(content, 0, len);
                }
            } catch (Exception e) {
                logger.error("read error", e);
            }
        }
        return content;
    }

    public static long get4ByteInt(InputStream in, boolean reverse) {
        byte[] byteLen = new byte[4];
        try {
            int len = in.read(byteLen, 0, 4);
            long dataLen;
            //logger.info("四字节的长度byte为" + BytesUtils.formatByte(byteLen));
            if (reverse) {
                byte[] byteRev = BytesUtils.reverseByte(byteLen);
                dataLen = BytesUtils.byteArrayToLong(byteRev);
            } else {
                dataLen = BytesUtils.byteArrayToLong(byteLen);
            }
            //logger.info("四字节dataLen:" + dataLen);
            return dataLen;
        } catch (Exception e) {
            logger.error("read error", e);
        }
        return 0;
    }

    public static long copyData(InputStream in, OutputStream outputStream, long dataLen) {

        long total = 0;
        int step = (int) Math.min(dataLen, 2048);
        try {
            while (total < dataLen) {
                byte[] buffer = new byte[step];
                int size = in.read(buffer, 0, step);
                if (size > 0) {
                    outputStream.write(buffer, 0, size);
                    total += size;
                } else {
                    logger.error("错误！！size=" + size);
                }
                if (total >= dataLen || size < 0) {
                    break;
                }
                step = (int) Math.min(dataLen - total, 2048);
            }
            outputStream.flush();
        } catch (Exception e) {
            logger.error("read error", e);
        }
        return total;
    }

    public static byte[] compress(byte[] plainBytes) {
        if (plainBytes == null || plainBytes.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPOutputStream gzip;
        try {
            gzip = new GZIPOutputStream(out);
            gzip.write(plainBytes);
            gzip.close();
        } catch (Exception e) {
            logger.error("compress error", e);
        }
        return out.toByteArray();
    }

    public static byte[] uncompress(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            GZIPInputStream ungzip = new GZIPInputStream(in);
            byte[] buffer = new byte[1024];
            int n;
            while ((n = ungzip.read(buffer)) > 0) {
                out.write(buffer, 0, n);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            logger.error("uncompress error", e);
        }
        return out.toByteArray();
    }
}
