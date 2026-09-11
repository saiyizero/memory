package com.murong.ecp.tools.fx.infrastructure.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MrDateUtils {

    public static String toShortTime(String time) {
        // 输入 yyyy-MM-dd HH:mm:ss，输出 MMdd HH:mm:ss
        if (time == null || time.length() < 19) return time;
        try {
            DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");
            LocalDateTime dt = LocalDateTime.parse(time, inputFmt);
            return dt.format(outputFmt);
        } catch (Exception e) {
            return time;
        }
    }

    public static String getCurrentTime(){
        LocalDateTime localNow = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return localNow.format(formatter);
    }

    public static String getCurrentTimeLongStr(){
        LocalDateTime localNow = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
        return localNow.format(formatter);
    }

    public static String getCurrentShortTime(){
        LocalDateTime localNow = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HHmmss");
        return localNow.format(formatter);
    }

    public static String getCurrentMonth(){
        LocalDateTime localNow = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM");
        return localNow.format(formatter);
    }

    public static String getCurrentDay(){
        LocalDateTime localNow = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd");
        return localNow.format(formatter);
    }

    public static String getCurrentDate(){
        LocalDateTime localNow = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        return localNow.format(formatter);
    }
}
