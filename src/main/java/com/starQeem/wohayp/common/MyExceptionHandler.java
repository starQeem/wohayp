package com.starQeem.wohayp.common;

import com.starQeem.wohayp.exception.BusinessException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @description: 自定义异常处理(处理BusinessException类型的异常)
 * @author: DT
 * @date: 2021/4/19 21:17
 * @version: v1.0
 */
@ControllerAdvice
public class MyExceptionHandler {


    @ExceptionHandler(value = BusinessException.class)
    @ResponseBody
    public ResultResponse businessExceptionHandler(BusinessException e){
        System.out.println("全局异常捕获>>>:"+e);
        ResultResponse response = new ResultResponse();
        response.setCode(500);
        if(null == e.getCode()){
            response.setCode(e.getCode());
        }
        response.setMessage(e.getMessage());
        return response;  //返回错误信息给前端
    }

    @ExceptionHandler(value =Exception.class)
    @ResponseBody
    public ResultResponse exceptionHandler(Exception e){
        System.out.println("全局异常捕获>>>:"+e);
        return ResultResponse.error(e.getMessage());  //返回错误信息给前端
    }
}
