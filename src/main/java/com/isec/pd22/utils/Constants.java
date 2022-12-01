package com.isec.pd22.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Constants {

    public static final String INITIAL_DB_BASE_URL= "databases/BaseDB/PD-2022-23-TP.db";
    public static final String BASE_URL_DB= "jdbc:sqlite:databases/";
    public static final String BASE_URL= "databases/";
    public static final String MULTICAST_IP = "239.39.39.30";
    public static final int MULTICAST_PORT = 4004;

    public static final String NAME_DEFAULT_DB = "PD-2022-23-TP.db";
    public static final String FILES_DIR_PATH= "files/";
    public static final String PICK_FILES_DIR_PATH= FILES_DIR_PATH;
    public static final long PAYMENT_TIMER = 10000; //miliseconds
    public static final long INITIAL_TIMEOUT = 30000;

    public static Date stringToDate(String date) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.parse(date);
    }
    public static String dateToString(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(date);
    }
}
