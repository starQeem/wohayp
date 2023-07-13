package com.starQeem.wohayp.common;

import com.alibaba.fastjson.JSONObject;
import com.starQeem.wohayp.exception.BusinessException;

/**
 * @description: 自定义数据传输
 * @author: DT
 * @date: 2021/4/19 21:47
 * @version: v1.0
 */
public class ResultResponse {
    /**
     * 响应代码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应结果
     */
    private Object result;

    public ResultResponse() {
    }

    public ResultResponse(BusinessException errorInfo) {
        this.code = errorInfo.getCode();
        this.message = errorInfo.getMessage();
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    /**
     * 成功
     *
     * @return
     */
   /* public static ResultResponse success() {
        return success(null);
    }
*/
    /**
     * 成功
     * @param data
     * @return
     */
    /*public static ResultResponse success(Object data) {
        ResultResponse rb = new ResultResponse();
        rb.setCode(BusinessException.SUCCESS.getResultCode());
        rb.setMessage(BusinessException.SUCCESS.getResultMsg());
        rb.setResult(data);
        return rb;
    }*/

    /**
     * 失败
     */
    public static ResultResponse error(BusinessException errorInfo) {
        ResultResponse rb = new ResultResponse();
        rb.setCode(errorInfo.getCode());
        rb.setMessage(errorInfo.getMessage());
        rb.setResult(null);
        return rb;
    }

    /**
     * 失败
     */
    public static ResultResponse error(Integer code, String message) {
        ResultResponse rb = new ResultResponse();
        rb.setCode(code);
        rb.setMessage(message);
        rb.setResult(null);
        return rb;
    }

    /**
     * 失败
     */
    public static ResultResponse error( String message) {
        ResultResponse rb = new ResultResponse();
        rb.setCode(500);
        rb.setMessage(message);
        rb.setResult(null);
        return rb;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

}
