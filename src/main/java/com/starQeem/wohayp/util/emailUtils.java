package com.starQeem.wohayp.util;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.mail.MessagingException;

/**
 * @Date: 2023/5/7 0:32
 * @author: Qeem
 */
@Component
public class emailUtils {
    @Resource
    private JavaMailSender javaMailSender;
    public void sendVerificationCode(String form,String to, String code) throws MessagingException {
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom(form);   //发送邮件的邮箱号
        simpleMailMessage.setTo(to);   //接收邮件的邮箱号
        simpleMailMessage.setText("[喔哈云盘] 验证码:"+code+"(验证码5分钟内有效)。请勿将验证码告诉他人哦!");   //邮件内容
        simpleMailMessage.setSubject("喔哈云盘");   //邮件标题
        javaMailSender.send(simpleMailMessage);  //发送
    }
}
