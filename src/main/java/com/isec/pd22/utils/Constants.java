package com.isec.pd22.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Constants {

    public static final String INITIAL_DB_BASE_URL= "src/main/resources/databases/BaseDB/PD-2022-23-TP.db";
    public static final String BASE_URL_DB= "jdbc:sqlite:src/main/resources/databases/";
    public static final String BASE_URL= "src/main/resources/databases/";
    public static final String MULTICAST_IP = "239.39.39.38";
    public static final int MULTICAST_PORT = 4004;

    public static final String NAME_DEFAULT_DB = "PD-2022-23-TP.db";
    public static final String FILES_DIR_PATH= "src/main/resources/files/";
    public static final long PAYMENT_TIMER = 10000; //miliseconds

    public static Date stringToDate(String date) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
        return dateFormat.parse(date);
    }
    public static String dateToString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
        return dateFormat.format(date);
    }
}
