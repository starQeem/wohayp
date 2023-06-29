package com.starQeem.wohayp.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * 用户
 * @Date: 2023/5/25 19:13
 * @author: Qeem
 * @date 2023/05/27
 */
@Data
public class user {
    @TableId
    private Long userId;  //用户id
    private String nickName;  //用户昵称
    private String password; //密码
    private String email;  //邮箱地址
    private String avatar;  //头像地址
    private Date joinTime;  //加入时间
    private Date lastLoginTime;  //最后登陆时间
    private Integer status;  //状态0禁用 1启用
    private Long useSpace; //使用空间单位
    private Long totalSpace;  //总空间
}
