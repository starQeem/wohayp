package com.starQeem.wohayp.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.starQeem.wohayp.entity.dto.DownloadFileDto;
import com.starQeem.wohayp.entity.enums.FileCategoryEnums;
import com.starQeem.wohayp.entity.enums.FileFolderTypeEnums;
import com.starQeem.wohayp.entity.pojo.File;
import com.starQeem.wohayp.entity.vo.ResponseVO;
import com.starQeem.wohayp.exception.BusinessException;
import com.starQeem.wohayp.exception.ResponseCodeEnum;
import com.starQeem.wohayp.service.fileService;
import com.starQeem.wohayp.util.Constants;
import com.starQeem.wohayp.util.StringTools;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.net.URLEncoder;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.starQeem.wohayp.util.Constants.*;

public class CommonFileController extends ABaseController {
    @Resource
    private fileService fileService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    /**
     * 得到图像
     *
     * @param response    响应
     * @param imageFolder 图片文件夹
     * @param imageName   图片名字
     */
    protected void getImage(HttpServletResponse response,String imageFolder,String imageName){
        if (StringTools.isEmpty(imageFolder)||StringTools.isEmpty(imageName)||!StringTools.pathIsOk(imageFolder)||!StringTools.pathIsOk(imageName)){
            return;
        }
        String imageSuffix = StringTools.getFileSuffix(imageName);
        String filePath = FILE +FILE_FOLDER_FILE +imageFolder + "/" + imageName;
        imageSuffix = imageSuffix.replace(".","");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control","max-age-2592000");
        readFile(response,filePath);
    }

    /**
     * 得到文件
     *
     * @param response 响应
     * @param fileId   文件标识
     * @param userId   用户id
     */
    protected void getFile(HttpServletResponse response,String fileId,String userId){
        String filePath = null;
        if (fileId.endsWith(".ts")){
            String[] isArray = fileId.split("_"); //以"_"为中心将fileId分割成两部分"3589909651"和"0001.ts"这样的
            String realFileId = isArray[0];//拿前面的"3589909651"
            File fileInfo = fileService.getBaseMapper().selectOne(Wrappers.<File>lambdaQuery()
                    .eq(File::getFileId,realFileId)
                    .eq(File::getUserId,Long.valueOf(userId)));
            if (fileInfo == null){
                return;
            }
            String fileName = fileInfo.getFilePath();
            String nameWithoutExtension = fileName.substring(0, fileName.lastIndexOf('.'));//去掉后缀.mp4
            fileName = StringTools.getFileNameNoSuffix(nameWithoutExtension + "/" + fileId);
            filePath = FILE + FILE_FOLDER_FILE + fileName +".ts";
        }else {
            File fileInfo = fileService.getBaseMapper().selectOne(Wrappers.<File>lambdaQuery()
                    .eq(File::getFileId,Long.valueOf(fileId))
                    .eq(File::getUserId,Long.valueOf(userId)));
            if (fileInfo == null){//判断文件是否存在
                return;
            }
            if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())){
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                filePath = FILE + FILE_FOLDER_FILE + fileNameNoSuffix + "/" + M3U8_NAME;
            }else {
                filePath = FILE + FILE_FOLDER_FILE + fileInfo.getFilePath();
            }
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()){
                return;
            }
        }
        readFile(response,filePath);
    }

    /**
     * 获取当前路径
     *
     * @param path   路径
     * @param userId 用户id
     * @return {@link ResponseVO}
     */
    protected ResponseVO getFolderInfo(String path,String userId){
        String[] pathArray = path.split("/"); //分割路径
        String result = StringTools.Array2String(pathArray);
        //查询当前目录
        QueryWrapper<File> queryWrapper = new QueryWrapper<>();
        if (userId != null){
            queryWrapper.eq("user_id",Long.valueOf(userId));
        }
        queryWrapper
                .eq("folder_type",FileFolderTypeEnums.FOLDER.getType())
                .last("and file_id in("+ result+ ")order by FIELD(file_id," + result + ")");
        List<File> fileList = fileService.getBaseMapper().selectList(queryWrapper);
        return getSuccessResponseVO(fileList);
    }

    /**
     * 创建下载链接
     *
     * @param fileId 文件id
     * @param userId 用户id
     * @return {@link ResponseVO}
     */
    protected ResponseVO createDownLoadUrl(String fileId,String userId){
        File fileInfo = fileService.getBaseMapper().selectOne(Wrappers.<File>lambdaQuery()
                .eq(File::getFileId,fileId)
                .eq(File::getUserId,userId));
        if (fileInfo == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String code = StringTools.getRandomString(LENGTH_50);
        DownloadFileDto fileDto = new DownloadFileDto();
        fileDto.setDownloadCode(code);
        fileDto.setFilePath(fileInfo.getFilePath());
        fileDto.setFileName(fileInfo.getFileName());
        stringRedisTemplate.opsForValue().set(DOWN_LOAD + code, JSONUtil.toJsonStr(fileDto),DOWN_LOAD_TTL, TimeUnit.SECONDS);
        return getSuccessResponseVO(code);
    }

    /**
     * 文件下载
     *
     * @param request  请求
     * @param response 响应
     * @param code     代码
     * @throws Exception 异常
     */
    protected void commonDownload(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        String redisDownloadFileDto = stringRedisTemplate.opsForValue().get(DOWN_LOAD + code);
        DownloadFileDto downloadFileDto = JSONUtil.toBean(redisDownloadFileDto, DownloadFileDto.class);
        if (downloadFileDto == null) {
            return;
        }
        String filePath = FILE + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/x-msdownload; charset=UTF-8");
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {//IE浏览器
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response, filePath);
    }

}
