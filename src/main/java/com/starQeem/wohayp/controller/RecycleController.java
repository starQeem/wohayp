package com.starQeem.wohayp.controller;

import com.starQeem.wohayp.annotation.GlobalInterceptor;
import com.starQeem.wohayp.annotation.VerifyParam;
import com.starQeem.wohayp.entity.dto.UserDto;
import com.starQeem.wohayp.entity.enums.FileDelFlagEnums;
import com.starQeem.wohayp.entity.pojo.File;
import com.starQeem.wohayp.entity.query.FileInfoQuery;
import com.starQeem.wohayp.entity.vo.FileInfoVO;
import com.starQeem.wohayp.entity.vo.PaginationResultVO;
import com.starQeem.wohayp.entity.vo.ResponseVO;
import com.starQeem.wohayp.service.fileService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;


/**
 * @Date: 2023/6/3 18:48
 * @author: Qeem
 */
@RestController
@RequestMapping("recycle")
public class RecycleController extends ABaseController{
    @Resource
    private fileService fileService;

    /**
     * 回收站分页查询
     *
     * @param session  会话
     * @param query    查询
     * @param pageNo   页码
     * @param pageSize 一页的数据条数
     * @return {@link ResponseVO}
     */
    @RequestMapping("/loadRecycleList")
    @GlobalInterceptor
    public ResponseVO loadRecycleList(HttpSession session, FileInfoQuery query,Integer pageNo,Integer pageSize){
        UserDto user = (UserDto) session.getAttribute("user");
        query.setFileType(FileDelFlagEnums.RECYCLE.getFlag());
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        PaginationResultVO<File> result = fileService.pageFileList(Long.valueOf(user.getUserId()),query,true,false);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }

    /**
     * 恢复文件
     *
     * @param session 会话
     * @param fileIds 文件id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/recoverFile")
    @GlobalInterceptor
    public ResponseVO recoverFile(HttpSession session, @VerifyParam(required = true)String fileIds){
        UserDto userDto = (UserDto) session.getAttribute("user");
        fileService.recoverFileBatch(userDto.getUserId(),fileIds);
        return getSuccessResponseVO(null);
    }

    /**
     * 彻底删除
     *
     * @param session 会话
     * @param fileIds 文件id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/delFile")
    @GlobalInterceptor
    public ResponseVO delFile(HttpSession session,@VerifyParam(required = true)String fileIds){
        UserDto userDto = (UserDto) session.getAttribute("user");
        fileService.delFileBath(userDto.getUserId(),fileIds,false);
        return getSuccessResponseVO(null);
    }
}
