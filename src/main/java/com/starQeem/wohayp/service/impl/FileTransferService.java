package com.starQeem.wohayp.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.starQeem.wohayp.entity.dto.UserDto;
import com.starQeem.wohayp.entity.enums.DateTimePatternEnum;
import com.starQeem.wohayp.entity.enums.FileStatusEnums;
import com.starQeem.wohayp.entity.enums.FileTypeEnums;
import com.starQeem.wohayp.entity.pojo.file;
import com.starQeem.wohayp.exception.BusinessException;
import com.starQeem.wohayp.service.fileService;
import com.starQeem.wohayp.util.ProcessUtils;
import com.starQeem.wohayp.util.ScaleFilter;
import com.starQeem.wohayp.util.StringTools;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import static com.starQeem.wohayp.util.Constants.*;

/**
 * @Date: 2023/5/31 22:38
 * @author: Qeem
 */
@Service
public class FileTransferService {
    @Resource
    private fileService fileService;
    private static final Logger logger = LoggerFactory.getLogger(FileTransferService.class);

    /**
     * 转码
     *
     * @param fileId  文件标识
     * @param userDto 用户dto
     */
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void transferFile(Long fileId, UserDto userDto) {
        boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnums fileTypeEnum = null;
        QueryWrapper<file> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_id",fileId).eq("user_id",Long.valueOf(userDto.getUserId()));
        file file = fileService.getBaseMapper().selectOne(queryWrapper);
        try {
            if (file == null||!FileStatusEnums.TRANSFER.getStatus().equals(file.getStatus())){//判断是否为非转码中
                //不是转码中
                return;
            }
            //临时目录
            String tempFolderName = FILE + FILE_FOLDER_TEMP;
            String currentUserFolderName = userDto.getUserId() + fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);

            String fileSuffix = StringTools.getFileSuffix(file.getFileName());
            String month = DateUtil.format(file.getCreateTime(), DateTimePatternEnum.YYYYMM.getPattern());
            //目标目录
            String targetFolderName = FILE + FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName + month);
            if (!targetFolder.exists()){
                targetFolder.mkdirs();
            }
            //真实的文件名
            String realFileName = currentUserFolderName + fileSuffix;
            targetFilePath = targetFolder.getPath()+ "/" + realFileName;
            //合并文件
            union(fileFolder.getPath(),targetFilePath,file.getFileName(),true);
            //视频文件切割
            fileTypeEnum = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            if (FileTypeEnums.VIDEO == fileTypeEnum){
                //视频
                cutFileVideo(fileId,targetFilePath);
                //视频生成缩略图
                cover = month + "/" +currentUserFolderName + IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                ScaleFilter.createCover4Video(new File(targetFilePath),LENGTH_150,new File(coverPath));
            }else if (FileTypeEnums.IMAGE == fileTypeEnum){
                //图片
                //生成缩略图
                cover = month + "/" + realFileName.replace(".","_.");//展示完整图片".",展示缩略图"_."
                String coverPath = targetFolderName +"/" + cover;
                boolean created = ScaleFilter
                        .createThumbnailWidthFFmpeg(new File(targetFilePath),LENGTH_150,new File(coverPath),false);
                if (!created){
                    FileUtils.copyFile(new File(targetFilePath),new File(coverPath)); //如果图太小了,生成的缩略图直接用原图
                }
            }
        }catch (Exception e){
            logger.error("文件转码失败,文件id:{},userId:{}",fileId,userDto.getUserId(),e);
            transferSuccess = false;
        }finally {
            file updateInfo = new file();
            updateInfo.setFileSize(new File(targetFilePath).length());
            updateInfo.setFileCover(cover);
            updateInfo.setStatus(transferSuccess?FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER_FAIL.getStatus());
            QueryWrapper<file> updateInfoQueryWrapper = new QueryWrapper<>();
            updateInfoQueryWrapper
                    .eq("file_id",fileId)
                    .eq("user_id",Long.valueOf(userDto.getUserId()))
                    .eq("status",FileStatusEnums.TRANSFER.getStatus());
            fileService.getBaseMapper().update(updateInfo,updateInfoQueryWrapper);
        }
    }

    /**
     * 合并
     *
     * @param dirPath    dir路径
     * @param toFilePath 文件路径
     * @param fileName   文件名称
     * @param delSource  德尔源
     */
    public void union(String dirPath,String toFilePath,String fileName,boolean delSource){
        File dir = new File(dirPath);
        if (!dir.exists()){
            throw new BusinessException("目录不存在!");
        }
        File[] fileList = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile,"rw");
            byte[] b = new byte[1024*10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                File chunkFile = new File(dirPath + File.separator + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile,"r");
                    while ((len = readFile.read(b)) != -1){
                        writeFile.write(b,0,len);
                    }
                }catch (Exception e){
                    logger.error("合并分片失败!",e);
                    throw  new BusinessException("合并分片失败!");
                }finally {
                    readFile.close();
                }
            }
        }catch (Exception e){
            logger.error("合并文件:{}失败!",fileName,e);
            throw new BusinessException("合并文件" + fileName + "出错了!");
        }finally {
            if (null!=writeFile){
                try {
                    writeFile.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
            if (delSource&&dir.exists()){
                try {
                    FileUtils.deleteDirectory(dir);
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 视频切割
     *
     * @param fileId        文件标识
     * @param videoFilePath 视频文件路径
     */
    private void cutFileVideo(Long fileId,String videoFilePath){
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0,videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()){
            tsFolder.mkdirs();
        }
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";
        String tsPath = tsFolder + "/" + TS_NAME;
        //生成.ts
        String cmd = String.format(CMD_TRANSFER_2TS,videoFilePath,tsPath);
        ProcessUtils.executeCommand(cmd,false);//false是不输出日志的意思
        //生成索引文件.m3u8和切片.ts
        cmd = String.format(CMD_CUT_TS,tsPath,tsFolder.getPath() + "/" + M3U8_NAME,tsFolder.getPath(),fileId);
        ProcessUtils.executeCommand(cmd,false);
        //删除index.ts文件
        new File(tsPath).delete();
    }
}