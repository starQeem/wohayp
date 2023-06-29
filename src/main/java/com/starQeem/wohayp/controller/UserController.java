package com.starQeem.wohayp.controller;


import com.starQeem.wohayp.annotation.GlobalInterceptor;
import com.starQeem.wohayp.annotation.VerifyParam;
import com.starQeem.wohayp.entity.dto.CreateImageCode;
import com.starQeem.wohayp.entity.dto.UserSpaceDto;
import com.starQeem.wohayp.entity.dto.UserDto;
import com.starQeem.wohayp.entity.vo.ResponseVO;
import com.starQeem.wohayp.enums.VerifyRegexEnum;
import com.starQeem.wohayp.exception.BusinessException;
import com.starQeem.wohayp.entity.pojo.user;
import com.starQeem.wohayp.service.userService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import static com.starQeem.wohayp.util.Constants.*;

/**
 * @Date: 2023/5/25 13:29
 * @author: Qeem
 */
@RestController
@CrossOrigin
public class UserController extends ABaseController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";
    @Resource
    private userService userService;

    /**
     * 生成图片验证码
     *
     * @param response 响应
     * @param session  会话
     * @param type     0:登录注册  1:邮箱验证码发送  默认0
     */
    @RequestMapping(value = "/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException {
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    /**
     * 发送邮箱验证码
     *
     * @param email     邮箱地址
     * @param checkCode 图片验证码
     * @param type      类型 0:注册 1:找回密码
     */
    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO sendEmailCode(HttpSession session,
                                    @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,
                                    @VerifyParam(required = true) String checkCode,
                                    @VerifyParam(required = true) Integer type)
            throws BusinessException, MessagingException {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(CHECK_CODE_KEY_EMAIL))) {//判断邮箱验证码是否正确
                //不正确
                throw new BusinessException("图片验证码不正确!");
            }
            userService.code(email, type);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(CHECK_CODE_KEY_EMAIL);
        }
    }

    /**
     * 注册
     *
     * @param session   会话
     * @param user      用户
     * @param checkCode 图片验证码
     * @param emailCode 邮箱验证码
     * @throws BusinessException 业务异常
     */
    @RequestMapping("/register")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO register(HttpSession session, user user,
                               @VerifyParam(required = true) String checkCode,
                               @VerifyParam(required = true) String emailCode)
            throws BusinessException {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(CHECK_CODE_KEY))) {//判断验证码是否正确
                //不正确
                throw new BusinessException("图片验证码不正确!");
            }
            userService.register(user, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(CHECK_CODE_KEY);
        }
    }

    /**
     * 登录
     *
     * @param session   会话
     * @param checkCode 验证码
     * @param email     邮箱地址
     * @param password  密码
     * @return {@link ResponseVO}
     */
    @RequestMapping("/login")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO login(HttpSession session,
                            @VerifyParam(required = true) String checkCode,
                            @VerifyParam(required = true) String email,
                            @VerifyParam(required = true) String password) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(CHECK_CODE_KEY))) {
                //不正确
                throw new BusinessException("图片验证码错误!");
            }
            UserDto userDto = userService.login(email, password);
            if (userDto != null) {
                session.setAttribute("user", userDto);
            }
            return getSuccessResponseVO(userDto);
        } finally {
            session.removeAttribute(CHECK_CODE_KEY);
        }
    }

    /**
     * 找回密码
     *
     * @param session   会话
     * @param email     邮箱地址
     * @param checkCode 验证码
     * @param password  密码
     * @param emailCode 邮箱验证码
     * @return {@link ResponseVO}
     */
    @RequestMapping("/resetPwd")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public ResponseVO resetPwd(HttpSession session,
                               @VerifyParam(required = true) String email,
                               @VerifyParam(required = true) String checkCode,
                               @VerifyParam(required = true) String password,
                               @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(CHECK_CODE_KEY))) {
                //不正确
                throw new BusinessException("图片验证码错误!");
            }
            userService.resetPwd(email, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(CHECK_CODE_KEY);
        }
    }
    /**
     * 注销
     *
     * @param session 会话
     */
    @RequestMapping("logout")
    public ResponseVO logout(HttpSession session) {
        session.invalidate();
        return getSuccessResponseVO(null);
    }

    /**
     * 获取用户头像
     */
    @RequestMapping("/getAvatar/{userId}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public void getAvatar(HttpServletResponse response,@VerifyParam(required = true) @PathVariable("userId") String userId) {
            String avatarFolderName = FILE_FOLDER_FILE + FILE_FOLDER_AVATAR_NAME;
            File folder = new File(FILE + avatarFolderName);
            if (!folder.exists()) {//判断目录是否存在
                //目录不存在,创建目录
                folder.mkdirs();
            }
            String avatarPath = FILE + avatarFolderName + userId + AVATAR_SUFFIX;
            File file = new File(avatarPath);
            if (!file.exists()) {//判断该用户是否有头像
                //没有
                if (!new File(FILE + avatarFolderName + AVATAR_DEFAULT + AVATAR_SUFFIX).exists()) {//判断默认头像是否存在
                    //不存在
                    printNoDefaultImage(response);
                    return;
                }
                //存在
                avatarPath = FILE + avatarFolderName + AVATAR_DEFAULT +AVATAR_SUFFIX;
            }
            //有
            response.setContentType("image/jpg");
            readFile(response, avatarPath);
    }

    /**
     * 更新用户头像
     *
     * @param session 会话
     * @param avatar  头像
     * @return {@link ResponseVO}
     */
    @RequestMapping("/updateUserAvatar")
    @GlobalInterceptor
    public ResponseVO updateUserAvatar(HttpSession session, MultipartFile avatar){
        UserDto userDto = (UserDto) session.getAttribute("user");
        String baseFolder = FILE + FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()){
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + userDto.getUserId() + AVATAR_SUFFIX);
        try {
            avatar.transferTo(targetFile);
        }catch (Exception e){
            logger.error("上传头像失败!",e);
        }
        userService.updateAvatar(Long.valueOf(userDto.getUserId()));
        userDto.setAvatar(null);
        session.setAttribute("user",userDto);
        return getSuccessResponseVO(null);
    }
    private void printNoDefaultImage(HttpServletResponse response) {
        response.setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
            writer.close();
        } catch (Exception e) {
            logger.error("输出无默认图失败", e);
        } finally {
            writer.close();
        }
    }

    /**
     * 获取用户信息
     *
     * @param session 会话
     * @return {@link ResponseVO}
     */
    @RequestMapping("/getUserInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getUserInfo(HttpSession session){
        UserDto userDto = (UserDto) session.getAttribute("user");
        return getSuccessResponseVO(userDto);
    }

    /**
     * 获取用户空间信息
     *
     * @param session 会话
     * @return {@link ResponseVO}
     */
    @RequestMapping("/getUseSpace")
    @GlobalInterceptor
    public ResponseVO getUseSpace(HttpSession session){
        UserDto userDto = (UserDto) session.getAttribute("user");
        UserSpaceDto userSpaceDto = userService.getUserSpace(Long.valueOf(userDto.getUserId()));
        return getSuccessResponseVO(userSpaceDto);
    }

    /**
     * 更新密码
     *
     * @param session  会话
     * @param password 密码
     * @return {@link ResponseVO}
     */
    @RequestMapping("/updatePassword")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO updatePassword(HttpSession session,
                                     @VerifyParam(required = true,regex = VerifyRegexEnum.PASSWORD,min = 8,max = 18)
                                     String password){
        UserDto userDto = (UserDto) session.getAttribute("user");
        userService.updatePasswordByUserId(Long.valueOf(userDto.getUserId()),password);
        return getSuccessResponseVO(null);
    }

}
