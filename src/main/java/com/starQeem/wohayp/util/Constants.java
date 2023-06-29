package com.starQeem.wohayp.util;


/**
 * @Date: 2023/5/25 21:45
 * @author: Qeem
 */
public class Constants {
    public final static String ADMIN_EMAIL[] = {"2572277647@qq.com"};//超级管理员账号
    public final static String CHECK_CODE_KEY = "check_code_key";  //图片验证码
    public final static String CHECK_CODE_KEY_EMAIL = "check_code_key_email"; //邮箱图片验证码
    public final static String EMAIL_CODE = "wohayp:emailUtils:code"; //邮箱验证码
    public final static String EMAIL_FORM = "2572277647@qq.com";  //发送验证码的邮箱地址
    public final static String USER_SPACE = "wohayp:user:space:"; //用户空间
    public final static String USER_FILE_TEMP_SIZE = "wohayp:user:file:temp:"; //临时文件
    public final static String DOWN_LOAD = "wohayp:download:"; //下载链接
    public final static String SESSION_SHARE_KEY = "session_share_key_";
    public final static int EMAIL_CODE_TTL = 60*5; //邮箱验证码过期时间
    public final static int USER_SPACE_TTL = 24*60*60; //用户空间过期时间
    public final static int USER_FILE_TIME_TTL = 60*60;//临时文件过期时间
    public final static int DOWN_LOAD_TTL = 60*10; //下载链接过期时间
    public final static String FILE = "d:/wohaypFile/"; //windows目录
//    public final static String FILE = "/wohaypFile/"; //Linux目录
    public final static String FILE_FOLDER_FILE = "file/"; //分目录
    public final static String FILE_FOLDER_AVATAR_NAME = "avatar/"; //用户头像目录
    public final static String AVATAR_DEFAULT = "default_avatar"; //用户默认头像名
    public final static String AVATAR_SUFFIX = ".jpg"; //头像图片后缀
    public final static String IMAGE_PNG_SUFFIX = ".png";
    public final static String FILE_FOLDER_TEMP = "temp/";
    public final static String TS_NAME = "index.ts";
    public final static String M3U8_NAME = "index.m3u8";
    public final static int ONE = 1; //类型1
    public final static int ZERO = 0; //类型0
    public final static String ZERO_STRING = "0";
    public final static int LENGTH_5 = 5;
    public final static int LENGTH_6 = 6;
    public final static int LENGTH_10 = 10;
    public final static int LENGTH_15 = 15;
    public final static int LENGTH_150 = 150;
    public final static int LENGTH_50 = 50;
    public final static int PAGE_SIZE = 10;
    public final static int RECYCLE_TTL = 10; //回收站过期时间
    public final static Long RANDOM_MIN = 1000000000L;
    public final static Long RANDOM_MAX = 9999999999L;
    public final static Long USE_SPACE = 0L; //用户初始使用空间
    public final static Long STOP_TOTAL_SPACE = 0L; //用户初始使用空间
    public final static Long TOTAL_SPACE = 5000000000L; //用户初始使用总空间
}
