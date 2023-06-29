package com.starQeem.wohayp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.starQeem.wohayp.entity.dto.UserDto;
import com.starQeem.wohayp.entity.dto.UserSpaceDto;
import com.starQeem.wohayp.entity.pojo.user;
import com.starQeem.wohayp.entity.vo.PaginationResultVO;
import com.starQeem.wohayp.entity.vo.UserInfoVO;

import javax.mail.MessagingException;

/**
 * @Date: 2023/5/25 19:16
 * @author: Qeem
 */
public interface userService extends IService<user> {
    /**
     * 生成验证码
     *
     * @return int
     */
    void code(String email,Integer type) throws MessagingException;

    /**
     * 注册
     *
     * @param user 用户
     */
    void register(user user,String emailCode);

    /**
     * 登录
     *
     * @param email    邮箱地址
     * @param password 密码
     */
    UserDto login(String email, String password);

    /**
     * 找回密码
     *
     * @param email     邮箱地址
     * @param password  密码
     * @param emailCode 邮箱验证码
     */
    void resetPwd(String email, String password, String emailCode);

    /**
     * 获取用户头像
     *
     * @param userId 用户id
     * @return {@link UserDto}
     */
    user getAvatar(Long userId);

    /**
     * 修改用户密码
     *
     * @param userId   用户id
     * @param password 密码
     */
    void updatePasswordByUserId(Long userId, String password);

    /**
     * 更新头像
     *
     * @param userId 用户id
     */
    void updateAvatar(Long userId);

    /**
     * 获取用户空间信息
     *
     * @param userId 用户id
     */
    UserSpaceDto getUserSpace(Long userId);

    /**
     * 获取用户列表
     *
     * @param nickNameFuzzy 尼克名字模糊
     * @param status        状态
     * @param pageNo        当前页码
     * @param pageSize      一页的数据条数
     * @return {@link PaginationResultVO}<{@link UserInfoVO}>
     */
    PaginationResultVO<user> getUserList(String nickNameFuzzy, String status, Integer pageNo, Integer pageSize);

    /**
     * 修改用户状态
     *
     * @param userId 用户id
     * @param status  状态
     */
    void updateUserStatus(Long userId, String status);

    /**
     * 修改用户空间
     *
     * @param userId     用户id
     * @param changeSpace 变化空间
     */
    void updateUserSpace(Long userId, String changeSpace);
}
