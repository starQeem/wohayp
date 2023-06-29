package com.starQeem.wohayp.controller;

import com.starQeem.wohayp.annotation.GlobalInterceptor;
import com.starQeem.wohayp.annotation.VerifyParam;
import com.starQeem.wohayp.entity.pojo.file;
import com.starQeem.wohayp.entity.pojo.user;
import com.starQeem.wohayp.entity.query.FileInfoQuery;
import com.starQeem.wohayp.entity.vo.PaginationResultVO;
import com.starQeem.wohayp.entity.vo.ResponseVO;
import com.starQeem.wohayp.entity.vo.UserInfoVO;
import com.starQeem.wohayp.service.fileService;
import com.starQeem.wohayp.service.userService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * @Date: 2023/6/3 18:48
 * @author: Qeem
 */
@RestController
@RequestMapping("admin")
public class AdminController extends CommonFileController{
    @Resource
    private userService userService;
    @Resource
    private fileService fileService;

    /**
     * 加载用户列表
     *
     * @param nickNameFuzzy 模糊搜索用户昵称
     * @param status        状态
     * @param pageNo        当前页码
     * @param pageSize      一页的数据条数
     * @return {@link ResponseVO}
     */
    @RequestMapping("/loadUserList")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO loadUserList(String nickNameFuzzy,String status,Integer pageNo,Integer pageSize){
        PaginationResultVO<user> resultVO = userService.getUserList(nickNameFuzzy,status,pageNo,pageSize);
        return getSuccessResponseVO(convert2PaginationVO(resultVO, UserInfoVO.class));
    }

    /**
     * 修改用户状态
     *
     * @param userId 用户id
     * @param status 状态
     * @return {@link ResponseVO}
     */
    @RequestMapping("/updateUserStatus")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO updateUserStatus(@VerifyParam(required = true)String userId,@VerifyParam(required = true)String status){
       userService.updateUserStatus(Long.valueOf(userId),status);
       return getSuccessResponseVO(null);
    }

    /**
     * 修改用户空间
     *
     * @param userId      用户id
     * @param changeSpace 变化空间
     * @return {@link ResponseVO}
     */
    @RequestMapping("/updateUserSpace")
    @GlobalInterceptor(checkAdmin = true,checkParams = true)
    public ResponseVO updateUserSpace(@VerifyParam(required = true)String userId,@VerifyParam(required = true)String changeSpace){
        userService.updateUserSpace(Long.valueOf(userId),changeSpace);
        return getSuccessResponseVO(null);
    }


    /**
     * 获取文件列表
     *
     * @param query 查询
     * @return {@link ResponseVO}
     */
    @RequestMapping("/loadFileList")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO loadFileList(FileInfoQuery query){
        PaginationResultVO<file> result = fileService.pageFileList(null,query,false,true);
        return getSuccessResponseVO(result);
    }

    /**
     * 获取文件夹信息
     *
     * @param path 路径
     * @return {@link ResponseVO}
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO getFolderInfo(@VerifyParam(required = true) String path){
        return super.getFolderInfo(path,null);
    }

    /**
     * 获取文件信息
     *
     * @param response 响应
     * @param userId   用户id
     * @param fileId   文件id
     */
    @RequestMapping("/getFile/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public void getFile(HttpServletResponse response,
                              @PathVariable("userId")String userId,
                              @PathVariable("fileId")String fileId){
        super.getFile(response,fileId,userId);
    }

    /**
     * 视频预览
     *
     * @param response 响应
     * @param fileId   文件标识
     * @param userId   用户id
     */
    @RequestMapping("/ts/getVideoInfo/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public void getFilePreview(HttpServletResponse response,
                               @PathVariable("userId")String userId,
                               @PathVariable("fileId") String fileId){
        super.getFile(response,fileId,userId);
    }

    /**
     * 创建下载链接
     *
     * @param userId 用户id
     * @param fileId 用户id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/createDownloadUrl/{userId}/{fileId}")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO createDownloadUrl(@PathVariable("userId")String userId,
                                        @PathVariable("fileId")String fileId){
        return super.createDownLoadUrl(fileId,userId);
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
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public void download(HttpServletRequest request, HttpServletResponse response,
                         @VerifyParam(required = true)@PathVariable("code")String code) throws Exception {
        super.commonDownload(request,response,code);
    }

    /**
     * 删除文件
     *
     * @param fileIdAndUserIds 文件id和用户id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true,checkAdmin = true)
    public ResponseVO delFile(@VerifyParam(required = true)String fileIdAndUserIds){
        String[] fileIdAndUserIdArray = fileIdAndUserIds.split(",");
        for (String fileIdAndUserId : fileIdAndUserIdArray) {
            String[] itemArray = fileIdAndUserId.split("_");
            fileService.delFileBath(itemArray[0],itemArray[1],true);
        }
        return getSuccessResponseVO(null);
    }

}
