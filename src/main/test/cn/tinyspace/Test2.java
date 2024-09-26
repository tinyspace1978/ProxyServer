package cn.tinyspace;

import cn.tinyspace.utils.BytesUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Test2 {

    static Logger logger = LoggerFactory.getLogger(Test2.class);

    public static void main(String[] args) throws Exception {

        String uid = "a\r\nb";

        byte[] tmp = uid.getBytes();
        for (int i = 0; i < tmp.length; i++) {
            System.out.print(tmp[i] + ",");
        }

        byte[] uidLen = BytesUtils.reverseByte(BytesUtils.unsignedIntToBytes(uid.length() * 1L));
        System.out.println(BytesUtils.formatByte(uidLen));

        if (uid.length() > 0) {
            System.out.println(BytesUtils.formatByte(uid.getBytes()));
        }

        String propName = "V";//""CurveDataOf1:Group=0;Uid=6,2,1,1,1,;Min=1724515200000;Max=1724601599000;TimeType=7;DtTm1=20240825;DtTm2=20240825";
        int len = propName.length();
        byte[] propLen = BytesUtils.reverseByte(BytesUtils.unsignedIntToBytes(len * 1L));
        System.out.println("send propLen:" + len + BytesUtils.formatByte(propLen));
        System.out.println("send propName:" + propName + BytesUtils.formatByte(propName.getBytes()));
        byte[] unzipBytes = new byte[]{
                (byte) 0x01, (byte) 0x31, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xA3, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1F, (byte) 0x8B, (byte) 0x08, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x3B, (byte) 0xBF, (byte) 0xF5, (byte) 0xEC, (byte) 0xE6, (byte) 0x4B, (byte) 0x6F, (byte) 0x36, (byte) 0x2F, (byte) 0x5E, (byte) 0xBC, (byte) 0x66, (byte) 0xE3,
                (byte) 0x8A, (byte) 0x7D, (byte) 0xEB, (byte) 0x77, (byte) 0x7C, (byte) 0x3B, (byte) 0xF5, (byte) 0xD7, (byte) 0xCA, (byte) 0xD0, (byte) 0x50, (byte) 0x61, (byte) 0xD3, (byte) 0xFE, (byte) 0xED, (byte) 0xD7,
                (byte) 0x40, (byte) 0xFC, (byte) 0x0B, (byte) 0x47, (byte) 0xCF, (byte) 0x2F, (byte) 0xB2, (byte) 0xB2, (byte) 0x49, (byte) 0x2A, (byte) 0xB2, (byte) 0xF3, (byte) 0xC9, (byte) 0xCC, (byte) 0x2B, (byte) 0xAD,
                (byte) 0x38, (byte) 0x0F, (byte) 0x54, (byte) 0xBB, (byte) 0xE7, (byte) 0xC1, (byte) 0xFE, (byte) 0x1B, (byte) 0x7A, (byte) 0x56, (byte) 0x0A, (byte) 0x67, (byte) 0x57, (byte) 0x5C, (byte) 0xDC, (byte) 0x76,
                (byte) 0xED, (byte) 0xC2, (byte) 0xB6, (byte) 0xF3, (byte) 0x20, (byte) 0x39, (byte) 0xA3, (byte) 0xC5, (byte) 0x8B, (byte) 0xCF, (byte) 0x1D, (byte) 0x3A, (byte) 0xB5, (byte) 0x6A, (byte) 0xDB, (byte) 0x09,
                (byte) 0x34, (byte) 0x71, (byte) 0x34, (byte) 0xEE, (byte) 0xEE, (byte) 0x55, (byte) 0xE7, (byte) 0x56, (byte) 0x85, (byte) 0x06, (byte) 0x04, (byte) 0xA3, (byte) 0x89, (byte) 0x5E, (byte) 0xDB, (byte) 0x71,
                (byte) 0xEE, (byte) 0xD8, (byte) 0xE1, (byte) 0xA3, (byte) 0x7B, (byte) 0x7F, (byte) 0xD3, (byte) 0x42, (byte) 0x18, (byte) 0xE4, (byte) 0xD6, (byte) 0x93, (byte) 0x2F, (byte) 0x36, (byte) 0xEE, (byte) 0xB8,
                (byte) 0x72, (byte) 0xEB, (byte) 0xFC, (byte) 0xFD, (byte) 0x3D, (byte) 0x6F, (byte) 0x36, (byte) 0x3D, (byte) 0x02, (byte) 0x4A, (byte) 0xEB, (byte) 0xEA, (byte) 0x6E, (byte) 0xFF, (byte) 0x77, (byte) 0xEE,
                (byte) 0xE3, (byte) 0xB1, (byte) 0xEF, (byte) 0x86, (byte) 0x0A, (byte) 0x07, (byte) 0xD7, (byte) 0xEC, (byte) 0xBD, (byte) 0x7C, (byte) 0x6A, (byte) 0xF9, (byte) 0x86, (byte) 0x3B, (byte) 0x20, (byte) 0x85,
                (byte) 0xA4, (byte) 0x88, (byte) 0x02, (byte) 0x00, (byte) 0x3A, (byte) 0xF0, (byte) 0x1C, (byte) 0x30, (byte) 0x31, (byte) 0x01, (byte) 0x00, (byte) 0x00
        };
        System.out.println(unzipBytes.length);
        InputStream in = new ByteArrayInputStream(unzipBytes);
        int packageFlag = in.read();
        if (packageFlag == 1) {
            long unCompressLen = BytesUtils.get4ByteInt(in, true);
            long zipCompressLen = BytesUtils.get4ByteInt(in, true);
            logger.info("unCompressLen=" + unCompressLen + ",zipCompressLen=" + zipCompressLen);
        }


//        byte[] zipBytes=new byte[]{
//                0x1F, (byte)0x8B,0x08,0,0,0,
//                0,0,0, 0x0A, 0x63, 0x64, 0x64, 0x60, 0x0F, (byte)0xAE, 0x2C, 0x0E ,(byte)0xC9, (byte)0xCC, 0x4D, 0x05,
//                0, 0x50, (byte)0xD1, (byte)0x8E, 0x21, 0x0B, 0,0,0
//        };
//        System.out.println(BytesUtils.formatByte(zipBytes));
//        System.out.println(new String(ParseUtils.uncompress(zipBytes)));
//        byte[] dot = ".".getBytes("GB18030");
//        System.out.println("dot="+ dot[0]+BytesUtils.formatByte(dot));

//        zipBytes=new byte[]{
//                0x1F, (byte)0x8B,0x08,0,0,0,
//                0,0,0, 0x0A, 0x63, 0x64, 0x64, 0x60, 0x0F, (byte)0xAE, 0x2C, 0x0E ,(byte)0xC9, (byte)0xCC, 0x4D, 0x65,
//                0x44, 0x30, (byte)0x01, 0x16, 0x51, (byte)0xA8, 0x72,0x16,0,0,0
//        };
//
//        System.out.println(new String(ParseUtils.uncompress(zipBytes)));
//
//
//
//        byte flag=0x02;
//        byte zipflag = 0x01;
//        byte[] zipLen =new byte[]{ 0x1F,0,0,0};
//        byte[] unzipLen =new byte[]{0x0B,0,0,0};
//        //02
//        //01
//        //1F 00 00 00
//        //0B 00 00 00
//        //1F 8B 08 00 00 00 00 00 00 0A 63 64 64 60  0F AE 2C 0E C9 CC 4D 05 00 50 D1 8E 21 0B 00 00 00                        .P..!... .
//
//        zipBytes =new byte[]{
//                0x1F, (byte)0x8B, 0x08, 0, 0, 0, 0, 0, 0, 0x0A, 0x63, 0x64, 0x64, 0x60,  0x0F,
//                (byte)0xAE, 0x2C ,0x0E, (byte)0xC9, (byte)0xCC, 0x4D, 0x05, 0x00, 0x50, (byte)0xD1, (byte)0x8E, 0x21,
//                0x0B, 0x00, 0x00, 0x00
//        };
//        System.out.println("zipLenRev="+ BytesUtils.byteArrayToLong(BytesUtils.reverseByte(zipLen)));
//        System.out.println( zipBytes.length);
//        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(zipBytes));
//        byte[] buffer = new byte[gzipInputStream.available()];
//        gzipInputStream.read(buffer, 0, gzipInputStream.available());
//        System.out.println("组包 解压后 字符串为:" + new String(buffer, "GB18030"));
//
//
//        //0x01,0x01,0x01,0x02,0x01,0x1B,0x00,0x00,0x00,0x07,0x00,0x00,0x00,0x1F,0x8B,0x08,
//        //        0x00,0x00,0x00,0x00,0x00,0x00,0x0A,0x63,0x64,0x64,0x60,0x8E,0xF0,0xF5,0x01,0x00,
//        //        0x7A,0xEB,0xE0,0xBA,0x07,0x00,0x00,0x00
//
//        zipBytes =new byte[]{
//                0x1F,(byte)0x8B,0x08,
//                        0x00,0x00,0x00,0x00,0x00,0x00,0x0A,0x63,0x64,0x64,0x60,(byte)0x8E,(byte)0xF0,(byte)0xF5,0x01,0x00,
//                        0x7A,(byte)0xEB,(byte)0xE0,(byte)0xBA,0x07,0x00,0x00,0x00
//        };
//        System.out.println("zipLenRev="+ BytesUtils.byteArrayToLong(BytesUtils.reverseByte(zipLen)));
//        System.out.println( zipBytes.length);
//        gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(zipBytes));
//        buffer = new byte[1024];
//        int len = gzipInputStream.read(buffer, 0, 1024);
//        byte[] realBytes =new byte[len];
//        System.arraycopy(buffer,0,realBytes,0,len);
//
//        System.out.println("组包 解压后 字符串为:" + new String(realBytes, "GB18030")+";len="+len);
//
//        byte[] byteRev = new byte[]{
//                0,2,-70,-62
//        };
//
//        long dataLen = BytesUtils.byteArrayToLong(byteRev);
//        System.out.println("dataLen="+dataLen);
    }
}
