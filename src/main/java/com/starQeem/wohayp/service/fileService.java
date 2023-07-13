package com.starQeem.wohayp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.starQeem.wohayp.entity.dto.UploadResultDto;
import com.starQeem.wohayp.entity.dto.UserDto;
import com.starQeem.wohayp.entity.pojo.File;
import com.starQeem.wohayp.entity.query.FileInfoQuery;
import com.starQeem.wohayp.entity.vo.PaginationResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Date: 2023/5/29 19:58
 * @author: Qeem
 */
public interface fileService extends IService<File> {
    /**
     * 文件分页列表
     *
     * @return {@link PaginationResultVO}<{@link FileInfo}>
     */
    PaginationResultVO<File> pageFileList(Long userId, FileInfoQuery query, boolean isRecycle, boolean isAdmin);

    /**
     * 上传文件
     *
     * @param userDto    用户dto
     * @param fileId     文件Id
     * @param file       文件
     * @param fileName   文件名称
     * @param filePid    文件pid
     * @param fileMd5    文件md5
     * @param chunkIndex 块索引
     * @param chunks     块
     */
    UploadResultDto uploadFile(UserDto userDto, String fileId,
                               MultipartFile file, String fileName,
                               String filePid, String fileMd5,
                               Integer chunkIndex, Integer chunks);

    /**
     * 新建目录
     *
     * @param filePid  父级id
     * @param fileName 文件名称
     * @return {@link File}
     */
    File newFolder(String filePid, String userId, String fileName);

    /**
     * 重命名
     *
     * @param fileId   文件标识
     * @param userId   用户id
     * @param fileName 文件名称
     * @return {@link File}
     */
    File rename(String fileId, String userId, String fileName);

    /**
     * 获取所有目录
     *
     * @param userId         用户id
     * @param filePid        文件pid
     * @param currentFileIds 当前文件id
     * @return {@link List}<{@link File}>
     */
    List<File> loadAllFolder(String userId, String filePid, String currentFileIds);

    /**
     * 移动文件
     *
     * @param userId  用户id
     * @param fileIds 需要移动的文件id
     * @param filePid 父级id
     */
    void changeFileFolder(String userId, String fileIds, String filePid);

    /**
     * 删除文件
     *
     * @param userId  用户id
     * @param fileIds 文件id
     */
    void removeFile2RecycleBath(String userId, String fileIds);

    /**
     * 恢复文件批处理
     *
     * @param userId  用户id
     * @param fileIds 文件id
     */
    void recoverFileBatch(String userId, String fileIds);

    /**
     * 彻底删除
     *
     * @param userId  用户id
     * @param fileIds 文件id
     */
    void delFileBath(String userId, String fileIds,boolean isAdmin);

    /**
     * 检查文件夹文件pid
     *
     * @param rootFilePid 根文件pid
     * @param userId      用户id
     * @param filePid     文件pid
     */
    void checkFolderFilePid(String rootFilePid, String userId, String filePid);

    /**
     * 保存到我的网盘
     *
     * @param shareRootFilePid 分享根文件pid
     * @param shareFileIds     共享文件id
     * @param myFolderId       我文件夹id
     * @param shareUserId      分享用户id
     * @param currentUserId    当前用户id
     */
    void saveShare(Long shareRootFilePid, String shareFileIds, String myFolderId, Long shareUserId, String currentUserId);
}
