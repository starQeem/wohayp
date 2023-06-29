package com.starQeem.wohayp.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.starQeem.wohayp.annotation.GlobalInterceptor;
import com.starQeem.wohayp.annotation.VerifyParam;
import com.starQeem.wohayp.entity.dto.SessionShareDto;
import com.starQeem.wohayp.entity.dto.UserDto;
import com.starQeem.wohayp.entity.enums.FileDelFlagEnums;
import com.starQeem.wohayp.entity.pojo.file;
import com.starQeem.wohayp.entity.pojo.share;
import com.starQeem.wohayp.entity.pojo.user;
import com.starQeem.wohayp.entity.query.FileInfoQuery;
import com.starQeem.wohayp.entity.vo.FileInfoVO;
import com.starQeem.wohayp.entity.vo.PaginationResultVO;
import com.starQeem.wohayp.entity.vo.ResponseVO;
import com.starQeem.wohayp.entity.vo.ShareInfoVO;
import com.starQeem.wohayp.exception.BusinessException;
import com.starQeem.wohayp.exception.ResponseCodeEnum;
import com.starQeem.wohayp.service.fileService;
import com.starQeem.wohayp.service.shareService;
import com.starQeem.wohayp.service.userService;
import com.starQeem.wohayp.util.CopyTools;
import com.starQeem.wohayp.util.StringTools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Date;

import static com.starQeem.wohayp.util.Constants.SESSION_SHARE_KEY;
import static com.starQeem.wohayp.util.Constants.ZERO_STRING;

/**
 * @Date: 2023/6/5 16:29
 * @author: Qeem
 * 外部分享
 */
@RestController
@RequestMapping("/showShare")
public class WebShareController extends CommonFileController {
    @Resource
    private fileService fileService;
    @Resource
    private shareService shareService;
    @Resource
    private userService userService;


    /**
     * 获取用户登录信息
     *
     * @param session 会话
     * @param shareId 分享id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/getShareLoginInfo")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO getShareLoginInfo(HttpSession session, @VerifyParam(required = true) String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(SESSION_SHARE_KEY + shareId);
        if (sessionShareDto == null) {
            return getSuccessResponseVO(null);
        }
        ShareInfoVO shareInfoVO = getShareInfoCommon(shareId);
        //判断是否是当前用户分享的文件
        UserDto userDto = (UserDto) session.getAttribute("user");
        if (userDto != null && userDto.getUserId().equals(sessionShareDto.getShareUserId().toString())) {
            shareInfoVO.setCurrentUser(true);
        } else {
            shareInfoVO.setCurrentUser(false);
        }
        return getSuccessResponseVO(shareInfoVO);
    }

    /**
     * 获取分享信息
     *
     * @param shareId 分享id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/getShareInfo")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO getShareInfo(@VerifyParam(required = true) String shareId) {
        return getSuccessResponseVO(getShareInfoCommon(shareId));
    }

    private ShareInfoVO getShareInfoCommon(String shareId) {
        QueryWrapper<share> shareQueryWrapper = new QueryWrapper<>();
        shareQueryWrapper.eq("share_id", shareId);
        share share = shareService.getBaseMapper().selectOne(shareQueryWrapper);
        if (share == null || share.getExpireTime() != null && new Date().after(share.getExpireTime())) { //判断分享链接是否存在,是否失效
            throw new BusinessException(ResponseCodeEnum.CODE_902.getMsg()); //分享链接不存在或者已经失效
        }
        ShareInfoVO shareInfoVO = CopyTools.copy(share, ShareInfoVO.class);
        //根据文件id和发布文件的用户名查询文件
        QueryWrapper<file> fileQueryWrapper = new QueryWrapper<>();
        fileQueryWrapper.eq("file_id", share.getFileId()).eq("user_id", share.getUserId());
        file file = fileService.getBaseMapper().selectOne(fileQueryWrapper);
        //判断查询出的文件是否存在,是否未被删除
        if (file == null || FileDelFlagEnums.RECYCLE.getFlag().equals(file.getDelFlag())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        //查询用户
        QueryWrapper<user> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.eq("user_id", share.getUserId());
        user user = userService.getBaseMapper().selectOne(userQueryWrapper);
        shareInfoVO.setFileName(file.getFileName());
        shareInfoVO.setNickName(user.getNickName());
        shareInfoVO.setAvatar(user.getAvatar());
        shareInfoVO.setUserId(user.getUserId());
        return shareInfoVO;
    }

    /**
     * 校验分享码
     *
     * @param session 会话
     * @param shareId 分享id
     * @param code    提取码
     * @return {@link ResponseVO}
     */
    @RequestMapping("/checkShareCode")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO checkShareCode(HttpSession session,
                                     @VerifyParam(required = true) String shareId,
                                     @VerifyParam(required = true) String code) {
        SessionShareDto sessionShareDto = shareService.checkShareCode(shareId, code);
        session.setAttribute(SESSION_SHARE_KEY + shareId, sessionShareDto);
        return getSuccessResponseVO(null);
    }

    /**
     * 获取分享文件列表
     *
     * @param session 会话
     * @param filePid 文件父级id
     * @param shareId 共享id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO loadFileList(HttpSession session, String filePid,
                                   @VerifyParam(required = true) String shareId) {
        SessionShareDto sessionShareDto = checkShare(session, shareId);
        FileInfoQuery query = new FileInfoQuery();
        if (!StringTools.isEmpty(filePid) && !ZERO_STRING.equals(filePid)) {
            fileService.checkFolderFilePid(sessionShareDto.getFileId().toString(), sessionShareDto.getShareUserId().toString(), filePid);
            query.setFilePid(filePid);
        } else {
            query.setFileId(sessionShareDto.getFileId().toString());
        }
        query.setFileType(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO<file> result = fileService.pageFileList(sessionShareDto.getShareUserId(), query, false, false);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    private SessionShareDto checkShare(HttpSession session, String shareId) {
        SessionShareDto sessionShareDto = (SessionShareDto) session.getAttribute(SESSION_SHARE_KEY + shareId);
        if (sessionShareDto == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_903);
        }
        if (sessionShareDto.getExpireTime() != null && new Date().after(sessionShareDto.getExpireTime())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
        return sessionShareDto;
    }

    /**
     * 获取文件目录
     *
     * @param session 会话
     * @param shareId 共享id
     * @param path    路径
     * @return {@link ResponseVO}
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO getFolderInfo(HttpSession session,
                                    @VerifyParam(required = true) String shareId,
                                    @VerifyParam(required = true) String path) {
        SessionShareDto shareSession = checkShare(session, shareId);
        return super.getFolderInfo(path, shareSession.getShareUserId().toString());
    }

    /**
     * 获取文件信息
     *
     * @param response 响应
     * @param session  会话
     * @param shareId  分享id
     * @param fileId   文件id
     */
    @RequestMapping("/getFile/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void getFile(HttpServletResponse response, HttpSession session,
                        @PathVariable("shareId") String shareId,
                        @PathVariable("fileId") String fileId) {
        SessionShareDto sessionShareDto = checkShare(session, shareId);
        super.getFile(response, fileId, sessionShareDto.getShareUserId().toString());
    }

    /**
     * 视频预览
     *
     * @param response 响应
     * @param session  会话
     * @param shareId  共享id
     * @param fileId   文件id
     */
    @RequestMapping("/ts/getVideoInfo/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void getFilePreview(HttpServletResponse response, HttpSession session,
                               @PathVariable("shareId") String shareId,
                               @PathVariable("fileId") String fileId) {
        SessionShareDto sessionShareDto = checkShare(session, shareId);
        super.getFile(response, fileId, sessionShareDto.getShareUserId().toString());
    }

    /**
     * 创建下载链接
     *
     * @param shareId 分享id
     * @param fileId  用户id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/createDownloadUrl/{shareId}/{fileId}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO createDownloadUrl(HttpSession session,
                                        @PathVariable("shareId") String shareId,
                                        @PathVariable("fileId") String fileId) {
        SessionShareDto sessionShareDto = checkShare(session, shareId);
        return super.createDownLoadUrl(fileId, sessionShareDto.getShareUserId().toString());
    }

    /**
     * 下载
     *
     * @param request  请求
     * @param response 响应
     * @param code     代码
     * @throws Exception 异常
     */
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @VerifyParam(required = true) @PathVariable("code") String code) throws Exception {
        super.commonDownload(request, response, code);
    }

    /**
     * 保存到我的网盘
     *
     * @param shareId      分享id
     * @param shareFileIds 分享的文件ID,多个逗号隔开
     * @param myFolderId   我的网盘目录ID
     */
    @RequestMapping("/saveShare")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO saveShare(HttpSession session,
                                @VerifyParam(required = true) String shareId,
                                @VerifyParam(required = true) String shareFileIds,
                                @VerifyParam(required = true) String myFolderId) {
        SessionShareDto sessionShareDto = checkShare(session, shareId);
        UserDto userDto = (UserDto) session.getAttribute("user");
        if (sessionShareDto.getShareUserId().toString().equals(userDto.getUserId())) {
            throw new BusinessException("无需保存自己分享的文件!");
        }
        fileService.saveShare(sessionShareDto.getFileId(), shareFileIds, myFolderId, sessionShareDto.getShareUserId(), userDto.getUserId());
        return getSuccessResponseVO(null);
    }


}
