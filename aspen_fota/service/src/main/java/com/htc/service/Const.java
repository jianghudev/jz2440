package com.htc.service;

/**
 * Created by hubin_jiang on 2018/5/23.
 */

public class Const {
    final static String G_TAG = "ChirpFota";

    public static final int CMD_FOTA_START = 'A';
    public static final int CMD_FOTA_TRANSFER = 'B';
    public static final int CMD_FOTA_END = 'C';
    public static final int CMD_FOTA_QUERY = 'D';
    public static final int CMD_FOTA_RESULT = 'E';

    public static final int FOTA_TYPE_CCG4=0;
    public static final int FOTA_TYPE_TP=1;
    public static final int FOTA_TYPE_FACEP_RF=2;
    public static final int FOTA_TYPE_FACEP_SYS=3;
    public static final int FOTA_TYPE_CTRL_RF=4;
    public static final int FOTA_TYPE_CTRL_SYS=5;
    public static final int FOTA_TYPE_CTRL_BL1=6;


}
