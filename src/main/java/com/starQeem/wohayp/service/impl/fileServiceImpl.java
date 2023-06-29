package com.starQeem.wohayp.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.starQeem.wohayp.entity.dto.UploadResultDto;
import com.starQeem.wohayp.entity.dto.UserDto;
import com.starQeem.wohayp.entity.dto.UserSpaceDto;
import com.starQeem.wohayp.entity.enums.*;
import com.starQeem.wohayp.entity.pojo.file;
import com.starQeem.wohayp.entity.pojo.user;
import com.starQeem.wohayp.entity.query.FileInfoQuery;
import com.starQeem.wohayp.entity.vo.PaginationResultVO;
import com.starQeem.wohayp.exception.BusinessException;
import com.starQeem.wohayp.exception.ResponseCodeEnum;
import com.starQeem.wohayp.mapper.fileMapper;
import com.starQeem.wohayp.mapper.userMapper;
import com.starQeem.wohayp.service.fileService;
import com.starQeem.wohayp.util.StringTools;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.starQeem.wohayp.util.Constants.*;

/**
 * @Date: 2023/5/29 19:59
 * @author: Qeem
 */
@Service
public class fileServiceImpl extends ServiceImpl<fileMapper, file> implements fileService {
    private static final Logger logger = LoggerFactory.getLogger(fileServiceImpl.class);
    @Resource
    private fileService fileService;
    @Resource
    private FileTransferService fileTransferService;
    @Resource
    private userMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public PaginationResultVO<file> pageFileList(Long userId,FileInfoQuery query,boolean isRecycle,boolean isAdmin) {
        if (query.getPageNo() == null){
            query.setPageNo(ONE);
        }
        if (query.getPageSize() == null){
            query.setPageSize(PAGE_SIZE);
        }
        if (!isRecycle){
            PageHelper.startPage(query.getPageNo(),query.getPageSize());
            PageHelper.orderBy("last_update_time desc");
        }
        QueryWrapper<file> queryWrapper = new QueryWrapper<>();
        if (query.getFileCategory() != null){
            queryWrapper.eq("file_category",query.getFileCategory());
        }
        if (query.getFilePid() != null){
            queryWrapper.eq("file_pid",query.getFilePid());
        }
        if (!isAdmin && userId != null){
            queryWrapper.eq("user_id",userId).eq("del_flag",query.getFileType());
        }
        if (query.getFileId() != null){
            queryWrapper.eq("file_id",query.getFileId());
        }
        List<file> list = fileService.getBaseMapper().selectList(queryWrapper);
        if (isRecycle){  //判断是否是回收站
            //把pid等于0的和父级目录不在回收站的留下,其它过滤掉
            List<file> filteredList = list.stream()
                    .filter(file -> file.getFilePid().equals(ZERO_STRING) || list.stream().noneMatch(f -> f.getFileId().equals(Long.valueOf(file.getFilePid()))))
                    .collect(Collectors.toList());
            int total = filteredList.size();  //数据总条数
            int fromIndex = (query.getPageNo() - 1) * query.getPageSize();//截止当前页码数的所有数据条数
            int toIndex = Math.min(fromIndex + query.getPageSize(), total); //分页查询中的结束索引位置
            List<file> pageRecycleList = filteredList.subList(fromIndex, toIndex);//调用subList进行分页
            int totalPages = (int) Math.ceil((double) total / query.getPageSize()); // 计算总页数
            return new PaginationResultVO<>(total, query.getPageSize(), query.getPageNo(), totalPages, pageRecycleList);
        }
        if (isAdmin){//设置界面的查询
            List<user> userList = userMapper.selectList(null);
            userList.forEach(user -> {
                list.stream()
                        .filter(file -> Objects.equals(user.getUserId(), file.getUserId()))
                        .forEach(file -> {
                            file.setNickName(user.getNickName());
                        });
            });
            int total = list.size();  //数据总条数
            int fromIndex = (query.getPageNo() - 1) * query.getPageSize();//截止当前页码数的所有数据条数
            int toIndex = Math.min(fromIndex + query.getPageSize(), total); //分页查询中的结束索引位置
            List<file> pageRecycleList = list.subList(fromIndex, toIndex);//调用subList进行分页
            int totalPages = (int) Math.ceil((double) total / query.getPageSize()); // 计算总页数
            return new PaginationResultVO<>(total, query.getPageSize(), query.getPageNo(), totalPages, pageRecycleList);
        }
        PageInfo<file> pageInfo = new PageInfo<>(list);
        return new PaginationResultVO<>(Math.toIntExact(pageInfo.getTotal()), pageInfo.getPageSize(), pageInfo.getPageNum(), pageInfo.getPages(), list);
    }

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
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto uploadFile(UserDto userDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks) {
        UploadResultDto resultDto = new UploadResultDto();
        boolean uploadSuccess = true;
        File tempFileFolder = null;
        try{
            if (StringTools.isEmpty(fileId)){ //判断fileId有没有
                //没有
                fileId = RandomUtil.randomNumbers(10);
            }
            resultDto.setFileId(fileId);
            Date curDate = new Date();
            String useSpace = stringRedisTemplate.opsForValue().get(USER_SPACE + userDto.getUserId());//获取用户空间信息
            UserSpaceDto spaceDto = JSONUtil.toBean(useSpace, UserSpaceDto.class);  //string类型转换为UseSpaceDto类型
            if (chunkIndex==ZERO) {
                QueryWrapper<file> queryWrapper = new QueryWrapper<>();
                queryWrapper
                        .eq("file_md5", fileMd5)
                        .eq("status", FileStatusEnums.USING.getStatus())
                        .last("limit 0,1");
                List<file> dbFileList = fileService.getBaseMapper().selectList(queryWrapper);
                //秒传
                if (!dbFileList.isEmpty()) {
                    file dbFile = dbFileList.get(ZERO);
                    //判断文件大小
                    if (dbFile.getFileSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                        throw new BusinessException(ResponseCodeEnum.CODE_904);
                    }
                    dbFile.setFileId(Long.valueOf(fileId));
                    dbFile.setFilePid(filePid);
                    dbFile.setUserId(Long.valueOf(userDto.getUserId()));
                    dbFile.setCreateTime(curDate);
                    dbFile.setStatus(FileStatusEnums.USING.getStatus());
                    dbFile.setDelFlag(FileDelFlagEnums.USING.getFlag());
                    dbFile.setFileMd5(fileMd5);
                    //文件重命名
                    fileName = autoRename(filePid, userDto.getUserId(), fileName);
                    dbFile.setFileName(fileName);
                    fileService.save(dbFile);
                    resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
                    //更新用户使用空间
                    updateUserSpace(userDto, dbFile.getFileSize());
                    return resultDto;
                }
            }
                //暂存在临时目录
                String tempFolderName = FILE + FILE_FOLDER_TEMP;
                String currenUserFolderName = userDto.getUserId() + fileId;
                //创建临时目录
                tempFileFolder = new File(tempFolderName + currenUserFolderName);
                if (!tempFileFolder.exists()){
                    tempFileFolder.mkdirs();
                }
                //判断磁盘空间
                String redisCurrentTempSize = stringRedisTemplate.opsForValue().get(USER_FILE_TEMP_SIZE + userDto.getUserId() + fileId);//获取临时文件大小
                long currentTempSize;
                if (redisCurrentTempSize == null||redisCurrentTempSize.equalsIgnoreCase("")){
                     currentTempSize = 0L;
                }else {
                     currentTempSize = Long.parseLong(redisCurrentTempSize);
                }
                if (file.getSize() + currentTempSize + spaceDto.getUseSpace() > spaceDto.getTotalSpace()){
                    throw new BusinessException(ResponseCodeEnum.CODE_904);
                }
                File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);
                file.transferTo(newFile);
                if (chunkIndex < chunks - 1){
                    resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                    //保存临时大小
                    stringRedisTemplate.opsForValue()
                            .set(USER_FILE_TEMP_SIZE+userDto.getUserId()+fileId,
                                    String.valueOf(currentTempSize + file.getSize()),
                                    USER_FILE_TIME_TTL,TimeUnit.SECONDS);
                    return resultDto;
                }
            //保存文件大小
            stringRedisTemplate.opsForValue().set(USER_FILE_TEMP_SIZE+userDto.getUserId()+fileId,
                            String.valueOf(currentTempSize + file.getSize()),
                            USER_FILE_TIME_TTL,TimeUnit.SECONDS);
            //最后一个分片上传完成,记录数据库,异步合并分片
            String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYYMM.getPattern());
            String fileSuffix = StringTools.getFileSuffix(fileName);
            //真实文件名
            String realFileName = currenUserFolderName + fileSuffix;
            FileTypeEnums fileTypeEnums = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            //自动重命名
            fileName = autoRename(filePid,userDto.getUserId(),fileName);
            //保存文件信息到数据库中
            file fileInfo = new file();
            fileInfo.setFileId(Long.valueOf(fileId));
            fileInfo.setUserId(Long.valueOf(userDto.getUserId()));
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFileName(fileName);
            fileInfo.setFilePath(month + "/" + realFileName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(curDate);
            fileInfo.setLastUpdateTime(curDate);
            fileInfo.setFileCategory(fileTypeEnums.getCategory().getCategory());
            fileInfo.setFileType(fileTypeEnums.getType());
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            fileService.save(fileInfo);
            String redisUseSpace = stringRedisTemplate.opsForValue().get(USER_FILE_TEMP_SIZE + userDto.getUserId() + fileId);
            long totalSpace;
            if (redisUseSpace == null||redisUseSpace.equalsIgnoreCase("")){
                totalSpace = 0L;
            }else {
                totalSpace = Long.parseLong(redisUseSpace);
            }
            updateUserSpace(userDto,totalSpace);
            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            // 事务提交后调用异步方法
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileTransferService.transferFile(fileInfo.getFileId(), userDto);
                }
            });
            return resultDto;
        }catch (BusinessException e){
            logger.error("文件上传失败!",e);
            uploadSuccess = false;
            throw e;
        }catch (Exception e){
            logger.error("文件上传失败!",e);
            uploadSuccess = false;
        }finally {
            if (!uploadSuccess && tempFileFolder!=null){
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                }catch (IOException e){
                    logger.error("删除临时目录失败!",e);
                }
            }
        }
        return resultDto;
    }
    /**
     * 重命名文件
     * @param filePid  文件pid
     * @param userId   用户id
     * @param fileName 文件名称
     * @return {@link String}
     */
    private String autoRename(String filePid,String userId,String fileName){
        QueryWrapper<file> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("user_id",userId)
                .eq("file_pid",filePid)
                .eq("del_flag",FileDelFlagEnums.USING.getFlag())
                .eq("file_name",fileName);
        Long count = fileService.getBaseMapper().selectCount(queryWrapper);
        if (count>ZERO){
            //重命名
            fileName = StringTools.rename(fileName);
        }
        //返回文件名
        return fileName;
    }

    /**
     * 更新用户空间
     *
     * @param userDto   用户dto
     */
    private void updateUserSpace(UserDto userDto,Long totalSize){
         Integer count =  userMapper.updateUserSpace(Long.valueOf(userDto.getUserId()),totalSize,null);
         if (count == ZERO){
             throw new BusinessException(ResponseCodeEnum.CODE_904);
         }
        String redisUserSpace = stringRedisTemplate.opsForValue().get(USER_SPACE + userDto.getUserId());
        UserSpaceDto spaceDto = JSONUtil.toBean(redisUserSpace, UserSpaceDto.class);
        spaceDto.setUseSpace(spaceDto.getUseSpace()+totalSize);
        stringRedisTemplate.opsForValue().set(USER_SPACE + userDto.getUserId(),JSONUtil.toJsonStr(spaceDto),USER_SPACE_TTL, TimeUnit.SECONDS);
    }

    /**
     * 新建目录
     *
     * @param filePid  父级id
     * @param fileName 文件名称
     * @return {@link file}
     */
    @Override
    public file newFolder(String filePid, String userId,String fileName) {
        //校验文件名(不能重复)
        checkFileName(filePid,userId,fileName,FileFolderTypeEnums.FOLDER.getType());
        Date curDate = new Date();
        file file = new file();
        file.setFileId(RandomUtil.randomLong(RANDOM_MIN,RANDOM_MAX));//设置文件id
        file.setUserId(Long.valueOf(userId));
        file.setFilePid(filePid);
        file.setFileName(fileName);
        file.setCreateTime(curDate);
        file.setLastUpdateTime(curDate);
        file.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        file.setStatus(FileStatusEnums.USING.getStatus());
        file.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileService.save(file);
        return file;
    }


    /**
     * 检查文件名称
     *
     * @param filePid    父级id
     * @param userId     用户id
     * @param fileName   文件名称
     * @param folderType 文件夹类型
     */
    private void checkFileName(String filePid,String userId,String fileName,Integer folderType){
        QueryWrapper<file> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("file_pid",filePid)
                .eq("user_id",Long.valueOf(userId))
                .eq("file_name",fileName)
                .eq("folder_type",folderType);
        Long count = fileService.getBaseMapper().selectCount(queryWrapper);
        if (count > ZERO) {
            throw new BusinessException("此目录下已经存在同名文件,请修改文件名称!");
        }
    }

    /**
     * 重命名
     *
     * @param fileId   文件标识
     * @param userId   用户id
     * @param fileName 文件名称
     * @return {@link file}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)//加上事务,有错误时回滚
    public file rename(String fileId, String userId, String fileName) {
        //根据文件id查询数据库
        QueryWrapper<file> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_id",Long.valueOf(fileId));
        file file = fileService.getBaseMapper().selectOne(queryWrapper);
        if (file == null){
            throw new BusinessException("文件不存在!");
        }
        String filePid = file.getFilePid();
        checkFileName(filePid,userId,fileName,file.getFolderType());
        //获取文件后缀
        if (FileFolderTypeEnums.FILE.getType().equals(file.getFolderType())){
            fileName = fileName + StringTools.getFileSuffix(file.getFileName());
        }
        //重命名
        Date curDate = new Date();
        file dbInfo = new file();
        dbInfo.setFileName(fileName);
        dbInfo.setLastUpdateTime(curDate);
        QueryWrapper<file> updateQueryWrapper = new QueryWrapper<>();
        updateQueryWrapper.eq("user_id",Long.valueOf(userId)).eq("file_id",Long.valueOf(fileId));
        fileService.getBaseMapper().update(dbInfo,updateQueryWrapper);

        QueryWrapper<file> selectQueryWrapper = new QueryWrapper<>();
        selectQueryWrapper.eq("file_pid",filePid).eq("user_id",userId).eq("file_name",fileName);
        Long count = fileService.getBaseMapper().selectCount(selectQueryWrapper);
        if (count > ONE){
            throw new BusinessException("文件名" + fileName + "已经存在!");
        }
        file.setFileName(fileName);
        file.setLastUpdateTime(curDate);
        return file;
    }

    /**
     * 获取所有目录
     *
     * @param userId         用户id
     * @param filePid        文件pid
     * @param currentFileIds 当前文件id
     * @return {@link List}<{@link file}>
     */
    @Override
    public List<file> loadAllFolder(String userId, String filePid, String currentFileIds) {
        QueryWrapper<file> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("user_id",Long.valueOf(userId))
                .eq("file_pid",Long.valueOf(filePid))
                .eq("del_flag",FileDelFlagEnums.USING.getFlag())
                .eq("folder_type",FileFolderTypeEnums.FOLDER.getType())
                .last("order by create_time desc");
        List<file> fileList = fileService.getBaseMapper().selectList(queryWrapper);
        //去除掉父级的文件
        if (!StringTools.isEmpty(currentFileIds)) {
            // 遍历集合找到对应的数据并移除
            Iterator<file> iterator = fileList.iterator();
            while (iterator.hasNext()) {
                file file = iterator.next();
                String[] strings = currentFileIds.split(",");
                for (String string : strings) {
                    if (Objects.equals(file.getFileId(), Long.valueOf(string))) {
                        iterator.remove(); // 移除对应的数据
                    }
                }
            }
        }
        return fileList;
    }

    /**
     * 移动文件
     *
     * @param userId  用户id
     * @param fileIds 需要移动的文件id
     * @param filePid 父级id
     */
    @Override
    public void changeFileFolder(String userId, String fileIds, String filePid) {
        if (fileIds.equals(filePid)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!filePid.equals(ZERO_STRING)){//判断是否是没有父级目录
            QueryWrapper<file> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("file_id",Long.valueOf(filePid)).eq("user_id",Long.valueOf(userId));
            file file = fileService.getBaseMapper().selectOne(queryWrapper);
            if (file == null || !FileDelFlagEnums.USING.getFlag().equals(file.getDelFlag())){//没有这个目录或者这个目录已经删除
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        //查询移动到的目录里的所有文件
        QueryWrapper<file> wrapper = new QueryWrapper<>();
        wrapper.select("file_name").eq("file_pid",Long.valueOf(filePid)).eq("user_id",Long.valueOf(userId));
        List<file> dbFileList = fileService.getBaseMapper().selectList(wrapper);
        //如果存在重复的文件名，则保留后来的文件对象
        Map<String,file> dbFileNameMap =
                dbFileList.stream().collect(Collectors.toMap(file::getFileName, Function.identity(),(date1,date2) -> date2));

       //查询选中的文件
        String[] arr = fileIds.split(",");//将需要移动的id分割成数组
        String result = StringTools.Array2String(arr);
        QueryWrapper<file> checkedQueryWrapper = new QueryWrapper<>();
        checkedQueryWrapper.eq("user_id",Long.valueOf(userId)).last("and file_id in(" + result + ")");
        List<file> fileList = fileService.getBaseMapper().selectList(checkedQueryWrapper);
        //将所选文件重命名
        for (file item : fileList){
            file rootFileInfo = dbFileNameMap.get(item.getFileName());
            //文件名已经存在,重命名被还原的文件名
            file updateFile = new file();
            if (rootFileInfo!=null){
                String fileName = StringTools.rename(item.getFileName());
                updateFile.setFileName(fileName);
            }
            updateFile.setFilePid(filePid);
            QueryWrapper<file> updateQueryWrapper = new QueryWrapper<>();
            updateQueryWrapper.eq("user_id",Long.valueOf(userId)).eq("file_id",item.getFileId());
            fileService.getBaseMapper().update(updateFile,updateQueryWrapper);
        }
    }

    /**
     * 删除文件
     *
     * @param userId  用户id
     * @param fileIds 文件id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFile2RecycleBath(String userId, String fileIds) {
        QueryWrapper<file> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("user_id",Long.valueOf(userId))
                .eq("del_flag",FileDelFlagEnums.USING.getFlag())
                .last("and file_id in(" + fileIds + ")");
        List<file> fileList = fileService.getBaseMapper().selectList(queryWrapper);
        if (fileList.isEmpty()){//判断是否不存在
            //不存在,说明文件已经在回收站里,直接结束
            return;
        }
        //获取文件的所有子目录(采用递归)
        List<Long> delFilePidList = new ArrayList<>();
        for (file fileInfo : fileList){
            findAllSubFolderFileList(delFilePidList,Long.valueOf(userId),fileInfo.getFileId(),FileDelFlagEnums.USING.getFlag());
        }
        //把选中的文件的所有子目录都设置为回收站状态
        if (!delFilePidList.isEmpty()){
            String delFileIds = delFilePidList.stream()
                    .map(String::valueOf) // 将每个 Long 类型元素转换为 String 类型
                    .collect(Collectors.joining(","));
            file file = new file();
            file.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
            file.setLastUpdateTime(new Date());
            QueryWrapper<file> updateQueryWrapper = new QueryWrapper<>();
            updateQueryWrapper
                    .eq("user_id",Long.valueOf(userId))
                    .eq("del_flag",FileDelFlagEnums.USING.getFlag())
                    .last("and file_id in(" + delFileIds + ")");
            fileService.getBaseMapper().update(file,updateQueryWrapper);
        }
        //将选中的文件更新为回收站
        file pFile = new file();
        pFile.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        pFile.setLastUpdateTime(new Date());
        QueryWrapper<file> updateQueryWrapper = new QueryWrapper<>();
        updateQueryWrapper
                .eq("user_id",Long.valueOf(userId))
                .eq("del_flag",FileDelFlagEnums.USING.getFlag())
                .last("and file_id in(" + fileIds + ")");
        fileService.getBaseMapper().update(pFile,updateQueryWrapper);
    }
    /**
     * 恢复文件批处理
     *
     * @param userId  用户id
     * @param fileIds 文件id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recoverFileBatch(String userId, String fileIds) {
        QueryWrapper<file> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("user_id",Long.valueOf(userId))
                .eq("del_flag",FileDelFlagEnums.RECYCLE.getFlag())
                .last("and file_id in(" + fileIds + ")");
        List<file> fileList = fileService.getBaseMapper().selectList(queryWrapper);
        if (fileList.isEmpty()){//判断是否不存在
            //不存在,说明文件不存在回收站里,直接结束
            return;
        }
        //获取文件的所有子目录(采用递归)
        List<Long> delFilePidList = new ArrayList<>();
        for (file fileInfo : fileList){
            findAllSubFolderFileList(delFilePidList,Long.valueOf(userId),fileInfo.getFileId(),FileDelFlagEnums.RECYCLE.getFlag());
        }
        //把选中的文件的所有子目录都设置为使用中状态
        if (!delFilePidList.isEmpty()){
            String delFileIds = delFilePidList.stream()
                    .map(String::valueOf) // 将每个 Long 类型元素转换为 String 类型
                    .collect(Collectors.joining(","));
            file file = new file();
            file.setDelFlag(FileDelFlagEnums.USING.getFlag());
            file.setLastUpdateTime(new Date());
            QueryWrapper<file> updateQueryWrapper = new QueryWrapper<>();
            updateQueryWrapper
                    .eq("user_id",Long.valueOf(userId))
                    .eq("del_flag",FileDelFlagEnums.RECYCLE.getFlag())
                    .last("and file_id in(" + delFileIds + ")");
            fileService.getBaseMapper().update(file,updateQueryWrapper);
        }
        //将选中的文件更新为使用中
        file pFile = new file();
        pFile.setDelFlag(FileDelFlagEnums.USING.getFlag());
        pFile.setLastUpdateTime(new Date());
        QueryWrapper<file> updateQueryWrapper = new QueryWrapper<>();
        updateQueryWrapper
                .eq("user_id",Long.valueOf(userId))
                .eq("del_flag",FileDelFlagEnums.RECYCLE.getFlag())
                .last("and file_id in(" + fileIds + ")");
        fileService.getBaseMapper().update(pFile,updateQueryWrapper);
    }

    /**
     * 彻底删除
     *
     * @param userId  用户id
     * @param fileIds 文件id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFileBath(String userId, String fileIds,boolean isAdmin) {
        QueryWrapper<file> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("user_id",Long.valueOf(userId))
                .eq("del_flag",FileDelFlagEnums.RECYCLE.getFlag())
                .last("and file_id in(" + fileIds + ")");
        List<file> fileList = fileService.getBaseMapper().selectList(queryWrapper);
        if (fileList.isEmpty() && !isAdmin){//判断是否不存在(管理员直接删,不走这里)
            //不存在,说明文件不存在回收站里,直接结束
            return;
        }
        //获取文件的所有子目录(采用递归)
        List<Long> delFilePidList = new ArrayList<>();
        for (file fileInfo : fileList){
            findAllSubFolderFileList(delFilePidList,Long.valueOf(userId),fileInfo.getFileId(),FileDelFlagEnums.RECYCLE.getFlag());
        }
        if (!delFilePidList.isEmpty()){//判断是否存在子目录
            //存在
            String delFileIds = delFilePidList.stream()//将 List<Long> 类型的id集合 delFilePidList 转换为以逗号分隔的字符串
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));
            //把选中的文件的所有子目录删除
            QueryWrapper<file> deleteQueryWrapper = new QueryWrapper<>();
            deleteQueryWrapper
                    .eq("user_id",Long.valueOf(userId))
                    .last("and file_id in(" + delFileIds + ")");
            if (!isAdmin){
                deleteQueryWrapper.eq("del_flag",FileDelFlagEnums.RECYCLE.getFlag());
            }
            fileService.getBaseMapper().delete(deleteQueryWrapper);
        }
        //将选中的文件删除
        QueryWrapper<file> deleteQueryWrapper = new QueryWrapper<>();
        deleteQueryWrapper
                .eq("user_id",Long.valueOf(userId))
                .last("and file_id in(" + fileIds + ")");
        if (!isAdmin){
            deleteQueryWrapper.eq("del_flag",FileDelFlagEnums.RECYCLE.getFlag());
        }
        fileService.getBaseMapper().delete(deleteQueryWrapper);
    }
    /**
     * 找到所有子文件夹文件列表
     *
     * @param fileIdList 文件id列表
     * @param userId     用户id
     * @param fileId     文件id
     * @param delFlag    状态(1:回收站  2:正常)
     */
    private void findAllSubFolderFileList(List<Long> fileIdList,Long userId,Long fileId,Integer delFlag){
        fileIdList.add(fileId);
        QueryWrapper<file> queryWrapper = new QueryWrapper<>();
        queryWrapper
                .eq("user_id",userId)
                .eq("file_pid",fileId)
                .eq("del_flag",delFlag);
        List<file> fileList = fileService.getBaseMapper().selectList(queryWrapper);
        for (file fileInfo : fileList){
            findAllSubFolderFileList(fileIdList,userId,fileInfo.getFileId(),delFlag);
        }
    }

    /**
     * 检查文件夹文件pid
     *
     * @param rootFilePid 根文件pid
     * @param userId      用户id
     * @param filePid     文件pid
     */
    @Override
    public void checkFolderFilePid(String rootFilePid, String userId, String filePid) {
        if (StringTools.isEmpty(filePid)){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (rootFilePid.equals(filePid)){
            return;
        }
        checkFilePid(rootFilePid,filePid,userId);
    }

    private void checkFilePid(String rootFilePid,String fileId,String userId){
        QueryWrapper<file> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("file_id",Long.valueOf(fileId)).eq("user_id",Long.valueOf(userId));
        file fileInfo = fileService.getBaseMapper().selectOne(queryWrapper);
        if (fileInfo == null){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (ZERO_STRING.equals(fileInfo.getFilePid())){
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (fileInfo.getFilePid().equals(rootFilePid)){
            return;
        }
        checkFilePid(rootFilePid,fileInfo.getFilePid(),userId); //递归
    }

    /**
     * 保存到我的网盘
     *
     * @param shareRootFilePid 分享根文件pid
     * @param shareFileIds     共享文件id
     * @param myFolderId       我文件夹id
     * @param shareUserId      分享用户id
     * @param currentUserId    当前用户id
     */
    @Override
    public void saveShare(Long shareRootFilePid, String shareFileIds, String myFolderId, Long shareUserId, String currentUserId) {
        //目标文件列表
        QueryWrapper<file> currentQueryWrapper = new QueryWrapper<>();
        currentQueryWrapper.eq("user_id",Long.valueOf(currentUserId)).eq("file_id",Long.valueOf(myFolderId));
        List<file> currentFileList = fileService.getBaseMapper().selectList(currentQueryWrapper);
        Map<String,file> currentFileMap = currentFileList.stream()
                .collect(Collectors.toMap(file::getFileName,Function.identity(),(data1,data2) -> data2));
        //选择的文件
        QueryWrapper<file> shareQueryWrapper = new QueryWrapper<>();
        shareQueryWrapper.eq("user_id",shareUserId).last("and file_id in(" + shareFileIds + ")");
        List<file> shareFileList = fileService.getBaseMapper().selectList(shareQueryWrapper);
        //重命名文件
        List<file> copyFileList = new ArrayList<>();
        Date curDate = new Date();
        for (file item : shareFileList){
            file haveFile = currentFileMap.get(item.getFileName());
            if (haveFile != null){
                item.setFileName(StringTools.rename(item.getFileName()));
            }
            fileAllSubFile(copyFileList,item,shareUserId,currentUserId,curDate,myFolderId);
        }
        fileService.saveBatch(copyFileList);
    }
    private void fileAllSubFile(List<file> copyFileList,file fileInfo,Long sourceUserId,String currentUserId,Date curDate,String newFilePid){
        Long sourceFileId = fileInfo.getFileId();
        fileInfo.setCreateTime(curDate);
        fileInfo.setLastUpdateTime(curDate);
        fileInfo.setFilePid(newFilePid);
        fileInfo.setUserId(Long.valueOf(currentUserId));
        Long newFileId = RandomUtil.randomLong(RANDOM_MIN,RANDOM_MAX);
        fileInfo.setFileId(newFileId);
        copyFileList.add(fileInfo);
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())){
            QueryWrapper<file> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("file_pid",sourceFileId).eq("user_id",sourceUserId);
            List<file> fileList = fileService.getBaseMapper().selectList(queryWrapper);
            for (file item : fileList){
                fileAllSubFile(copyFileList,item,sourceUserId,currentUserId,curDate,newFileId.toString());
            }
        }
    }
}
