package com.starQeem.wohayp.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.starQeem.wohayp.entity.pojo.file;
import com.starQeem.wohayp.entity.vo.PaginationResultVO;
import com.starQeem.wohayp.entity.vo.UserInfoVO;
import com.starQeem.wohayp.util.SnowflakeIdUtils;
import com.starQeem.wohayp.util.emailUtils;
import com.starQeem.wohayp.entity.dto.UserSpaceDto;
import com.starQeem.wohayp.entity.dto.UserDto;
import com.starQeem.wohayp.exception.BusinessException;
import com.starQeem.wohayp.mapper.fileMapper;
import com.starQeem.wohayp.mapper.userMapper;
import com.starQeem.wohayp.entity.pojo.user;
import com.starQeem.wohayp.service.userService;
import com.starQeem.wohayp.util.MD5Utils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import static com.starQeem.wohayp.util.Constants.*;

/**
 * @Date: 2023/5/25 19:16
 * @author: Qeem
 */
@Service
public class userServiceImpl extends ServiceImpl<userMapper, user> implements userService {
    @Resource
    private userService userService;
    @Resource
    private fileMapper fileMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private emailUtils sendEmailCodeUtils;
    /**
     * 生成验证码
     */
    @Override
    public void code(String email,Integer type) throws MessagingException {
        if (type == ZERO){ //注册
            QueryWrapper<user> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("user_id","email").eq("email",email);
            user user = userService.getBaseMapper().selectOne(queryWrapper);
            if (user != null){
                throw new BusinessException("发送失败!该邮箱已经注册过!");
            }else {
                //生成验证码
                String code = RandomUtil.randomNumbers(LENGTH_6);
                stringRedisTemplate.opsForValue().set(EMAIL_CODE + email, code,EMAIL_CODE_TTL, TimeUnit.SECONDS);
                //发送验证码
                sendEmailCodeUtils.sendVerificationCode(EMAIL_FORM,email, code);
            }
        }
        if (type == ONE){
            //找回密码
            QueryWrapper<user> queryWrapper = new QueryWrapper<>();
            queryWrapper.select("user_id","email").eq("email",email);
            user user = userService.getBaseMapper().selectOne(queryWrapper);
            if (user == null){
                throw new BusinessException("邮箱不存在,请输入正确的邮箱!");
            }else {
                //生成验证码
                String code = RandomUtil.randomNumbers(LENGTH_6);
                stringRedisTemplate.opsForValue().set(EMAIL_CODE + email,code,EMAIL_CODE_TTL,TimeUnit.SECONDS);
                //发送验证码
                sendEmailCodeUtils.sendVerificationCode(EMAIL_FORM,email,code);
            }
        }
    }

    /**
     * 注册
     *
     * @param user 用户
     */
    @Override
    public void register(user user,String emailCode) {
        //1.根据邮箱地址查询数据库
        QueryWrapper<user> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("user_id","email").eq("email",user.getEmail());
        user getUser = userService.getBaseMapper().selectOne(queryWrapper);
        //2.判断查询结果是否为空
        if (getUser != null){
            //不为空,邮箱已经被注册,抛出异常
            throw new BusinessException("注册失败!该邮箱已经注册过!");
        }else {
            //为空,查询邮箱验证码
            String code = stringRedisTemplate.opsForValue().get(EMAIL_CODE + user.getEmail());
            if (Objects.equals(code, emailCode)){//判断邮箱验证码是否正确
                SnowflakeIdUtils idUtils = new SnowflakeIdUtils();
                //邮箱验证码正确,将用户信息存入数据库完成注册
                user.setPassword(MD5Utils.code(user.getPassword())); //密码
                user.setTotalSpace(TOTAL_SPACE);
                user.setUseSpace(USE_SPACE);
                user.setStatus(ONE);
                user.setUserId(idUtils.nextId());
                user.setJoinTime(new Date());
                user.setLastLoginTime(new Date());
                boolean save = userService.save(user);
                if (!save){
                    throw new BusinessException("注册失败!");
                }
            }else {
                //邮箱验证码错误,抛出异常
                throw new BusinessException("验证码错误!");
            }

        }
    }

    /**
     * 登录
     *
     * @param email    邮箱地址
     * @param password 密码
     */
    @Override
    public UserDto login(String email, String password) {
        //1.根据邮箱地址查询数据库
        QueryWrapper<user> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("user_id","email","status","password","total_space").eq("email",email);
        user user = userService.getBaseMapper().selectOne(queryWrapper);
        //2.判断查询出来的是否为空
        if (user == null){//为空
            throw new BusinessException("用户名不存在,请先注册!");
        }
        if (!user.getPassword().equalsIgnoreCase(password)){//判断密码是否正确
            throw new BusinessException("密码错误!");
        }
        if (user.getStatus() == ZERO){ //判断账号是否被禁用(0禁用 1启用)
            throw new BusinessException("登录失败!账号已被禁用!");
        }
        //3.更新最后登录时间
        user updateUser = new user();
        updateUser.setUserId(user.getUserId());
        updateUser.setLastLoginTime(new Date());
        userService.updateById(updateUser);
        //4.给userDto设置值
        UserDto userDto = new UserDto();
        userDto.setNickName(user.getNickName());
        userDto.setUserId(user.getUserId().toString());
        //5.判断是否为超级管理员
        boolean exists = false;
        for (String adminEmail : ADMIN_EMAIL) {
            if (adminEmail.equals(email)) {
                exists = true;
                break;
            }
        }
        if (exists){
            //是超级管理员
            userDto.setAdmin(true);
        }else {
            //不是超级管理员
            userDto.setAdmin(false);
        }
        //6.用户空间
        Long useSpace = fileMapper.getUseSpace(user.getUserId());
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        userSpaceDto.setTotalSpace(user.getTotalSpace());
        userSpaceDto.setUseSpace(useSpace);
        stringRedisTemplate.opsForValue().set(USER_SPACE + user.getUserId(), JSONUtil.toJsonStr(userSpaceDto),USER_SPACE_TTL,TimeUnit.SECONDS);
        //7.返回userDto
        return userDto;
    }

    /**
     * 找回密码
     *
     * @param email     邮箱地址
     * @param password  密码
     * @param emailCode 邮箱验证码
     */
    @Override
    public void resetPwd(String email, String password, String emailCode) {
        //1.从redis中获取验证码
        String code = stringRedisTemplate.opsForValue().get(EMAIL_CODE + email);
        //2.判断验证码是否正确
        if (!emailCode.equalsIgnoreCase(code)){
            //错误,抛出异常
            throw new BusinessException("验证码错误!");
        }
        //3.根据邮箱地址查询数据库
        QueryWrapper<user> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("user_id","email","password").eq("email",email);
        user user = userService.getBaseMapper().selectOne(queryWrapper);
        //4.判断新密码与旧密码是否相同
        if (user.getPassword().equalsIgnoreCase(MD5Utils.code(password))){
            //相同,抛出异常
            throw new BusinessException("修改失败,请勿输入与修改前相同的密码!");
        }
        //5.不相同,修改密码
        user updateUser = new user();
        updateUser.setUserId(user.getUserId());
        updateUser.setEmail(email);
        updateUser.setPassword(MD5Utils.code(password));
        userService.updateById(updateUser);
    }

    /**
     * 获取用户头像
     *
     * @param userId 用户id
     * @return {@link UserDto}
     */
    @Override
    public user getAvatar(Long userId) {
        QueryWrapper<user> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("user_id","avatar").eq("user_id",userId);
        return userService.getBaseMapper().selectOne(queryWrapper);
    }

    /**
     * 修改密码
     *
     * @param userId   用户id
     * @param password 密码
     */
    @Override
    public void updatePasswordByUserId(Long userId, String password) {
        user user = new user();
        user.setUserId(userId);
        user.setPassword(MD5Utils.code(password));
        userService.updateById(user);
    }

    /**
     * 更新头像
     *
     * @param userId 用户id
     */
    @Override
    public void updateAvatar(Long userId) {
        user user = new user();
        user.setUserId(userId);
        user.setAvatar("");
        userService.updateById(user);
    }

    /**
     * 获取用户空间信息
     *
     * @param userId 用户id
     */
    @Override
    public UserSpaceDto getUserSpace(Long userId) {
        String userSpace = stringRedisTemplate.opsForValue().get(USER_SPACE + userId);
        UserSpaceDto userSpaceDto = JSONUtil.toBean(userSpace, UserSpaceDto.class);
        user user = new user();
        user.setUserId(userId);
        user.setUseSpace(userSpaceDto.getUseSpace());
        userService.updateById(user);
        return userSpaceDto;
    }

    /**
     * 获取用户列表
     *
     * @param nickNameFuzzy 尼克名字模糊
     * @param status        状态
     * @param pageNo        当前页码
     * @param pageSize      页面大小
     * @return {@link PaginationResultVO}<{@link UserInfoVO}>
     */
    @Override
    public PaginationResultVO<user> getUserList(String nickNameFuzzy, String status, Integer pageNo, Integer pageSize) {
        if (pageNo == null){
            pageNo = ONE;
        }
        if (pageSize == null){
            pageSize = PAGE_SIZE;
        }
        if (nickNameFuzzy == null){
            nickNameFuzzy = "";
        }
        PageHelper.startPage(pageNo,pageSize);
        PageHelper.orderBy("last_login_time desc");
        QueryWrapper<user> queryWrapper = new QueryWrapper<>();
        if (status != null){
            queryWrapper.eq("status",status);
        }
        queryWrapper.like("nick_name",nickNameFuzzy);
        List<user> list = userService.getBaseMapper().selectList(queryWrapper);
        PageInfo<user> pageInfo = new PageInfo<>(list);
        return new PaginationResultVO<>(Math.toIntExact(pageInfo.getTotal()), pageInfo.getPageSize(), pageInfo.getPageNum(), pageInfo.getPages(), list);
    }

    /**
     * 修改用户状态
     *
     * @param userId 用户id
     * @param status  状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, String status) {
        //修改用户状态
        user user = new user();
        user.setUserId(userId);
        user.setStatus(Integer.parseInt(status));
        if (status.equals(ZERO_STRING)){  //如果是禁用,则删除该用户的所有文件
            QueryWrapper<file> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id",userId);
            fileMapper.delete(queryWrapper);
            //把使用空间和总空间都设置为0
            user.setTotalSpace(STOP_TOTAL_SPACE);
            user.setUseSpace(USE_SPACE);
        }
        userService.updateById(user);
    }

    /**
     * 修改用户空间
     *
     * @param userId      用户id
     * @param changeSpace 变化空间
     */
    @Override
    public void updateUserSpace(Long userId, String changeSpace) {
        QueryWrapper<user> selectQueryWrapper = new QueryWrapper<>();
        selectQueryWrapper.select("user_id","use_space").eq("user_id",userId);
        user user = userService.getBaseMapper().selectOne(selectQueryWrapper);
        if (user.getUseSpace() > Long.parseLong(changeSpace)){
            throw new BusinessException("修改的用户空间大小小于用户已经使用的空间大小,修改失败!");
        }
        user.setTotalSpace(Long.valueOf(changeSpace));
        userService.updateById(user);
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        userSpaceDto.setUseSpace(user.getUseSpace());
        userSpaceDto.setTotalSpace(Long.valueOf(changeSpace));
        stringRedisTemplate.opsForValue().set(USER_SPACE + userId,JSONUtil.toJsonStr(userSpaceDto));//顺便修改redis,用户就不用重新登录也能生效了
    }

}
