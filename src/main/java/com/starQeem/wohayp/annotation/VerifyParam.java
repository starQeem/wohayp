package com.starQeem.wohayp.annotation;

import com.starQeem.wohayp.enums.VerifyRegexEnum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Date: 2023/5/27 18:06
 * @author: Qeem
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER,ElementType.FIELD})
public @interface VerifyParam {
    int min() default -1;
    int max() default -1;
    boolean required() default false;
    //正则校验(默认不校验)
    VerifyRegexEnum regex() default VerifyRegexEnum.NO;
}
