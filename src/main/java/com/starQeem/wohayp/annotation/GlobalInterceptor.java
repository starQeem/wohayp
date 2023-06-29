package com.starQeem.wohayp.annotation;

import org.springframework.web.bind.annotation.Mapping;
import java.lang.annotation.*;

/**
 * @Date: 2023/5/27 18:00
 * @author: Qeem
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface GlobalInterceptor {
    /**
     * 检查参数
     *
     * @return boolean
     */
    boolean checkParams() default false;  //默认非必填

    /**
     * 检查登录
     *
     * @return boolean
     */
    boolean checkLogin() default true;   //默认需登录

    /**
     * 校验超级管理员
     *
     * @return boolean
     */
    boolean checkAdmin() default false;  //默认不是

}
