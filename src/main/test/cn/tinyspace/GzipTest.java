package cn.tinyspace;

import cn.tinyspace.utils.BytesUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class GzipTest {

    public static void main(String[] args) throws Exception {
        String plainText = "<CProject AppName=\"DataCenter\n" +
                "\" Name=\"机房环境监控系统\" ID=\"0\"\n" +
                "\tAlarmCount\t=\"14\"\n" +
                "\tAlarmCountBad\t=\"0\"\n" +
                "\tAlarmCountCommon\t=\"0\"\n" +
                "\tAlarmCountConfirmedToday\t=\"0\"\n" +
                "\tAlarmCountDead\t=\"0\"\n" +
                "\tAlarmCountEL\t=\"12\"\n" +
                "\tAlarmCountEmergency\t=\"11\"\n" +
                "\tAlarmCountPreAlarm\t=\"1\"\n" +
                "\tAlarmCountToday\t=\"12\"\n" +
                "\tAlarmLevel\t=\"6\"\n" +
                "\tAlarmLevelSetBad\t=\"8\"\n" +
                "\tAlarmLevelSetCommon\t=\"4\"\n" +
                "\tAlarmLevelSetDead\t=\"9\"\n" +
                "\tAlarmLevelSetEmergency\t=\"6\"\n" +
                "\tAlarmLevelSetPreAlarm\t=\"2\"\n" +
                "\tAlarmUid\t=\"6.8.1.1.\"\n" +
                "\tBigIpIsMaster\t=\"1\"\n" +
                "\tBkCycConfig\t=\"30\"\n" +
                "\tBkCycRunData\t=\"10\"\n" +
                "\tBkStorePath\t=\"/datas/\"";
        byte[] plainBytes = plainText.getBytes(StandardCharsets.UTF_8);
        int unCompressLen = plainBytes.length;

        File gzFile = new File("/Users/tinyspace/aaa.gz");

        //
        GZIPOutputStream zipout = new GZIPOutputStream(new FileOutputStream(gzFile));
        zipout.write(plainText.getBytes(StandardCharsets.UTF_8));
        zipout.close();
        System.out.println(gzFile.length());


        long compressLen = gzFile.length();
        System.out.println("unCompressLen=" + unCompressLen + ";compressLen=" + compressLen);

        //byte[] zipBuf = out.toByteArray();
        //System.out.println(BytesUtils.formatByte(zipBuf));

        ByteArrayOutputStream tmpOut = new ByteArrayOutputStream();
        GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(gzFile));


        BytesUtils.copyData(gzipInputStream, tmpOut, unCompressLen);
        tmpOut.close();

        System.out.println(";compressLen=" + compressLen);
        //System.out.println(BytesUtils.formatByte(buffer));
        System.out.println(new String(tmpOut.toByteArray(),"UTF-8"));


    }


}
