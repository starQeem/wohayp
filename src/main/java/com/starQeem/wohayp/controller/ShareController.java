package com.starQeem.wohayp.controller;

import com.starQeem.wohayp.annotation.GlobalInterceptor;
import com.starQeem.wohayp.annotation.VerifyParam;
import com.starQeem.wohayp.entity.dto.UserDto;
import com.starQeem.wohayp.entity.pojo.Share;
import com.starQeem.wohayp.entity.vo.PaginationResultVO;
import com.starQeem.wohayp.entity.vo.ResponseVO;
import com.starQeem.wohayp.service.shareService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import static com.starQeem.wohayp.util.Constants.ZERO;


/**
 * @Date: 2023/6/3 18:48
 * @author: Qeem
 */
@RestController
@RequestMapping("share")
public class ShareController extends ABaseController{
    @Resource
    private shareService shareService;

    /**
     * 获取分享文件列表
     *
     * @param session  会话
     * @param pageNo   当前页码
     * @param pageSize 一页的数据条数
     * @return {@link ResponseVO}
     */
    @RequestMapping("/loadShareList")
    @GlobalInterceptor
    public ResponseVO loadShareList(HttpSession session,Integer pageNo,Integer pageSize){
        UserDto user = (UserDto) session.getAttribute("user");
        PaginationResultVO<Share> result = shareService.pageFileList(Long.valueOf(user.getUserId()),pageNo,pageSize);
        return getSuccessResponseVO(result);
    }

    /**
     * 分享文件
     *
     * @param session   会话
     * @param code      代码
     * @param fileId    文件标识
     * @param validType 有效类型
     * @return {@link ResponseVO}
     */
    @RequestMapping("/shareFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO shareFile(HttpSession session,String code,
                                @VerifyParam(required = true)String fileId,
                                @VerifyParam(required = true)Integer validType){
        UserDto userDto = (UserDto) session.getAttribute("user");
        Share share = new Share();
        share.setUserId(Long.valueOf(userDto.getUserId()));
        share.setCode(code);
        share.setFileId(Long.valueOf(fileId));
        share.setValidType(validType);
        share.setView(ZERO);
        shareService.saveShare(share);
        return getSuccessResponseVO(share);
    }

    /**
     * 取消分享
     *
     * @param session  会话
     * @param shareIds 分享id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/cancelShare")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO cancelShare(HttpSession session, @VerifyParam(required = true)String shareIds){
        UserDto userDto = (UserDto) session.getAttribute("user");
        shareService.deleteFileShareBath(shareIds,Long.valueOf(userDto.getUserId()));
        return getSuccessResponseVO(null);
    }
}
