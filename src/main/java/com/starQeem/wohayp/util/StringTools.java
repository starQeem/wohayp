package com.starQeem.wohayp.util;


import org.apache.commons.lang3.RandomStringUtils;

/**
 * @Date: 2023/5/27 18:54
 * @author: Qeem
 */
public class StringTools {
    public static boolean isEmpty(String str) {

        if (null == str || "".equals(str) || "null".equals(str) || "\u0000".equals(str)) {
            return true;
        } else if ("".equals(str.trim())) {
            return true;
        }
        return false;
    }
    public static boolean pathIsOk(String path) {
        if (StringTools.isEmpty(path)) {
            return true;
        }
        if (path.contains("../") || path.contains("..\\")) {
            return false;
        }
        return true;
    }
    public static String rename(String fileName) {
        String fileNameReal = getFileNameNoSuffix(fileName);
        String suffix = getFileSuffix(fileName);
        return fileNameReal + "_" + getRandomString(5) + suffix;
    }
    public static String getFileSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return "";
        }
        String suffix = fileName.substring(index);
        return suffix;
    }


    public static String getFileNameNoSuffix(String fileName) {
        Integer index = fileName.lastIndexOf(".");
        if (index == -1) {
            return fileName;
        }
        fileName = fileName.substring(0, index);
        return fileName;
    }

    public static final String getRandomString(Integer count) {
        return RandomStringUtils.random(count, true, true);
    }

    /**
     * //将String数组arr转换成这种形式的字符串:1,2,3,4....
     *
     * @param arr 字符串数组
     * @return {@link String}
     */
    public static String Array2String(String[] arr){
        //将String数组pathArray转换成这种形式的字符串:1,2,3,4....
        StringBuilder sb = new StringBuilder();
        for (String item : arr) {
            sb.append(item).append(",");
        }
        String result = sb.toString();
        result = result.substring(0, result.length() - 1); // 移除最后一个逗号
        return result;
    }

}
