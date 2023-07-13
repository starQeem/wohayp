package com.starQeem.wohayp.controller;

import com.starQeem.wohayp.annotation.GlobalInterceptor;
import com.starQeem.wohayp.annotation.VerifyParam;
import com.starQeem.wohayp.entity.dto.UploadResultDto;
import com.starQeem.wohayp.entity.dto.UserDto;
import com.starQeem.wohayp.entity.enums.FileCategoryEnums;
import com.starQeem.wohayp.entity.enums.FileDelFlagEnums;
import com.starQeem.wohayp.entity.pojo.File;
import com.starQeem.wohayp.entity.query.FileInfoQuery;
import com.starQeem.wohayp.entity.vo.FileInfoVO;
import com.starQeem.wohayp.entity.vo.PaginationResultVO;
import com.starQeem.wohayp.entity.vo.ResponseVO;
import com.starQeem.wohayp.service.fileService;
import com.starQeem.wohayp.util.CopyTools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * @Date: 2023/5/29 20:09
 * @author: Qeem
 */
@RestController
@RequestMapping("file")
public class FileController extends CommonFileController{
    @Resource
    private fileService fileService;

    /**
     * 文件信息分页列表
     *
     * @param session  会话
     * @param query    查询
     * @param category 类别
     * @return {@link ResponseVO}
     */
    @RequestMapping("/loadDataList")
    @GlobalInterceptor
    public ResponseVO loadDataList(HttpSession session, FileInfoQuery query,String category){
        FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
        if (null != categoryEnum) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        query.setFileType(FileDelFlagEnums.USING.getFlag());
        UserDto user = (UserDto) session.getAttribute("user");
        PaginationResultVO<File> result = fileService.pageFileList(Long.valueOf(user.getUserId()),query,false,false);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfoVO.class));
    }
    /**
     * 上传文件
     *
     * @param session    会话
     * @param fileId     文件标识
     * @param file       文件
     * @param fileName   文件名称
     * @param filePid    文件pid
     * @param fileMd5    文件md5
     * @param chunkIndex 块索引
     * @param chunks     块
     * @return {@link ResponseVO}
     */
    @RequestMapping("/uploadFile")
    @GlobalInterceptor
    public ResponseVO uploadFile(HttpSession session, String fileId, MultipartFile file,
                                 @VerifyParam(required = true)String fileName,
                                 @VerifyParam(required = true)String filePid,
                                 @VerifyParam(required = true)String fileMd5,
                                 @VerifyParam(required = true)Integer chunks,
                                 @VerifyParam(required = true)Integer chunkIndex
                                 ){
        UserDto userDto = (UserDto) session.getAttribute("user");
        UploadResultDto uploadResultDto = fileService.uploadFile(userDto, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
        return getSuccessResponseVO(uploadResultDto);
    }
    /**
     * 图片缩略图显示和视频封面显示
     *
     * @param response    响应
     * @param imageFolder 图片文件夹
     * @param imageName   图片名字
     */
    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response,
                         @PathVariable("imageFolder")String imageFolder, @PathVariable("imageName")String imageName){
        super.getImage(response,imageFolder,imageName);
    }
    /**
     * 视频预览
     *
     * @param response 响应
     * @param fileId   文件标识
     */
    @RequestMapping("/ts/getVideoInfo/{fileId}")
    public void getFilePreview(HttpSession session,HttpServletResponse response,@PathVariable("fileId") String fileId){
        UserDto userDto = (UserDto) session.getAttribute("user");
        super.getFile(response,fileId,userDto.getUserId());
    }
    /**
     * 其他文件预览
     *
     * @param session  会话
     * @param response 响应
     * @param fileId   文件标识
     */
    @RequestMapping("/getFile/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public void getFile(HttpSession session,HttpServletResponse response,@PathVariable("fileId") String fileId){
        UserDto userDto = (UserDto) session.getAttribute("user");
        super.getFile(response,fileId,userDto.getUserId());
    }
    /**
     * 新建目录
     *
     * @param session  会话
     * @param filePid  父级id
     * @param fileName 文件名称
     * @return {@link ResponseVO}
     */
    @RequestMapping("/newFoloder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO newFolder(HttpSession session,
                                @VerifyParam(required = true)String filePid,
                                @VerifyParam(required = true)String fileName){
        UserDto userDto = (UserDto) session.getAttribute("user");
        File file = fileService.newFolder(filePid, userDto.getUserId(), fileName);
        return getSuccessResponseVO(CopyTools.copy(file, FileInfoVO.class));
    }
    /**
     * 获取当前目录
     *
     * @param path 路径
     * @return {@link ResponseVO}
     */
    @RequestMapping("/getFolderInfo")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO getFolderInfo(HttpSession session,@VerifyParam(required = true)String path){
        UserDto userDto = (UserDto) session.getAttribute("user");
        return super.getFolderInfo(path,userDto.getUserId());
    }
    /**
     * 重命名
     *
     * @param session  会话
     * @param fileId   文件标识
     * @param fileName 文件名称
     * @return {@link ResponseVO}
     */
    @RequestMapping("/rename")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO rename(HttpSession session,
                             @VerifyParam(required = true)String fileId,
                             @VerifyParam(required = true)String fileName){
        UserDto userDto = (UserDto) session.getAttribute("user");
        File file = fileService.rename(fileId,userDto.getUserId(),fileName);
        return getSuccessResponseVO(CopyTools.copy(file, FileInfoVO.class));
    }

    /**
     * 获取所有目录
     *
     * @param session        会话
     * @param filePid        父级id
     * @param currentFileIds 当前目录PID
     * @return {@link ResponseVO}
     */
    @RequestMapping("/loadAllFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadAllFolder(HttpSession session,String currentFileIds, @VerifyParam(required = true)String filePid){
        UserDto userDto = (UserDto) session.getAttribute("user");
        List<File> fileList = fileService.loadAllFolder(userDto.getUserId(),filePid,currentFileIds);
        return getSuccessResponseVO(CopyTools.copyList(fileList, FileInfoVO.class));
    }


    /**
     * 移动文件
     *
     * @param session 会话
     * @param fileIds 需要移动的文件id
     * @param filePid 父级id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/changeFileFolder")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO changeFileFolder(HttpSession session,
                                       @VerifyParam(required = true)String fileIds,
                                       @VerifyParam(required = true)String filePid){
        UserDto userDto = (UserDto) session.getAttribute("user");
        fileService.changeFileFolder(userDto.getUserId(),fileIds,filePid);
        return getSuccessResponseVO(null);
    }

    /**
     * 创建下载链接
     *
     * @param session 会话
     * @param fileId  文件id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/createDownloadUrl/{fileId}")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO createDownloadUrl(HttpSession session, @VerifyParam(required = true)@PathVariable("fileId")String fileId){
        UserDto userDto = (UserDto) session.getAttribute("user");
        return super.createDownLoadUrl(fileId,userDto.getUserId());
    }

    /**
     * 文件下载
     *
     * @param request  请求
     * @param response 响应
     * @param code     代码
     * @throws Exception 异常
     */
    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkParams = true,checkLogin = false)
    public void download(HttpServletRequest request,HttpServletResponse response,
                               @VerifyParam(required = true)@PathVariable("code")String code) throws Exception {
        super.commonDownload(request,response,code);
    }

    /**
     * 删除文件
     *
     * @param session 会话
     * @param fileIds  文件id
     * @return {@link ResponseVO}
     */
    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO delFile(HttpSession session,@VerifyParam(required = true)String fileIds){
        UserDto userDto = (UserDto) session.getAttribute("user");
        fileService.removeFile2RecycleBath(userDto.getUserId(),fileIds);
        return getSuccessResponseVO(null);
    }
}
